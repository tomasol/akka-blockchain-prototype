package prototype;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class SingleNodeMain {

    public static void main(String... args) {
        int difficulty = 5;
        String httpServer = "127.0.0.1:8083";
        String akkaServer = "127.0.0.1:2553";
        BlockchainController blockchainController = initiateBlockchainController(difficulty);
        startHttpServer(blockchainController, httpServer);
        List<String> seedNodes = Arrays.asList("127.0.0.1:2550", "127.0.0.1:2551", "127.0.0.1:2552");
        startGossip(blockchainController, akkaServer, seedNodes);
    }



    static BlockchainController initiateBlockchainController(int difficulty) {
        Block initialBlock = Block.createInitialBlock("Alice", 100);
        return new BlockchainController(initialBlock, difficulty);
    }

    static BlockchainWebServer startHttpServer(BlockchainController blockchainController, String httpServerHostPort) {
        String[] split = httpServerHostPort.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new BlockchainWebServer(blockchainController, host, port);
    }

    static void startGossip(BlockchainController blockchainController, String akkaServer, List<String> seedNodes) {
        String[] split = akkaServer.split(":");
        String akkaAddress = split[0];
        int akkaPort = Integer.parseInt(split[1]);

        System.err.println("Starting at " + akkaAddress + ":" + akkaPort);
        Config config = ConfigFactory.parseString(String.format("" +
                "akka {\n" +
                "  actor {\n" +
                "    provider = \"cluster\"\n" +
                "  }\n" +
                "  remote {\n" +
                "    log-remote-lifecycle-events = on\n" +
                "    netty.tcp {\n" +
                "      hostname=\"%s\"\n" +
                "      port=%d\n" +

                "    }" +
                "  }\n" +
                "}\n", akkaAddress, akkaPort));
        for (int i = 0; i < seedNodes.size(); i++) {
            String remote = seedNodes.get(i);
            String seedNode = "akka.cluster.seed-nodes." + i + " = \"akka.tcp://ClusterSystem@" + remote + "\"";
            config = config.withFallback(ConfigFactory.parseString(seedNode));
        }

        ActorSystem system = ActorSystem.create("ClusterSystem", config);

        Cluster.get(system).registerOnMemberUp(() -> {
            System.err.println("registerOnMemberUp");

            system.actorOf(
                    Props.create(GossipReceiver.class, blockchainController), GossipReceiver.GOSSIP_RECEIVER);

            final ActorRef simpleClusterListener =
                    system.actorOf(Props.create(GossipSender.class), "gossip-sender");
            final FiniteDuration interval = Duration.create(2, TimeUnit.SECONDS);
            final ExecutionContext ec = system.dispatcher();
            system.scheduler().schedule(interval, interval, () ->
                    simpleClusterListener.tell(blockchainController.getBlockchain(), ActorRef.noSender()), ec);
        });
    }


}
