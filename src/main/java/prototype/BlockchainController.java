package prototype;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockchainController {

    private final List<Block> blocks = new ArrayList<>();
    private final Map<String /*address*/, Integer> currentBalanceCache = new HashMap<>();
    private final int difficulty;

    public BlockchainController(Block initialBlock, int difficulty) {
        checkNotNull(initialBlock);
        this.difficulty = difficulty;

        // no validation for initial block
        blocks.add(initialBlock);
        Transaction transaction = initialBlock.getTransaction();
        checkArgument("".equals(transaction.getFrom()));
        currentBalanceCache.put(transaction.getTo(), transaction.getAmount());
    }

    public synchronized List<Block> getBlocks() {
        return new ArrayList<>(blocks);
    }

    public synchronized void addBlock(Block block) {
        // validate
        // compute hash
        if (CryptoUtils.isBlockValid(block, difficulty) == false) {
            throw new IllegalArgumentException("Invalid block");
        }
        // must link to the latest hash
        if (getTopmostHash().equals(block.getParentHash()) == false) {
            throw new IllegalArgumentException("Invalid parent hash");
        }
        // update balance cache, fail if sender has negative balance

        Transaction transaction = block.getTransaction();
        int senderBalance = balanceOf(transaction.getFrom()) - transaction.getAmount();
        // after operation, sender must not be in debt
        if (senderBalance < 0) {
            throw new IllegalStateException("Cannot send this amount");
        }
        currentBalanceCache.put(transaction.getFrom(), senderBalance);
        int receiverBalance = balanceOf(transaction.getTo()) + transaction.getAmount();
        currentBalanceCache.put(transaction.getTo(), receiverBalance);

        blocks.add(block);
    }


    public synchronized int balanceOf(String address) {
        checkNotNull(address);
        Integer value = currentBalanceCache.get(address);
        return value == null ? 0 : value;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public synchronized String getTopmostHash() {
        return CryptoUtils.sha256(blocks.get(blocks.size() - 1));
    }
}
