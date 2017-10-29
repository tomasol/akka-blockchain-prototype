package prototype;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class GossipReceiver extends AbstractActor {
    public static final String GOSSIP_RECEIVER = "gossip-receiver";

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final BlockchainController blockchainController;
    private final Cluster cluster = Cluster.get(getContext().getSystem());

    public GossipReceiver(BlockchainController blockchainController) {
        this.blockchainController = blockchainController;
    }

    @Override
    public Receive createReceive() {
        String myself = cluster.selfMember().address().toString();
        return receiveBuilder()
                .match(Blockchain.class, blockchain -> {
                    log.debug("{} Got blockchain from {}: {}", myself, sender(), blockchain);
                    boolean success = blockchainController.offer(blockchain);
                    if (success) {
                        log.info("{} Updated blockchain from {}", myself, sender().path());
                    }
                })
                .build();
    }
}
