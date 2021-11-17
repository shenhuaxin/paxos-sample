package com;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import static com.PaxosHelper.*;

public class Acceptor implements Runnable {

    int port;

    public Acceptor(int port) {
        this.port = port;
    }
    Queue<Msg> receiveQueue = new LinkedBlockingQueue();

    int maxId = 0;
    int choseId = 0;
    String choseValue;


    @Override
    public void run() {
        new Thread(() -> receive(port)).start();

        while (true) {
            var msg = receiveQueue.poll();
            if (msg instanceof PrepareMsg prepareMsg) {
                int id = prepareMsg.id;

                Socket socket = getSocket(new Net(prepareMsg.host, prepareMsg.port));
                if (id <= maxId) {
                    writePrepareResp(socket, id, 0, 0, null);
                    continue; // 处理下一个 prepare 请求
                }
                // 只要新的 prepare id 比之前的prepare id 大， 那么就需要返回 promise ,
                // 但是promise 中有没有已经被选中的值，是另外一回事。
                writePrepareResp(socket, id, 1, choseId, choseValue);
            } else if (msg instanceof AcceptMsg acceptMsg) {
                int id = acceptMsg.id;
                if (maxId > id) {
                    // 已经接收到了其他
                }
            }
        }

    }

    private void writePrepareResp(Socket socket, int id, int resp, int choseId, String choseValue) {
        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(id);
            byte[] bytes = choseValue.getBytes(StandardCharsets.UTF_8);
            outputStream.write(4 + 4 + bytes.length);
            outputStream.write(resp);
            outputStream.write(choseId);
            outputStream.write(bytes);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receive(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                Socket socket = serverSocket.accept();
                InputStream inputStream = socket.getInputStream();
                byte[] type = new byte[4];
                byte[] len = new byte[4];
                byte[] seq = new byte[4];
                inputStream.read(type);
                inputStream.read(len);
                inputStream.read(seq);
                String remoteHost = socket.getInetAddress().getHostAddress();
                int remotePort = socket.getPort();
                Msg msg;
                if (toInt(type) == 1) {
                    msg = new PrepareMsg(toInt(seq), remoteHost, remotePort);
                } else {
                    byte[] value = new byte[toInt(len) - 4];
                    inputStream.read(value);
                    msg = new AcceptMsg(toInt(seq), new String(value), remoteHost, remotePort);
                }
                receiveQueue.offer(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    Map<Net, Socket> socketMap = new HashMap<>();

    Function<Net, Socket> socketSupplier = (net) -> {
        try {
            return new Socket(net.addr, net.port);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    };
    private Socket getSocket(Net acceptorAddr) {
        Socket socket = Optional.ofNullable(socketMap.get(acceptorAddr))
                .orElseGet(() -> socketSupplier.apply(acceptorAddr));
        return socket;
    }

    private static int toInt(byte[] bytes) {
        return Integer.parseInt(new String(bytes));
    }
}


