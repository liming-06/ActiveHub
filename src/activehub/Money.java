package activehub;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");
    private Money() { }
    public static BigDecimal of(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
    public static String rm(BigDecimal value) { return "RM" + round(value).toPlainString(); }
}
