package prototype;

import java.net.InetAddress;
import java.util.Arrays;

public class PrototypeMain {

    public static void main(String[] args) throws Exception {

        Block initialBlock = new Block(null, "Alice", 100);
        BlockchainController blockchainController = new BlockchainController(Arrays.asList(initialBlock));

        int httpPort = 8080;
        InetAddress loopback = InetAddress.getLoopbackAddress();
        BlockchainServer server = new BlockchainServer(blockchainController, loopback, httpPort);
        server.start();
    }
}
