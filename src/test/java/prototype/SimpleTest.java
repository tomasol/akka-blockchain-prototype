package prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SimpleTest {
    private final String alice = "Alice";
    private final String bob = "Bob";
    private final Block initialBlock = Block.createInitialBlock(alice, 100);
    private final int httpPort = 8080;
    private final InetAddress loopback = InetAddress.getLoopbackAddress();
    private final int difficulty = 5;

    BlockchainController blockchainController;
    BlockchainServer server;
    BlockchainClient client;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws IOException {
        blockchainController = new BlockchainController(initialBlock, difficulty);
        server = new BlockchainServer(blockchainController, loopback, httpPort);
        server.start();
        client = new BlockchainClient(loopback, httpPort);
        assertTrue(client.isUp());
    }

    @After
    public void tearDown() throws Exception {
        server.close();
        client.close();
    }

    @Test
    public void testSendingMoney() throws Exception {
        List<Block> chain = client.getChain();
        assertEquals(Arrays.asList(initialBlock), chain);
        assertEquals(100, client.getBalance(alice));
        assertEquals(0, client.getBalance(bob));
        // send money to Bob
        client.send(alice, bob, 10);
        assertEquals(90, client.getBalance(alice));
        assertEquals(10, client.getBalance(bob));
    }

    @Test
    public void testCannotGoToDebt() throws Exception {
        thrown.expect(IllegalStateException.class);
        client.send(alice, bob, 101);
        assertEquals(100, client.getBalance(alice));
    }
}
