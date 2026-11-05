package activehub;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RentalTransaction {
    private final String id;
    private final String customerName;
    private final Booking booking;
    private final List<RentalLine> lines = new ArrayList<>();
    private LocalDate paidDate;
    private String paymentMethod;
    private BigDecimal paidAmount;

    public RentalTransaction(String id, String customerName, Booking booking) {
        if (customerName == null || customerName.trim().isEmpty())
            throw new IllegalArgumentException("Customer name is required.");
        this.id = id;
        this.customerName = customerName.trim();
        this.booking = booking;
    }
    public String getId() { return id; }
    public String getCustomerName() { return customerName; }
    public Booking getBooking() { return booking; }
    public List<RentalLine> getLines() { return Collections.unmodifiableList(lines); }
    public boolean isPaid() { return paidDate != null; }
    public LocalDate getPaidDate() { return paidDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public boolean isVoided() { return booking != null && booking.isCancelled(); }
    public void addItem(RentalItem item, int quantity) {
        if (isPaid()) throw new IllegalArgumentException("A paid transaction cannot be changed.");
        if (isVoided()) throw new IllegalArgumentException("This booking is cancelled.");
        if (quantity < 1 || quantity > 999) throw new IllegalArgumentException("Quantity must be 1 to 999.");
        if (item instanceof FacilityPackage) {
            FacilityPackage pack = (FacilityPackage) item;
            if (booking == null || !pack.getFacilityType().equals(booking.getFacility().getType()))
                throw new IllegalArgumentException("Choose a matching facility booking first.");
            if (quantity != 1 || facilitySubtotal().signum() > 0)
                throw new IllegalArgumentException("One booked hour allows one facility package.");
        }
        for (RentalLine line : lines) {
            if (line.getItem().getCode().equals(item.getCode())) {
                line.addQuantity(quantity);
                return;
            }
        }
        lines.add(new RentalLine(item, quantity));
    }
    public BigDecimal subtotal() {
        BigDecimal total = Money.ZERO;
        for (RentalLine line : lines) total = total.add(line.subtotal());
        return total;
    }
    public BigDecimal facilitySubtotal() {
        BigDecimal total = Money.ZERO;
        for (RentalLine line : lines)
            if (line.getItem().getCategory() == Category.FACILITY) total = total.add(line.subtotal());
        return total;
    }
    public int equipmentQuantity() {
        int count = 0;
        for (RentalLine line : lines)
            if (line.getItem().getCategory() == Category.EQUIPMENT) count += line.getQuantity();
        return count;
    }
    public void markPaid(LocalDate date, String method, BigDecimal amount) {
        if (isPaid()) throw new IllegalArgumentException("Already paid.");
        if (isVoided() || lines.isEmpty()) throw new IllegalArgumentException("No payable items.");
        if (booking != null && facilitySubtotal().signum() == 0)
            throw new IllegalArgumentException("Add the booked facility package before payment.");
        if (date == null || method == null || amount == null || amount.signum() < 0)
            throw new IllegalArgumentException("Invalid payment record.");
        paidDate = date;
        paymentMethod = method;
        paidAmount = Money.round(amount);
    }
}
