package activehub;

import java.math.BigDecimal;

/** Local payment recording only: no real card or wallet connection. */
public interface PaymentMethod {
    String getName();
    String process(BigDecimal amount);
}
