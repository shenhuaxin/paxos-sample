package main.java.com;


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



    static class Msg {
        // 1 prepare
        // 2 prepare_response
        // 3 accept
        int type;

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }
    }

    static class PrepareMsg extends Msg {

        public PrepareMsg() {
        }

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


        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    static class AcceptMsg extends Msg {

        public AcceptMsg() {
        }

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

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    static class PrepareResponse extends Msg {

        public PrepareResponse() {
        }

        int prepareId;
        int resp;
        int choseId;
        String choseValue;

        int port;

        public PrepareResponse(int prepareId, int resp, int choseId, String choseValue, int port) {
            this.prepareId = prepareId;
            this.resp = resp;
            this.choseId = choseId;
            this.choseValue = choseValue;
            this.port = port;
        }

        public int getPrepareId() {
            return prepareId;
        }

        public void setPrepareId(int prepareId) {
            this.prepareId = prepareId;
        }

        public int getResp() {
            return resp;
        }

        public void setResp(int resp) {
            this.resp = resp;
        }

        public int getChoseId() {
            return choseId;
        }

        public void setChoseId(int choseId) {
            this.choseId = choseId;
        }

        public String getChoseValue() {
            return choseValue;
        }

        public void setChoseValue(String choseValue) {
            this.choseValue = choseValue;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    static class IdCreator {
        static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

        public static int getId(int port) {
            int i = ATOMIC_INTEGER.incrementAndGet();
            System.out.println("[proposer-" + port + "] 获取Id -> " + i);
            return i;
        }
    }
}
