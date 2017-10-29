package prototype;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

public class BlockchainController {

    private final List<Block> blocks;

    public BlockchainController(List<Block> initialBlocks) {
        checkNotNull(initialBlocks);
        blocks = new ArrayList<>(initialBlocks);
    }

    public synchronized List<Block> getBlocks() {
        return new ArrayList<>(blocks);
    }

    public synchronized void addBlock(Block block) {
        validate(block);
        blocks.add(block);
    }

    private synchronized void validate(Block block) {
        // after operation, sender must not be in debt
        if (balanceOf(block.getFrom()) - block.getAmount() < 0) {
            throw new IllegalStateException("Cannot send this amount");
        }
    }

    public synchronized int balanceOf(String address) {
        checkNotNull(address);
        int amount = 0;
        for (Block block : blocks) {
            if (address.equals(block.getFrom())) {
                amount -= block.getAmount();
            }
            if (address.equals(block.getTo())) {
                amount += block.getAmount();
            }
        }
        return amount;
    }
}
