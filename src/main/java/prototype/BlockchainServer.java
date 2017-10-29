package prototype;

import static akka.http.javadsl.server.Directives.complete;
import static akka.http.javadsl.server.Directives.get;
import static akka.http.javadsl.server.Directives.getFromResource;
import static akka.http.javadsl.server.Directives.parameter;
import static akka.http.javadsl.server.Directives.path;
import static akka.http.javadsl.server.Directives.pathPrefix;
import static akka.http.javadsl.server.Directives.reject;
import static akka.http.javadsl.server.Directives.route;
import static com.google.common.base.Preconditions.checkNotNull;

import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCode;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.unmarshalling.StringUnmarshallers;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import java.io.IOException;
import java.net.InetAddress;
import java.util.function.Supplier;

public class BlockchainServer implements AutoCloseable {
    private final BlockchainController blockchainController;
    private final InetAddress inetAddress;
    private final int httpPort;
    private final ActorSystem system;

    public BlockchainServer(BlockchainController blockchainController, InetAddress inetAddress, int httpPort) {
        this.blockchainController = checkNotNull(blockchainController);
        this.httpPort = httpPort;
        this.inetAddress = inetAddress;
        this.system = ActorSystem.create();
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
                                Block block = new Block(from, to, amount);
                            blockchainController.addBlock(block);
                            return complete(StatusCodes.OK);
                        })));

        return route(
                path("getChain", getChain),
                path("getBalance", getBalance),
                path("send", send),
                get(() -> complete(StatusCodes.OK))
        );
    }

    public void start() throws IOException {
        try {
            final Materializer materializer = ActorMaterializer.create(system);
            Http http = Http.get(system);

            ConnectHttp connectHttp = ConnectHttp.toHost("127.0.0.1", httpPort);
            http.bindAndHandle(createRoute().flow(system, materializer), connectHttp, materializer);
            System.out.println("Server online at http://" + inetAddress.getHostName() + ":" + httpPort);
        } catch (RuntimeException e) {
            system.terminate();
        }
    }

    @Override
    public void close() throws Exception {
        system.terminate();
    }
}
