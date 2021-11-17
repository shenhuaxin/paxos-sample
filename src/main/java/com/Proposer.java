package com;

import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import static com.PaxosHelper.*;


public class Proposer extends Thread {

    List<Net> acceptors = new ArrayList<>();

    int port;
    public Proposer(List<Net> acceptors, int port) {
        this.acceptors = acceptors;
        this.port = port;
    }

    @Override
    public void run() {
        new Thread(() -> receive(port)).start();

        while (true) {
            // 进行一轮prepare消息的发送
            int id = IdCreator.getId();
            // 发送prepare消息
            for (Net net : acceptors) {
                sendPrepare(id, net);
            }
            // prepare消息发送完毕之后，等待 prepare_response
            int respCount = 0;
            String choseValue = null;
            while (true) {
                PrepareResponse resp = null;
                try {
                    resp = receiveQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("proposer-" + port + ": " + "接收到prepare resp: " + JSON.toJSONString(resp));
                if (resp.resp == 1) {
                    if (resp.choseValue == null) {
                        // res
                    } else {
                        // prepare response , 代表已经有了 choseValue
                        choseValue = resp.choseValue;
                    }
                    respCount++;
                }
                if (respCount > (acceptors.size() / 2)) {
                    // 超过一半的 acceptor 进行了响应， 对所有 acceptor 发送 accept
                    for (Net net : acceptors) {
                        choseValue = choseValue == null ? UUID.randomUUID().toString().substring(0, 6) : choseValue;
                        sendAccept(id, choseValue, net);
                    }
                    break;
                }
            }

        }
    }

    BlockingQueue<PrepareResponse> receiveQueue = new LinkedBlockingQueue();

    public void receive(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                Socket socket = serverSocket.accept();
                InputStream inputStream = socket.getInputStream();
                byte[] bytes = inputStream.readAllBytes();

                PrepareResponse msg = JSON.parseObject(Arrays.copyOfRange(bytes, 1, bytes.length),
                        PrepareResponse.class);

                receiveQueue.offer(msg);
                inputStream.close();
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

    public void sendPrepare(int seq, Net acceptorAddr) {
        Socket socket = getSocket(acceptorAddr);
        if (socket != null) {
            try {
                OutputStream outputStream = socket.getOutputStream();
//                outputStream.write(1);  // 消息类型
//                outputStream.write(4);  // 字节长度
//                outputStream.write(seq);
//                outputStream.flush();
                PrepareMsg prepareMsg = new PrepareMsg(seq, "127.0.0.1", port);
                prepareMsg.type = 1;

                outputStream.write(0x1);
                outputStream.write(JSON.toJSONBytes(prepareMsg));
                outputStream.flush();
                outputStream.close();
                System.out.println("proposer-" + port + ": " + "向acceptor-" + acceptorAddr.port + "发送prepare消息：" + seq);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendAccept(int seq, String value, Net acceptorAddr) {
        Socket socket = getSocket(acceptorAddr);
        try {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

            OutputStream outputStream = socket.getOutputStream();
//            outputStream.write(2);
//            outputStream.write(bytes.length + 4);
//            outputStream.write(seq);
//            outputStream.write(bytes);
            AcceptMsg acceptMsg = new AcceptMsg(seq, value, "127.0.0.1", port);
            acceptMsg.type = 3;
            outputStream.write(0x3);
            outputStream.write(JSON.toJSONBytes(acceptMsg));
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    private Socket getSocket(Net acceptorAddr) {
        Socket socket = Optional.ofNullable(socketMap.get(acceptorAddr))
                .orElseGet(() -> socketSupplier.apply(acceptorAddr));
        return socket;
    }


    private static int toInt(byte[] bytes) {
        return Integer.parseInt(new String(bytes));
    }

}
