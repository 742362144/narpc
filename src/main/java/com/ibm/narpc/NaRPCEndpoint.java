/*
 * NaRPC: An NIO-based RPC library
 *
 * Author: Patrick Stuedi <stu@zurich.ibm.com>
 *
 * Copyright (C) 2016-2018, IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.ibm.narpc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class NaRPCEndpoint<R extends NaRPCMessage, T extends NaRPCMessage> {
	private NaRPCGroup group;
	private ConcurrentHashMap<Long, NaRPCFuture<R,T>> pendingRPCs;
	private ArrayBlockingQueue<ByteBuffer> bufferQueue;
	private AtomicLong sequencer;
	private SocketChannel channel;
	private ReentrantLock readLock;
	private ReentrantLock writeLock;

	public NaRPCEndpoint(NaRPCGroup group, SocketChannel channel) throws Exception {
		this.group = group;
		this.channel = channel;
		this.channel.setOption(StandardSocketOptions.TCP_NODELAY, group.isNodelay());
		this.channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		this.pendingRPCs = new ConcurrentHashMap<Long, NaRPCFuture<R,T>>();
		this.bufferQueue = new ArrayBlockingQueue<ByteBuffer>(group.getQueueDepth());
		for (int i = 0; i < group.getQueueDepth(); i++){
			ByteBuffer reqBuffer = ByteBuffer.allocate(group.getMessageSize());
			bufferQueue.put(reqBuffer);
		}
		this.sequencer = new AtomicLong(1);
		this.readLock = new ReentrantLock();
		this.writeLock = new ReentrantLock();
	}

	public void connect(InetSocketAddress address) throws IOException {
		this.channel.configureBlocking(false);
		this.channel.connect(address);
		while(!channel.finishConnect() ){
		}

	}

	public void close() throws IOException{
		this.channel.close();
	}

	/**
	 * 发送rpc请求，并获得future
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 */
	public NaRPCFuture<R,T> issueRequest(R request, T response) throws IOException {
		// 从队列里获得buffer
		ByteBuffer buffer = getBuffer();
		while(buffer == null){
			buffer = getBuffer();
		}

		long ticket = sequencer.getAndIncrement();
		// 构建请求消息体
		NaRPCProtocol.makeMessage(ticket, request, buffer);
		NaRPCFuture<R,T> future = new NaRPCFuture<R,T>(this, request, response, ticket);
		pendingRPCs.put(ticket, future);

		// 将buffer内容写入到channel中
		// buffer size由NaRPCGroup初始化的messageSize指定
		while(!writeLock.tryLock());
		channel.write(buffer);
		while(buffer.hasRemaining()){
			// 从channel读取数据
			pollResponse();

			channel.write(buffer);
		}
		writeLock.unlock();

		putBuffer(buffer);
		return future;
	}

	/**
	 * 从channel中读取数据并存入buffer中
	 * NaRPCFuture将读取到buffer中的数据根据Response实现的update方法进行解析
	 * @throws IOException
	 */
	void pollResponse() throws IOException {
		ByteBuffer buffer = getBuffer();
		if (buffer == null){
			return;
		}
		if(readLock.tryLock()){
			// 一直从channel中读取数据，直到读取完毕
			// fetchBuffer返回buffer中数据的大小，ticket > 0表明buffer中有数据
			long ticket = NaRPCProtocol.fetchBuffer(channel, buffer);
			readLock.unlock();
			if (ticket > 0){
				NaRPCFuture<R, T> future = pendingRPCs.remove(ticket);
				future.getResponse().update(buffer);
				future.signal();
			}
		}
		putBuffer(buffer);
	}
	
	public String address() throws IOException {
		return channel.getRemoteAddress().toString();
	}
	
	private ByteBuffer getBuffer(){
		ByteBuffer buffer = bufferQueue.poll();
		return buffer;
	}

	private void putBuffer(ByteBuffer buffer) throws IOException{
		try {
			bufferQueue.put(buffer);
		} catch(InterruptedException e){
			throw new IOException(e);
		}
	}
}
