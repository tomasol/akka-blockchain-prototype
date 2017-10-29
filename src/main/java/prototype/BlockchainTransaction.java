package prototype;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Objects;

public class BlockchainTransaction implements Serializable {

    private final String from;
    private final String to;
    private final int amount;

    @JsonCreator
    public BlockchainTransaction(@JsonProperty("from") String from, @JsonProperty("to") String to, @JsonProperty("amount") int amount) {
        this.from = checkNotNull(from);
        this.to = checkNotNull(to);
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
        return "Transaction{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", amount=" + amount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockchainTransaction transaction = (BlockchainTransaction) o;
        return amount == transaction.amount &&
                Objects.equals(from, transaction.from) &&
                Objects.equals(to, transaction.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, amount);
    }
}
