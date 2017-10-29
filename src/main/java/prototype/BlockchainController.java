package prototype;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlockchainController {
    private static final Logger LOG = Logger.getLogger(BlockchainController.class.getName());

    private final Blockchain blockchain;
    private final Map<String /*address*/, Integer> currentBalanceCache = new HashMap<>();
    private final int difficulty;

    public BlockchainController(Block initialBlock, int difficulty) {
        checkNotNull(initialBlock);
        this.difficulty = difficulty;

        // no validation for initial block
        List<Block> blocks = Lists.newArrayList(initialBlock);
        this.blockchain = new Blockchain(blocks);
        BlockchainTransaction transaction = initialBlock.getTransaction();
        checkArgument("".equals(transaction.getFrom()));
        currentBalanceCache.put(transaction.getTo(), transaction.getAmount());
    }

    @Deprecated
    public synchronized List<Block> getBlocks() {
        return blockchain.getBlocks();
    }

    public Blockchain getBlockchain() {
        return new Blockchain(getBlocks());
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
        updateBalanceCache(block.getTransaction(), currentBalanceCache);

        blockchain.add(block);
    }

    private static void updateBalanceCache(BlockchainTransaction transaction, Map<String /*address*/, Integer> balanceCache) {
        int senderBalance = balanceOf(transaction.getFrom(), balanceCache) - transaction.getAmount();
        // after operation, sender must not be in debt
        if (senderBalance < 0) {
            throw new IllegalStateException("Cannot send this amount");
        }
        balanceCache.put(transaction.getFrom(), senderBalance);
        int receiverBalance = balanceOf(transaction.getTo(), balanceCache) + transaction.getAmount();
        balanceCache.put(transaction.getTo(), receiverBalance);
    }

    private static int balanceOf(String address, Map<String /*address*/, Integer> balanceCache) {
        checkNotNull(address);
        Integer value = balanceCache.get(address);
        return value == null ? 0 : value;
    }


    public synchronized int balanceOf(String address) {
        return balanceOf(address, currentBalanceCache);
    }

    public int getDifficulty() {
        return difficulty;
    }

    public synchronized String getTopmostHash() {
        return blockchain.getTopmostHash();
    }

    /**
     * Try to replace local chain with offered.
     *
     * @return true iif chain was replaced
     */
    public synchronized boolean offer(Blockchain remoteBlockchain) {
        // in order to accept remote blockchain, it has to be longer, valid, and there must be a common ancestor.
        if (remoteBlockchain.size() > blockchain.size()) {
            // is it valid?
            // genesis block must be the same
            if (blockchain.getBlock(0).equals(remoteBlockchain.getBlock(0)) == false) {
                LOG.log(Level.WARNING, "Cannot merge chains, wrong genesis block");
                return false;
            }
            // each block must point to parent hash, with correct leading zeros according to difficulty
            int lastCommonBlock = 0;
            for (int i = 0; i < remoteBlockchain.size() - 1; i++) {
                int parentIdx = i;
                int childIdx = parentIdx + 1;
                Block remoteParentBlock = remoteBlockchain.getBlock(parentIdx);
                Block remoteChildBlock = remoteBlockchain.getBlock(childIdx);
                String parentHash = CryptoUtils.sha256(remoteParentBlock);
                if (parentHash.equals(remoteChildBlock.getParentHash()) == false) {
                    LOG.log(Level.WARNING, "Wrong parent hash for remote block {0}, expected {1}, got {2}",
                            new Object[]{childIdx, remoteChildBlock.getParentHash(), parentHash});
                    return false;
                }
                if (lastCommonBlock == parentIdx && blockchain.size() > childIdx && remoteChildBlock.equals(blockchain.getBlock(childIdx))) {
                    lastCommonBlock = childIdx;
                } else {
                    // only validate remote blocks that are not common to local blockchain
                    if (CryptoUtils.isBlockValid(remoteChildBlock, difficulty) == false) {
                        LOG.log(Level.WARNING, "Invalid block {0}", new Object[]{childIdx});
                        return false;
                    }
                }
            }
            // We need validate all blocks from common ancestor onwards.
            // No address can have negative balance on each transaction.
            Map<String /*address*/, Integer> newBalanceCache = new HashMap<>(currentBalanceCache);
            // undo blocks that should be discarded
            for (int i = blockchain.size() - 1; i > lastCommonBlock; i--) {
                Block revertedBlock = blockchain.getBlock(i);
                BlockchainTransaction revertedTransaction = revertedBlock.getTransaction();
                int fromBalance = newBalanceCache.get(revertedTransaction.getFrom()) + revertedTransaction.getAmount();
                newBalanceCache.put(revertedTransaction.getFrom(), fromBalance);
                int toBalance = newBalanceCache.get(revertedTransaction.getTo()) - revertedTransaction.getAmount();
                newBalanceCache.put(revertedTransaction.getTo(), toBalance);
            }
            // start applying remote blocks
            for (int i = lastCommonBlock + 1; i < remoteBlockchain.size(); i++) {
                Block newRemoteBlock = remoteBlockchain.getBlock(i);
                BlockchainTransaction transaction = newRemoteBlock.getTransaction();
                int mustNotBeNegative = balanceOf(transaction.getFrom(), newBalanceCache) - transaction.getAmount();
                // after operation, sender must not be in debt
                if (mustNotBeNegative < 0) {
                    LOG.log(Level.WARNING, "Rejecting - invalid block {0}", new Object[]{i});
                    return false;
                }
                updateBalanceCache(transaction, newBalanceCache);
            }
            // all ok
            this.blockchain.replaceWith(remoteBlockchain);
            this.currentBalanceCache.clear();
            this.currentBalanceCache.putAll(newBalanceCache);
            return true;
        }
        return false;
    }
}
