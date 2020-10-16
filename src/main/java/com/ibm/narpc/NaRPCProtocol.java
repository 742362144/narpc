package com.ibm.narpc;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NaRPCProtocol {
    public static final int HEADERSIZE = Integer.BYTES + Long.BYTES;
    private static final Logger LOG = NaRPCUtils.getLogger();

    /**
     * 构造消息
     * @param ticket
     * @param message request or response
     * @param buffer  请求内容
     * @throws IOException
     */
    public static void makeMessage(long ticket, NaRPCMessage message, ByteBuffer buffer) throws IOException {
        // 将写指针移动到HEADERSIZE
        buffer.clear().position(HEADERSIZE);
        // 调用NaRPCMessage具体的实现类（比如SimpleRpcRequest）将消息写入到buffer中
        int size = message.write(buffer);
        // 将buffer从写模式转化为读模式
        buffer.flip();
        if ((size + HEADERSIZE) != buffer.remaining()) {
            throw new IOException("Error in serialization");
        }
        // 不会真正的删除掉buffer中的数据，只是把position移动到最前面，同时把limit调整为capacity
        buffer.clear();
        // 写入消息体大小
        buffer.putInt(size);
        // 写入ticket（作用暂时未知，可能是一种简单的标识）
        buffer.putLong(ticket);
        // 指定buffer中可读数据的大小为HEADERSIZE + size
        buffer.clear().limit(HEADERSIZE + size);
    }

    /**
     * 读取channel中的内容并存入buffer中，返回buffer中存储数据的大小
     * @param channel
     * @param buffer
     * @return
     * @throws IOException
     */
    public static long fetchBuffer(SocketChannel channel, ByteBuffer buffer) throws IOException{
        buffer.clear().limit(HEADERSIZE);
        int ret = channel.read(buffer);
        if (ret <= 0){
            return ret;
        }
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) < 0){
                return -1;
            }
        }
        buffer.flip();
        int size = buffer.getInt();
        long ticket = buffer.getLong();
        buffer.clear().limit(size);
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) < 0) {
                throw new IOException("error when reading header from socket");
            }

        }
        buffer.flip();
//		LOG.info("fetching message with ticket " + ticket + ", threadid " + Thread.currentThread().getName());
        return ticket;
    }
}
