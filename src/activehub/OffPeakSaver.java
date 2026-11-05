package activehub;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

public final class OffPeakSaver implements Promotion {
    @Override public String getCode() { return "A"; }
    @Override public String getName() { return "Off-Peak Saver"; }
    @Override public boolean isEligible(RentalTransaction t) {
        Booking b = t.getBooking();
        if (b == null || b.isCancelled() || t.facilitySubtotal().signum() == 0) return false;
        DayOfWeek day = b.getDate().getDayOfWeek();
        LocalTime time = b.getTime();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY
                && !time.isBefore(LocalTime.of(9, 0)) && time.isBefore(LocalTime.of(16, 0));
    }
    @Override public BigDecimal saving(RentalTransaction t) {
        return isEligible(t) ? Money.round(t.facilitySubtotal().multiply(new BigDecimal("0.15"))) : Money.ZERO;
    }
}
