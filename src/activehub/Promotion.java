package activehub;

import java.math.BigDecimal;

public interface Promotion {
    String getCode();
    String getName();
    boolean isEligible(RentalTransaction transaction);
    BigDecimal saving(RentalTransaction transaction);
}
