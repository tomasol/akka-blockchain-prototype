package prototype;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class CryptoUtils {

    public static String sha256(Block block) {
        String input = block.toString();
        return Hashing.sha256().hashString(input, StandardCharsets.UTF_8).toString();
    }


    /**
     * Proof of work: Produce nonce such that the hash meets the expected difficulty.
     */
    public static Block computeValidBlock(Function<String, Block> withNonce, int dificulty) {
        int counter = 1;
        System.out.println();
        while (true) {
            if (counter % 100 == 0) {
                System.out.print(".");
            }
            if (counter % 10_000 == 0) {
                System.out.println();
            }
            Block block = withNonce.apply(String.valueOf(counter));
            if (isBlockValid(block, dificulty)) {
                System.out.printf("%nGot hash %s%n", sha256(block));
                return block;
            }
            counter++;
        }
    }

    /**
     * Count number of leading zeros inside sha256 hash. Difficulty is minimal accepted number of zeros.
     */
    public static boolean isBlockValid(Block block, int difficulty) {
        String hash = sha256(block);
        checkState(hash.length() >= difficulty);
        String zeros = Strings.repeat("0", difficulty);
        return hash.startsWith(zeros);
    }
}
