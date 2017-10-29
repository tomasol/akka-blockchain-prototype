package prototype;

import static akka.http.javadsl.server.Directives.complete;
import static akka.http.javadsl.server.Directives.parameter;
import static akka.http.javadsl.server.Directives.path;
import static akka.http.javadsl.server.Directives.route;
import static com.google.common.base.Preconditions.checkNotNull;

import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.unmarshalling.StringUnmarshallers;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import java.util.function.Supplier;

public class BlockchainWebServer implements AutoCloseable {
    private final BlockchainController blockchainController;
    private final String listeningHost;
    private final int listeningHttpPort;
    private final ActorSystem system;

    public BlockchainWebServer(BlockchainController blockchainController, String listeningHost, int listeningHttpPort) {
        this.blockchainController = checkNotNull(blockchainController);
        this.listeningHttpPort = listeningHttpPort;
        this.listeningHost = listeningHost;
        this.system = ActorSystem.create();

        final Materializer materializer = ActorMaterializer.create(system);
        Http http = Http.get(system);

        ConnectHttp connectHttp = ConnectHttp.toHost(listeningHost, listeningHttpPort);
        http.bindAndHandle(createRoute().flow(system, materializer), connectHttp, materializer);
        System.err.println("Server online at http://" + listeningHost + ":" + listeningHttpPort);
    }

    private Route createRoute() {

        Supplier<Route> getChain = () ->
                complete(StatusCodes.OK, blockchainController.getBlocks(), Jackson.marshaller());

        Supplier<Route> getBalance = () ->
                parameter(StringUnmarshallers.STRING, "address", address ->
                        complete(String.valueOf(blockchainController.balanceOf(address))));

        Supplier<Route> send = () ->
                parameter(StringUnmarshallers.STRING, "from", from ->
                        parameter(StringUnmarshallers.STRING, "to", to ->
                                parameter(StringUnmarshallers.INTEGER, "amount", amount -> {
                                    BlockchainTransaction transaction = new BlockchainTransaction(from, to, amount);
                                    Block block = new Block(transaction, "", blockchainController.getTopmostHash());
                                    block = CryptoUtils.computeValidBlock(block::withNonce,
                                            blockchainController.getDifficulty());
                                    blockchainController.addBlock(block);
                                    return complete(StatusCodes.OK);
                                })));

        return route(
                path("getChain", getChain),
                path("getBalance", getBalance),
                path("send", send),
                path("getStatus", () -> complete(StatusCodes.OK))
        );
    }

    @Override
    public void close() throws Exception {
        system.terminate();
    }
}
