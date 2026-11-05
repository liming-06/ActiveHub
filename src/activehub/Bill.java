package activehub;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Evaluates all strategies and applies exactly one; never stacks discounts. */
public final class Bill {
    private final List<Promotion> eligible = new ArrayList<>();
    private final Promotion selected;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal serviceCharge;
    private final BigDecimal total;

    public Bill(RentalTransaction transaction) {
        this(transaction, Arrays.asList(new OffPeakSaver(), new TeamBookingReward(), new EquipmentBundleDiscount()));
    }
    public Bill(RentalTransaction transaction, List<Promotion> promotions) {
        Promotion best = null;
        BigDecimal bestSaving = Money.ZERO;
        for (Promotion promotion : promotions) {
            if (!promotion.isEligible(transaction)) continue;
            eligible.add(promotion);
            BigDecimal saving = promotion.saving(transaction);
            if (best == null || saving.compareTo(bestSaving) > 0
                    || (saving.compareTo(bestSaving) == 0 && promotion.getCode().compareTo(best.getCode()) < 0)) {
                best = promotion;
                bestSaving = saving;
            }
        }
        selected = best;
        subtotal = transaction.subtotal();
        discount = bestSaving.min(subtotal);
        // Assignment does not specify the base: use the discounted subtotal.
        BigDecimal net = subtotal.subtract(discount);
        serviceCharge = Money.round(net.multiply(new BigDecimal("0.10")));
        total = net.add(serviceCharge);
    }
    public List<Promotion> getEligible() { return Collections.unmodifiableList(eligible); }
    public Promotion getSelected() { return selected; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getServiceCharge() { return serviceCharge; }
    public BigDecimal getTotal() { return total; }
}
