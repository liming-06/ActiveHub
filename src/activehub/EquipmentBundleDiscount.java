package activehub;

import java.math.BigDecimal;

public final class EquipmentBundleDiscount implements Promotion {
    @Override public String getCode() { return "C"; }
    @Override public String getName() { return "Equipment Bundle Discount"; }
    @Override public boolean isEligible(RentalTransaction t) {
        return t.getBooking() != null && !t.isVoided()
                && t.facilitySubtotal().signum() > 0 && t.equipmentQuantity() >= 3;
    }
    @Override public BigDecimal saving(RentalTransaction t) {
        return isEligible(t) ? Money.of("20").min(t.subtotal()) : Money.ZERO;
    }
}
