package prototype;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class Block {

    private final Transaction transaction;
    private final String nonce;
    private final String parentHash;

    @JsonCreator
    public Block(@JsonProperty("transaction") Transaction transaction, @JsonProperty("nonce") String nonce,
                 @JsonProperty("parentHash") String parentHash) {
        this.transaction = checkNotNull(transaction);
        this.nonce = checkNotNull(nonce);
        this.parentHash = parentHash;
    }

    public static Block createInitialBlock(String address, int amount) {
        Transaction transaction = new Transaction("", address, amount);
        return new Block(transaction, "", "");
    }

    public Block withNonce(String nonce) {
        return new Block(transaction, nonce, parentHash);
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public String getNonce() {
        return nonce;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Block block = (Block) o;
        return Objects.equals(transaction, block.transaction) &&
                Objects.equals(nonce, block.nonce) &&
                Objects.equals(parentHash, block.parentHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transaction, nonce, parentHash);
    }

    @Override
    public String toString() {
        return "Block{" +
                "transaction=" + transaction +
                ", nonce='" + nonce + '\'' +
                ", parentHash='" + parentHash + '\'' +
                '}';
    }

    public String getParentHash() {
        return parentHash;
    }
}
