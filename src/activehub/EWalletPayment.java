package activehub;

import java.math.BigDecimal;

public final class EWalletPayment implements PaymentMethod {
    @Override public String getName() { return "E_WALLET"; }
    @Override public String process(BigDecimal amount) {
        return "Staff-confirmed e-wallet payment recorded: " + Money.rm(amount);
    }
}
