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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NaRPCServerChannel {
	private SocketChannel channel;
	private ByteBuffer buffer;
	
	public NaRPCServerChannel(NaRPCGroup group, SocketChannel channel){
		this.channel = channel;
		this.buffer = ByteBuffer.allocate(group.getMessageSize());
	}
	
	public long receiveMessage(NaRPCMessage message) throws IOException {
		long ticket = NaRPCProtocol.fetchBuffer(channel, buffer);
		while(ticket == 0){
            ticket = NaRPCProtocol.fetchBuffer(channel, buffer);
        }
		if (ticket > 0){
			message.update(buffer);	
		}
		return ticket;
	}
	
	public void transmitMessage(long ticket, NaRPCMessage message) throws IOException {
		NaRPCProtocol.makeMessage(ticket, message, buffer);
		while(buffer.hasRemaining()){
			channel.write(buffer);
		}
	}
	
	public void close() throws IOException{
		this.channel.close();
	}

	public SocketChannel getSocketChannel() {
		return channel;
	}

	public String address() throws IOException {
		return channel.getRemoteAddress().toString();
	}
}
