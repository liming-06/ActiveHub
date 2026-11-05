package activehub;

import java.math.BigDecimal;

public final class TeamBookingReward implements Promotion {
    @Override public String getCode() { return "B"; }
    @Override public String getName() { return "Team Booking Reward"; }
    @Override public boolean isEligible(RentalTransaction t) {
        return t.getBooking() != null && !t.isVoided()
                && t.getBooking().getParticipants() >= 8 && t.facilitySubtotal().signum() > 0;
    }
    @Override public BigDecimal saving(RentalTransaction t) {
        if (!isEligible(t)) return Money.ZERO;
        return BigDecimal.valueOf(t.getBooking().getParticipants()).multiply(Money.of("5"))
                .min(Money.of("60")).min(t.facilitySubtotal());
    }
}
