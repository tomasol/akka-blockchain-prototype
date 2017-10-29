package prototype;

import static prototype.GossipReceiver.GOSSIP_RECEIVER;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class GossipSender extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Cluster cluster = Cluster.get(getContext().getSystem());
    private final List<String> members = new ArrayList<>();
    private final Random random = new Random();

    //subscribe to cluster changes
    @Override
    public void preStart() {
        cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
                MemberEvent.class, UnreachableMember.class);
    }

    //re-subscribe when restart
    @Override
    public void postStop() {
        cluster.unsubscribe(getSelf());
    }

    @Override
    public Receive createReceive() {
        String myself = cluster.selfMember().address().toString();
        return receiveBuilder()
                .match(MemberUp.class, mUp -> {
                    if (mUp.member().address().equals(cluster.selfAddress()) == false) {
                        members.add(mUp.member().address().toString());
                        log.info("{} MemberUp: {}, members: {}", myself, mUp.member(), members);
                    } else {
                        log.debug("{} Ignoring self MemberUp: {}, members: {}",myself, mUp.member(), members);
                    }
                })
                .match(UnreachableMember.class, mUnreachable -> {
                    members.remove(mUnreachable.member().address().toString());
                    log.info("{} UnreachableMember: {}, members: {}", myself, mUnreachable.member(), members);
                })
                .match(MemberRemoved.class, mRemoved -> {
                    members.remove(mRemoved.member().address().toString());
                    log.info("{} MemberRemoved: {}, members: {}", myself, mRemoved.member(), members);
                })
                .match(MemberEvent.class, message -> {
                    log.info("{} Ignoring MemberEvent: {}, members: {}", myself, message, members);
                })
                .match(Blockchain.class, blockchain -> {
                    log.debug("{} Got blockchain for gossipping: {}", myself, blockchain);
                    // send the blockchain to randomly selected remote node
                    Optional<String> randomMember = getRandomMember();
                    if (randomMember.isPresent()) {
                        String memberAddress = randomMember.get();
                        log.debug("{} Sending blockchain to {}", myself, memberAddress);
                        getContext().actorSelection(memberAddress + "/user/" + GOSSIP_RECEIVER).tell(blockchain, self());
                    } else {
                        log.debug("{} Not sending to any node", myself);
                    }
                })
                .build();
    }

    private Optional<String> getRandomMember() {
        if (members.size() == 0) {
            return Optional.empty();
        }
        return Optional.of(members.get(random.nextInt(members.size())));

    }

}
