package prototype;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class BlockchainClient implements AutoCloseable {
    private static final ObjectMapper defaultObjectMapper =
            new ObjectMapper().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);

    private final ActorSystem system = ActorSystem.create();
    private final Materializer materializer = ActorMaterializer.create(system);
    private final Http http = Http.get(system);
    private final String urlPrefixWithoutSlash;

    public BlockchainClient(String serverAddress, int port) {
        try {
            urlPrefixWithoutSlash = new URL("http", serverAddress, port, "").toExternalForm();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private <T> T get(CompletionStage<T> completionStage, String errorHint) {
        try {
            return Uninterruptibles.getUninterruptibly(completionStage.toCompletableFuture());
        } catch (ExecutionException e) {
            throw new IllegalStateException(errorHint, e);
        }
    }

    private HttpResponse getSuccessfulResponse(String fullURL) {
        CompletionStage<HttpResponse> responseFuture =
                http.singleRequest(HttpRequest.create(fullURL), materializer);
        HttpResponse httpResponse = get(responseFuture, "Cannot get " + fullURL);
        if (httpResponse.status().isSuccess()) {
            return httpResponse;
        }
        throw new IllegalStateException("Invalid status " + httpResponse.status().intValue() + " for " + fullURL);
    }

    private String getSuccessfulResponseAsString(String fullURL) {
        HttpResponse httpResponse = getSuccessfulResponse(fullURL);
        return get(Unmarshaller.entityToString().unmarshal(httpResponse.entity(), materializer), "Cannot unmarshal response");
    }

    public void send(String from, String to, int amount) {
        getSuccessfulResponse(urlPrefixWithoutSlash + "/send?from=" + from + "&to=" + to +
                "&amount=" + amount);
    }

    public int getBalance(String account) {
        String s = getSuccessfulResponseAsString(urlPrefixWithoutSlash + "/getBalance?address=" + account);
        return Integer.parseInt(s);
    }

    public boolean isUp() {
        try {
            getSuccessfulResponse(urlPrefixWithoutSlash + "/getStatus");
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public List<Block> getChain() {
        String jsonContent = getSuccessfulResponseAsString(urlPrefixWithoutSlash + "/getChain");
        // deserialize from json
        TypeReference ref = new TypeReference<List<Block>>() {
        };
        try {
            return defaultObjectMapper.readerFor(ref).readValue(jsonContent);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot deserialize into List<Block>:" + jsonContent, e);
        }
    }

    @Override
    public void close() throws Exception {
        system.terminate();
    }
}
