package activehub;

import java.math.BigDecimal;

/** A line belongs to one transaction; it refers to a shared catalogue item. */
public final class RentalLine {
    private final RentalItem item;
    private int quantity;
    public RentalLine(RentalItem item, int quantity) {
        if (item == null || quantity < 1 || quantity > 999)
            throw new IllegalArgumentException("Quantity must be 1 to 999.");
        this.item = item;
        this.quantity = quantity;
    }
    public RentalItem getItem() { return item; }
    public int getQuantity() { return quantity; }
    void addQuantity(int amount) {
        if (amount < 1 || quantity + amount > 999)
            throw new IllegalArgumentException("Total item quantity must be 1 to 999.");
        quantity += amount;
    }
    public BigDecimal subtotal() { return item.getPrice().multiply(BigDecimal.valueOf(quantity)); }
}
