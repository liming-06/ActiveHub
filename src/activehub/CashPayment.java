package activehub;

import java.math.BigDecimal;

public final class CashPayment implements PaymentMethod {
    private final BigDecimal tendered;
    public CashPayment(BigDecimal tendered) {
        if (tendered == null || tendered.signum() < 0)
            throw new IllegalArgumentException("Cash must be non-negative.");
        this.tendered = Money.round(tendered);
    }
    @Override public String getName() { return "CASH"; }
    @Override public String process(BigDecimal amount) {
        if (tendered.compareTo(amount) < 0) throw new IllegalArgumentException("Insufficient cash.");
        return "Cash recorded. Change: " + Money.rm(tendered.subtract(amount));
    }
}
