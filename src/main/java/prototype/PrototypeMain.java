package prototype;

import java.net.InetAddress;
import java.util.Arrays;

public class PrototypeMain {

    public static void main(String[] args) throws Exception {
        Block initialBlock = Block.createInitialBlock("Alice", 100);
        int httpPort = 8080;
        InetAddress inetAddress = InetAddress.getLoopbackAddress();
        int difficulty = 5;
        if (args.length == 3) {
            String serverAddress = args[0];
            inetAddress = InetAddress.getByName(serverAddress);
            httpPort = Integer.parseInt(args[1]);
            difficulty = Integer.parseInt(args[2]);
        }
        BlockchainController blockchainController = new BlockchainController(initialBlock, difficulty);
        BlockchainServer server = new BlockchainServer(blockchainController, inetAddress, httpPort);
        server.start();
    }
}
