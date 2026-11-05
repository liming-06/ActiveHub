package activehub;

import java.math.BigDecimal;

public final class CardPayment implements PaymentMethod {
    @Override public String getName() { return "CARD"; }
    @Override public String process(BigDecimal amount) {
        return "Staff-confirmed credit/debit card payment recorded: " + Money.rm(amount);
    }
}
