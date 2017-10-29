package prototype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Blockchain implements Serializable {
    private final List<Block> blocks;

    @JsonCreator
    public Blockchain(@JsonProperty("blocks") List<Block> blocks) {
        this.blocks = new ArrayList<>(blocks);
    }

    public synchronized List<Block> getBlocks() {
        return new ArrayList<>(blocks);
    }

    public synchronized void add(Block block) {
        blocks.add(block);
    }

    public synchronized String getTopmostHash() {
        return CryptoUtils.sha256(blocks.get(blocks.size() - 1));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Blockchain that = (Blockchain) o;
        return Objects.equals(blocks, that.blocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blocks);
    }

    @Override
    public String toString() {
        return "Blockchain{" +
                "blocks=" + blocks +
                '}';
    }

    public synchronized int size() {
        return blocks.size();
    }

    public synchronized Block getBlock(int i) {
        return blocks.get(i);
    }

    public synchronized void replaceWith(Blockchain remoteBlockchain) {
        blocks.clear();
        blocks.addAll(remoteBlockchain.blocks);
    }
}
