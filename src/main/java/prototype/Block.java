package prototype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class Block {
    private final String from;
    private final String to;
    private final int amount;

    @JsonCreator
    public Block(@JsonProperty("from") String from, @JsonProperty("to") String to, @JsonProperty("amount") int amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "Block{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", amount=" + amount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Block block = (Block) o;
        return amount == block.amount &&
                Objects.equals(from, block.from) &&
                Objects.equals(to, block.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, amount);
    }
}
