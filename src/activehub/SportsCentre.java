package activehub;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SportsCentre {
    private final List<RentalItem> catalogue = new ArrayList<>();
    private final List<Facility> facilities = new ArrayList<>();
    private final List<Booking> bookings = new ArrayList<>();
    private final List<RentalTransaction> transactions = new ArrayList<>();

    public SportsCentre() {
        catalogue.add(new Equipment("E01", "Badminton racquet", "8"));
        catalogue.add(new Equipment("E02", "Shuttlecock set", "6"));
        catalogue.add(new Equipment("E03", "Basketball", "10"));
        catalogue.add(new Equipment("E04", "Training bib", "3"));
        catalogue.add(new FacilityPackage("F01", "Badminton court package", "40", "BADMINTON"));
        catalogue.add(new FacilityPackage("F02", "Basketball court package", "100", "BASKETBALL"));
        catalogue.add(new AddOn("A01", "Sports towel", Category.ACCESSORY, "5"));
        catalogue.add(new AddOn("S01", "Locker service", Category.SERVICE, "4"));
        facilities.add(new Facility("B1", "BADMINTON", "Badminton Court 1"));
        facilities.add(new Facility("B2", "BADMINTON", "Badminton Court 2"));
        facilities.add(new Facility("C1", "BASKETBALL", "Basketball Court 1"));
    }
    public List<RentalItem> getCatalogue() { return Collections.unmodifiableList(catalogue); }
    public List<Facility> getFacilities() { return Collections.unmodifiableList(facilities); }
    public List<Booking> getBookings() { return Collections.unmodifiableList(bookings); }
    public List<RentalTransaction> getTransactions() { return Collections.unmodifiableList(transactions); }
    private String nextId(String prefix) { return prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); }
    public RentalItem findItem(String code) {
        for (RentalItem item : catalogue) if (item.getCode().equalsIgnoreCase(code)) return item;
        throw new IllegalArgumentException("Unknown item code.");
    }
    public Facility findFacility(String code) {
        for (Facility facility : facilities) if (facility.getCode().equalsIgnoreCase(code)) return facility;
        throw new IllegalArgumentException("Unknown facility code.");
    }
    public Booking findBooking(String id) {
        for (Booking b : bookings) if (b.getId().equalsIgnoreCase(id)) return b;
        throw new IllegalArgumentException("Booking not found.");
    }
    public RentalTransaction findTransaction(String id) {
        for (RentalTransaction t : transactions) if (t.getId().equalsIgnoreCase(id)) return t;
        throw new IllegalArgumentException("Transaction not found.");
    }
    public boolean isAvailable(Facility facility, LocalDate date, LocalTime time) {
        for (Booking b : bookings)
            if (b.getFacility().getCode().equals(facility.getCode()) && b.overlaps(date, time)) return false;
        return true;
    }
    public Booking book(String name, String contact, LocalDate date, LocalTime time, int people, String code) {
        Facility facility = findFacility(code);
        Booking b = new Booking(nextId("B"), name, contact, date, time, people, facility);
        if (!isAvailable(facility, date, time)) throw new IllegalArgumentException("Facility already booked in that hour.");
        bookings.add(b);
        return b;
    }
    public void cancelBooking(String id) {
        Booking b = findBooking(id);
        if (b.isCancelled()) throw new IllegalArgumentException("Booking already cancelled.");
        for (RentalTransaction t : transactions)
            if (t.getBooking() == b && t.isPaid())
                throw new IllegalArgumentException("Paid booking: cancellation needs a refund; refunds are outside this project.");
        b.cancel();
    }
    public RentalTransaction createTransaction(String name, Booking booking) {
        if (booking != null) {
            if (booking.isCancelled()) throw new IllegalArgumentException("Booking cancelled.");
            for (RentalTransaction t : transactions)
                if (t.getBooking() == booking) throw new IllegalArgumentException("Booking already has transaction " + t.getId() + ". Use Update.");
        }
        RentalTransaction t = new RentalTransaction(nextId("T"), booking == null ? name : booking.getCustomerName(), booking);
        transactions.add(t);
        return t;
    }
    public String pay(RentalTransaction t, PaymentMethod method, LocalDate date) {
        if (!transactions.contains(t)) throw new IllegalArgumentException("Unknown transaction.");
        if (t.isPaid()) throw new IllegalArgumentException("Transaction already paid.");
        if (t.isVoided() || t.getLines().isEmpty()) throw new IllegalArgumentException("No payable items.");
        if (t.getBooking() != null && t.facilitySubtotal().signum() == 0)
            throw new IllegalArgumentException("Add the booked facility package before payment.");
        Bill bill = new Bill(t);
        String receipt = method.process(bill.getTotal()); // Runtime polymorphism.
        t.markPaid(date, method.getName(), bill.getTotal());
        return receipt;
    }
    // Package-private hydration is used only by TextStore.
    void restoreBooking(Booking b) { bookings.add(b); }
    void restoreTransaction(RentalTransaction t) { transactions.add(t); }

    public String dailyReport(LocalDate date) {
        int active = 0, cancelled = 0, completed = 0;
        BigDecimal revenue = Money.ZERO;
        Map<String, Integer> quantities = new LinkedHashMap<>();
        Map<String, Integer> methodCount = new LinkedHashMap<>();
        Map<String, BigDecimal> methodRevenue = new LinkedHashMap<>();
        for (String method : new String[]{"CASH", "CARD", "E_WALLET"}) {
            methodCount.put(method, 0);
            methodRevenue.put(method, Money.ZERO);
        }
        for (Booking b : bookings) if (b.getDate().equals(date)) {
            if (b.isCancelled()) cancelled++; else active++;
        }
        for (RentalTransaction t : transactions) {
            if (!t.isPaid() || !t.getPaidDate().equals(date)) continue;
            completed++;
            revenue = revenue.add(t.getPaidAmount());
            String method = t.getPaymentMethod();
            methodCount.put(method, methodCount.getOrDefault(method, 0) + 1);
            methodRevenue.put(method, methodRevenue.getOrDefault(method, Money.ZERO).add(t.getPaidAmount()));
            for (RentalLine line : t.getLines()) {
                String code = line.getItem().getCode();
                quantities.put(code, quantities.getOrDefault(code, 0) + line.getQuantity());
            }
        }
        int max = 0;
        for (int quantity : quantities.values()) max = Math.max(max, quantity);
        StringBuilder out = new StringBuilder("DAILY REPORT " + date + "\n");
        out.append("Facility bookings scheduled: ").append(active + cancelled)
                .append(" (active: ").append(active).append(", cancelled: ").append(cancelled).append(")\n");
        out.append("Completed transactions: ").append(completed).append("\nRevenue: ").append(Money.rm(revenue)).append("\n");
        out.append("Most frequently rented (units in paid transactions): ");
        if (max == 0) out.append("None");
        else for (Map.Entry<String, Integer> entry : quantities.entrySet())
            if (entry.getValue() == max) out.append(findItem(entry.getKey()).getName()).append(" [").append(max).append("] ");
        out.append("\nPayment methods:\n");
        for (String method : methodCount.keySet()) out.append("  ").append(method).append(": ")
                .append(methodCount.get(method)).append(" / ").append(Money.rm(methodRevenue.get(method))).append("\n");
        return out.toString();
    }
}
