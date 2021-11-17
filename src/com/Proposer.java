package com;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import static com.PaxosHelper.*;


public class Proposer implements Runnable {


    Map<Net, Socket> socketMap = new HashMap<>();

    List<Net> acceptor = new ArrayList<>();

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
                outputStream.write(1);  // 消息类型
                outputStream.write(4);  // 字节长度
                outputStream.write(seq);
                outputStream.flush();
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
            outputStream.write(2);
            outputStream.write(bytes.length + 4);
            outputStream.write(seq);
            outputStream.write(bytes);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    private Socket getSocket(Net acceptorAddr) {
        Socket socket = Optional.ofNullable(socketMap.get(acceptorAddr))
                .orElseGet(() -> socketSupplier.apply(acceptorAddr));
        return socket;
    }



    @Override
    public void run() {

    }
}
