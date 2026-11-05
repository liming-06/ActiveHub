package activehub;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/** Dependency-free regression checks. Run this class separately from Main. */
public final class SelfTest {
    private static int checks;
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
        System.out.println("PASS " + label);
    }
    private static void money(String expected, java.math.BigDecimal actual, String label) {
        check(Money.of(expected).compareTo(actual) == 0, label + " = " + Money.rm(actual));
    }
    private static void rejects(Runnable action, String label) {
        try { action.run(); }
        catch (IllegalArgumentException e) { check(true, label); return; }
        throw new AssertionError("Expected rejection: " + label);
    }
    private static RentalTransaction transaction(LocalDate day, String time, int people, String facilityCode) {
        SportsCentre s = new SportsCentre();
        Booking b = s.book("Li Ming", "0123456789", day, LocalTime.parse(time), people, facilityCode);
        RentalTransaction t = s.createTransaction("Li Ming", b);
        t.addItem(s.findItem(facilityCode.equals("C1") ? "F02" : "F01"), 1);
        return t;
    }
    public static void main(String[] args) throws Exception {
        Promotion a = new OffPeakSaver(), b = new TeamBookingReward(), c = new EquipmentBundleDiscount();
        check(!a.isEligible(transaction(MONDAY, "08:59", 2, "B1")), "A excludes 08:59");
        check(a.isEligible(transaction(MONDAY, "09:00", 2, "B1")), "A includes 09:00");
        check(a.isEligible(transaction(MONDAY, "15:59", 2, "B1")), "A includes 15:59");
        check(!a.isEligible(transaction(MONDAY, "16:00", 2, "B1")), "A excludes 16:00");
        check(!a.isEligible(transaction(MONDAY.plusDays(5), "10:00", 2, "B1")), "A excludes Saturday");
        check(!a.isEligible(transaction(MONDAY.plusDays(6), "10:00", 2, "B1")), "A excludes Sunday");
        SportsCentre catalogue = new SportsCentre();
        check(catalogue.getCatalogue().size() == 8, "Exactly eight catalogue entries");
        RentalTransaction offPeak = transaction(MONDAY, "10:00", 2, "B1");
        offPeak.addItem(catalogue.findItem("E01"), 1);
        offPeak.addItem(catalogue.findItem("A01"), 1);
        money("6", a.saving(offPeak), "A discounts only facility charge");
        check(!b.isEligible(transaction(MONDAY, "10:00", 7, "B1")), "B excludes seven people");
        money("40", b.saving(transaction(MONDAY, "10:00", 8, "C1")), "B includes eight people");
        money("60", b.saving(transaction(MONDAY, "10:00", 20, "C1")), "B capped at RM60");
        money("40", b.saving(transaction(MONDAY, "10:00", 20, "B1")), "B cannot exceed facility charge");
        RentalTransaction bundle = transaction(MONDAY, "16:00", 2, "B1");
        bundle.addItem(catalogue.findItem("E01"), 2);
        check(!c.isEligible(bundle), "C excludes two equipment units");
        bundle.addItem(catalogue.findItem("A01"), 3);
        bundle.addItem(catalogue.findItem("S01"), 2);
        check(!c.isEligible(bundle), "Accessories and services do not count as equipment");
        bundle.addItem(catalogue.findItem("E02"), 1);
        money("20", c.saving(bundle), "C includes three mixed equipment units");
        RentalTransaction same = transaction(MONDAY, "16:00", 2, "B1");
        same.addItem(catalogue.findItem("E01"), 3);
        check(c.isEligible(same), "C includes three units of same equipment");
        RentalTransaction all = transaction(MONDAY, "10:00", 8, "C1");
        all.addItem(catalogue.findItem("E01"), 3);
        Bill bill = new Bill(all);
        check(bill.getEligible().size() == 3, "All three eligible promotions are exposed");
        check(bill.getSelected().getCode().equals("B"), "Largest saving selected");
        money("40", bill.getDiscount(), "Discounts do not stack");
        money("8.40", bill.getServiceCharge(), "Service charge uses discounted subtotal");
        money("92.40", bill.getTotal(), "Final bill calculation");
        List<Promotion> ties = Arrays.asList(fixed("C"), fixed("B"), fixed("A"));
        check(new Bill(all, ties).getSelected().getCode().equals("A"), "Alphabetic tie-break independent of order");
        check(new Bill(all, Arrays.asList(fixed("C"), fixed("B"))).getSelected().getCode().equals("B"), "B wins B/C tie");

        SportsCentre s = new SportsCentre();
        Booking booking = s.book("Li | Ming 中文", "+60 123456789", MONDAY, LocalTime.of(10, 0), 8, "B1");
        rejects(() -> s.book("Other", "0123456", MONDAY, LocalTime.of(10, 0), 2, "B1"), "Duplicate booking rejected");
        rejects(() -> s.book("Other", "0123456", MONDAY, LocalTime.of(10, 30), 2, "B1"), "Overlapping booking rejected");
        check(s.isAvailable(s.findFacility("B1"), MONDAY, LocalTime.of(11, 0)), "Adjacent slot available");
        check(s.isAvailable(s.findFacility("B2"), MONDAY, LocalTime.of(10, 0)), "Different court available");
        rejects(() -> s.book("Other", "0123456", MONDAY, LocalTime.of(12, 0), 0, "B1"), "Zero participants rejected");
        rejects(() -> s.book("Other", "0123456", MONDAY, LocalTime.of(23, 1), 2, "B1"), "Cross-midnight slot rejected");
        RentalTransaction t = s.createTransaction("ignored", booking);
        rejects(() -> s.createTransaction("again", booking), "Duplicate transaction for booking rejected");
        rejects(() -> t.addItem(s.findItem("F02"), 1), "Wrong court package rejected");
        t.addItem(s.findItem("F01"), 1);
        rejects(() -> t.addItem(s.findItem("F01"), 1), "Duplicate facility charge rejected");
        rejects(() -> t.addItem(s.findItem("E01"), 0), "Zero quantity rejected");
        t.addItem(s.findItem("E01"), 2);
        t.addItem(s.findItem("E01"), 1);
        check(t.equipmentQuantity() == 3 && t.getLines().size() == 2, "Update merges quantities");
        rejects(() -> s.pay(t, new CashPayment(Money.of("1")), MONDAY), "Insufficient cash rejected");
        check(!t.isPaid(), "Failed payment leaves transaction open");
        String receipt = s.pay(t, new CashPayment(Money.of("50")), MONDAY);
        check(receipt.contains("RM23.60"), "Correct cash change");
        rejects(() -> s.pay(t, new CardPayment(), MONDAY), "Duplicate payment rejected");
        rejects(() -> t.addItem(s.findItem("E02"), 1), "Paid transaction cannot be edited");
        rejects(() -> s.cancelBooking(booking.getId()), "Paid booking cannot cancel without refund");
        RentalTransaction walk = s.createTransaction("Walk-in", null);
        rejects(() -> walk.addItem(s.findItem("F01"), 1), "Walk-in facility needs a booking");
        walk.addItem(s.findItem("E03"), 1);
        check(new Bill(walk).getSelected() == null, "Walk-in equipment receives no booking promotion");
        s.pay(walk, new CardPayment(), MONDAY);
        RentalTransaction wallet = s.createTransaction("Wallet", null);
        wallet.addItem(s.findItem("E04"), 1);
        s.pay(wallet, new EWalletPayment(), MONDAY);
        Booking cancelled = s.book("Cancel", "0123456", MONDAY, LocalTime.of(15, 0), 2, "B2");
        RentalTransaction voided = s.createTransaction("Cancel", cancelled);
        voided.addItem(s.findItem("F01"), 1);
        s.cancelBooking(cancelled.getId());
        check(s.isAvailable(s.findFacility("B2"), MONDAY, LocalTime.of(15, 0)), "Cancellation releases slot");
        rejects(() -> s.pay(voided, new CardPayment(), MONDAY), "Cancelled booking cannot be paid");
        String report = s.dailyReport(MONDAY);
        check(report.contains("Completed transactions: 3"), "Daily completed count");
        check(report.contains("Revenue: RM40.70"), "Daily revenue from actual payment totals");
        check(report.contains("CASH: 1") && report.contains("CARD: 1") && report.contains("E_WALLET: 1"), "All three payment method counts");
        check(report.contains("Badminton racquet [3]"), "Most-rented item based on units");
        check(s.dailyReport(MONDAY.plusDays(1)).contains("Revenue: RM0.00"), "Report filters payment date");

        Path folder = Files.createTempDirectory("activehub-check-");
        Path file = folder.resolve("state.txt");
        try {
            TextStore store = new TextStore(file);
            check(store.load().getTransactions().isEmpty(), "Missing file loads empty state");
            store.save(s);
            SportsCentre loaded = store.load();
            check(loaded.getBookings().size() == s.getBookings().size(), "Bookings persist");
            check(loaded.findBooking(booking.getId()).getCustomerName().equals("Li | Ming 中文"), "Escaped separator and Unicode persist");
            check(loaded.findTransaction(t.getId()).isPaid(), "Payment persists");
            check(loaded.findBooking(cancelled.getId()).isCancelled(), "Cancellation persists");
            check(loaded.dailyReport(MONDAY).equals(report), "Daily report identical after reload");
            store.save(loaded);
            check(store.load().dailyReport(MONDAY).equals(report), "Second save/reload stable");
            Files.writeString(file, "ACTIVEHUB|1\nBROKEN|x\n");
            try { store.load(); throw new AssertionError("Corrupt file accepted"); }
            catch (IOException e) { check(e.getMessage().contains("line 2"), "Corrupt file reports line and stops"); }
            check(Files.readString(file).contains("BROKEN"), "Corrupt file is not overwritten");
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(folder); }
        System.out.println("\nALL " + checks + " CHECKS PASSED");
    }
    private static Promotion fixed(String code) {
        return new Promotion() {
            public String getCode() { return code; }
            public String getName() { return "Tie test " + code; }
            public boolean isEligible(RentalTransaction t) { return true; }
            public java.math.BigDecimal saving(RentalTransaction t) { return Money.of("20"); }
        };
    }
}
