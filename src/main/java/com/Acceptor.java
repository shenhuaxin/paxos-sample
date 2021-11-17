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

public class Acceptor extends Thread {

    int port;

    public Acceptor(int port) {
        this.port = port;
    }
    BlockingQueue<Msg> receiveQueue = new LinkedBlockingQueue();

    int maxId = 0;
    int choseId = 0;
    String choseValue;


    @Override
    public void run() {
        new Thread(() -> receive(port)).start();

        while (true) {
            Msg msg = null;
            try {
                msg = receiveQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
                if (maxId <= id) {
                    // 没有接收到了更新的 prepare
                    choseValue = acceptMsg.value;
                    System.out.println("Acceptor接收到accept" + choseValue);
                }
            }
        }
    }

    private void writePrepareResp(Socket socket, int id, int resp, int choseId, String choseValue) {
        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(0x2);
            PrepareResponse prepareResponse = new PrepareResponse(id, resp, choseId, choseValue);
            prepareResponse.type = 0x2;
            outputStream.write(JSON.toJSONBytes(prepareResponse));
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receive(int port) {
        try {
            System.out.println("acceptor-"+port+"开始接受消息");
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                Socket socket = serverSocket.accept();
                InputStream inputStream = socket.getInputStream();
                byte[] bytes = inputStream.readAllBytes();

                Msg msg = null;
                if (bytes[0] == 0x1) {
                    msg = JSON.<PrepareMsg>parseObject(Arrays.copyOfRange(bytes, 1, bytes.length), PrepareMsg.class);
                } else if (bytes[0] == 0x3) {
                    msg = JSON.<AcceptMsg>parseObject(Arrays.copyOfRange(bytes, 1, bytes.length), AcceptMsg.class);
                }
                System.out.println("acceptor-"+port+"接收到消息：" + JSON.toJSONString(msg));
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
            System.out.println(net.addr + "," + net.port);
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
        int value;
        value = (int) ((bytes[0]&0xFF)
                | ((bytes[1]<<8) & 0xFF00)
                | ((bytes[2]<<16)& 0xFF0000)
                | ((bytes[3]<<24) & 0xFF000000));
        return value;
    }
}


