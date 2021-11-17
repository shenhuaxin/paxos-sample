package com;


import java.util.concurrent.atomic.AtomicInteger;

public class PaxosHelper {

    static class Net {
        public Net(String addr, int port) {
            this.addr = addr;
            this.port = port;
        }

        public String addr;
        public int port;
    }



    interface Msg{}

    static class PrepareMsg implements Msg {
        public PrepareMsg(int id) {
            this.id = id;
        }

        public PrepareMsg(int id, String host, int port) {
            this.id = id;
            this.host = host;
            this.port = port;
        }

        public int id;

        public String host;

        public int port;
    }

    static class AcceptMsg implements Msg {
        public int id;

        public String value;

        public String host;

        public int port;

        public AcceptMsg(int id, String value) {
            this.id = id;
            this.value = value;
        }

        public AcceptMsg(int id, String value, String host, int port) {
            this.id = id;
            this.value = value;
            this.host = host;
            this.port = port;
        }
    }

    static class PrepareResponse implements Msg {
        int prepareId;
        int resp;
        int choseId;
        String choseValue;

        public PrepareResponse(int prepareId, int resp, int choseId, String choseValue) {
            this.prepareId = prepareId;
            this.resp = resp;
            this.choseId = choseId;
            this.choseValue = choseValue;
        }
    }

    static class IdCreator {
        static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

        public static int getId() {
            return ATOMIC_INTEGER.incrementAndGet();
        }
    }
}
