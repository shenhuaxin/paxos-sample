package main.java.com;

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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static main.java.com.PaxosHelper.*;


public class Proposer extends Thread {

    List<Net> acceptors = new ArrayList<>();

    int port;

    public Proposer(List<Net> acceptors, int port) {
        this.acceptors = acceptors;
        this.port = port;
    }

    int phase;
    @Override
    public void run() {
        new Thread(() -> receive(port)).start();

        while (true) {
            // 每轮投票前， 模拟一个停顿。
            random_pause();
            phase = IdCreator.getId(port);
            // 进行一轮prepare消息的发送
            // 发送prepare消息
            Collections.shuffle(acceptors);
            for (Net net : acceptors) {
                random_pause();
                sendPrepare(phase, net);
            }
            // prepare消息发送完毕之后，等待 prepare_response
            int respCount = 0;
            String choseValue = null;
            int choseId = 0;
            while (true) {
                PrepareResponse resp = null;
                try {
                    resp = receiveQueue.poll(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (resp != null && resp.resp == 1) {
                    if (resp.choseValue == null) {
                        // res
                    } else {
                        // prepare response , 代表已经有了 choseValue
                        if (choseValue == null || choseId < resp.choseId) {
                            choseValue = resp.choseValue;
                            choseId = resp.choseId;
                        }
                    }
                    printPrepareResp(resp);
                    respCount++;
                }
                if (resp == null) {
                    if (respCount > (acceptors.size() / 2)) {
                        choseValue = choseValue == null ? UUID.randomUUID().toString() : choseValue;
                        System.out.println("[proposer-" + port + "]" + "phase -> "+ phase + ": 得到了多数派的同意，proposal = " + choseValue);
                        // 超过一半的 acceptor 进行了响应， 对所有 acceptor 发送 accept
                        for (Net net : acceptors) {
                            sendAccept(phase, choseValue, net);
                        }
                        break;
                    }
                }
            }

        }
    }

    private void printPrepareResp(PrepareResponse resp) {
        System.out.println("[acceptor-" + resp.port +  "] -> " + "[proposer-" + port + "]" + ": prepare_resp phase,"
                + " promise ->" + resp.prepareId
                + ", resp -> " + (resp.resp == 1 ? "ok" : "reject")
                + ", "  + resp.choseValue);
    }

    private void random_pause() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 3000));
        } catch (InterruptedException e) {
            e.printStackTrace();
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
                PrepareMsg prepareMsg = new PrepareMsg(seq, "127.0.0.1", port);
                prepareMsg.type = 1;
                outputStream.write(0x1);
                outputStream.write(JSON.toJSONBytes(prepareMsg));
                outputStream.flush();
                outputStream.close();
                System.out.println("prepare : [proposer-" + port + "] -> [acceptor-" + acceptorAddr.port + "] " + "phase -> "+ phase);
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
