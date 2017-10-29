package prototype;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class MultiNodeMain {

    public static void main(String[] args) {
        int numberOfNodes = 3;
        if (args.length == 1) {
            numberOfNodes = Integer.parseInt(args[0]);
        }

        int difficulty = 5;
        BlockchainController blockchainController = SingleNodeMain.initiateBlockchainController(difficulty);

        String localhost = InetAddress.getLoopbackAddress().getHostAddress();
        int httpPort = 8080;
        int akkaPort = 2550;
        List<String> seedNodes = new ArrayList<>();
        for (int i = 0; i < numberOfNodes + 1; i++) {
            seedNodes.add(localhost + ":" + (akkaPort + i));
        }
        for(int i = 0; i < numberOfNodes; i++) {
            String httpServer = localhost + ":" + (httpPort + i);
            SingleNodeMain.startHttpServer(blockchainController, httpServer);
            String akkaServer = localhost + ":" + (akkaPort + i);
            SingleNodeMain.startGossip(blockchainController, akkaServer, seedNodes);
        }


    }
}
