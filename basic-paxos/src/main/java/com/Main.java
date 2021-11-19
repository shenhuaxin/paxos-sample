package main.java.com;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Main {


    List<Proposer> proposers = new ArrayList<>();
    List<Acceptor> acceptors = new ArrayList<>();


    void mockCluster(int pNum, int aNum) {

        int aPort = 17000;
        for (int i = 0; i < aNum; i++) {
            acceptors.add(new Acceptor(aPort++));
        }

        List<PaxosHelper.Net> nets = acceptors.stream()
                .map(acceptor -> new PaxosHelper.Net("127.0.0.1", acceptor.port))
                .collect(Collectors.toList());

        int pPort = 18000;
        for (int i = 0; i < pNum; i++) {
            proposers.add(new Proposer(nets, pPort++));
        }

    }

    public static void main(String[] args) throws InterruptedException {
        Main main = new Main();
        main.mockCluster(3, 5);

        for (Acceptor acceptor : main.acceptors) {
            acceptor.start();
        }
        Thread.sleep(100);
        System.out.println("<=======================================================>");
        for (Proposer proposer : main.proposers) {
            proposer.start();
        }


        Thread.sleep(100000L);
    }
}
