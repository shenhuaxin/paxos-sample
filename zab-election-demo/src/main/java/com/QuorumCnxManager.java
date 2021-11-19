package com;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集群连接管理器
 */
public class QuorumCnxManager {

    public static final int RECV_CAPACITY = 100;
    // 待发送队列
    final ConcurrentHashMap<Long, ArrayBlockingQueue<ByteBuffer>> queueSendMap;
    // 待处理队列
    public final ArrayBlockingQueue<Message> recvQueue;

    private long serverId;

    public QuorumCnxManager(long serverId) {
        this.serverId = serverId;
        this.recvQueue = new ArrayBlockingQueue<Message>(RECV_CAPACITY);
        this.queueSendMap = new ConcurrentHashMap<Long, ArrayBlockingQueue<ByteBuffer>>();
    }


    public void toSend(Long serverId, ByteBuffer buf) {
        if (this.serverId == serverId) {
            buf.position(0);
            addToRecvQueue(new Message(buf.duplicate(), serverId));
        } else {

        }
    }

    public void addToSendQueue() {

    }

    public void addToRecvQueue(Message message) {

    }

    public Message pollRecvQueue() {

    }


    public void connectAll() {

    }


    private long getServerId() {
        // todo
        return 0;
    }




    static public class Message {

        Message(ByteBuffer buffer, long sid) {
            this.buffer = buffer;
            this.sid = sid;
        }

        ByteBuffer buffer;
        long sid;
    }
}
