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

import org.slf4j.Logger;

/**
 * 作为NaRPCServerGroup和NaRPCClientGroup，记录该RPC组的配置
 * 默认的NaRPCEndpoint buffer队列深度，消息的大小（NaRPCEndpoint会用来初始化buffer）
 */
public class NaRPCGroup {
	private static final Logger LOG = NaRPCUtils.getLogger();
	
	public static int DEFAULT_QUEUE_DEPTH = 16;
	public static int DEFAULT_MESSAGE_SIZE = 512;
	public static boolean DEFAULT_NODELAY = false;
	
	private int queueDepth;
	private int messageSize;
	private boolean nodelay;
	
	public NaRPCGroup(){
		this(DEFAULT_QUEUE_DEPTH, DEFAULT_MESSAGE_SIZE, DEFAULT_NODELAY);
	}	
	
	public NaRPCGroup(int queueDepth, int messageSize, boolean nodelay){
		this.queueDepth = queueDepth;
		this.messageSize = messageSize;
		this.nodelay = nodelay;
	}

	public int getQueueDepth() {
		return queueDepth;
	}

	public int getMessageSize() {
		return messageSize;
	}

	public boolean isNodelay() {
		return nodelay;
	}
}
