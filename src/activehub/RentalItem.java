package activehub;

import java.math.BigDecimal;

/** Shared abstraction for all eight catalogue entries. */
public abstract class RentalItem {
    private final String code;
    private final String name;
    private final Category category;
    private final BigDecimal price;

    protected RentalItem(String code, String name, Category category, String price) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.price = Money.of(price);
        if (this.price.signum() < 0) throw new IllegalArgumentException("Negative price.");
    }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public abstract String getRentalUnit();
}
