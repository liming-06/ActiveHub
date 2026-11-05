package activehub;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.NoSuchElementException;
import java.util.Scanner;

public final class Main {
    private final Scanner input = new Scanner(System.in);
    private final TextStore store;
    private final SportsCentre centre;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private Main(TextStore store, SportsCentre centre) { this.store = store; this.centre = centre; }
    public static void main(String[] args) {
        Path file = args.length == 0 ? Path.of("data", "activehub.txt") : Path.of(args[0]);
        TextStore store = new TextStore(file);
        SportsCentre centre;
        try { centre = store.load(); }
        catch (IOException e) {
            System.err.println("Cannot load data: " + e.getMessage());
            System.err.println("Existing data was preserved. Fix the file or choose a different data-file path.");
            return;
        }
        try { new Main(store, centre).run(); }
        catch (IOException e) { System.err.println("Stopped because data could not be saved: " + e.getMessage()); }
    }
    private String read(String prompt) {
        System.out.print(prompt);
        String value = input.nextLine().trim();
        if (value.isEmpty()) throw new IllegalArgumentException("A value is required.");
        return value;
    }
    private int number(String prompt) {
        try { return Integer.parseInt(read(prompt)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Enter a whole number."); }
    }
    private LocalDate date(String prompt) {
        try { return LocalDate.parse(read(prompt)); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("Use a real date in YYYY-MM-DD format."); }
    }
    private LocalTime time() {
        try { return LocalTime.parse(read("Start time (HH:mm, 24-hour): "), TIME); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("Use HH:mm, e.g. 09:00 or 15:59."); }
    }
    private void save() throws IOException { store.save(centre); }
    private void run() throws IOException {
        System.out.println("ActiveHub Sports Centre | one-hour court bookings");
        System.out.println("Data file: " + store.getFile());
        while (true) {
            System.out.println("\n===== ACTIVEHUB SYSTEM =====");
            System.out.println("1. Facility bookings / availability\n2. View rental catalogue\n3. Create / update rental transaction");
            System.out.println("4. View bill / apply best promotion\n5. Make payment\n6. Daily report\n7. Exit");
            try {
                switch (number("Choose: ")) {
                    case 1: bookings(); break;
                    case 2: catalogue(); break;
                    case 3: transactions(); break;
                    case 4: printBill(selectTransaction()); break;
                    case 5: payment(); break;
                    case 6: System.out.println(centre.dailyReport(date("Report date (YYYY-MM-DD): "))); break;
                    case 7: save(); System.out.println("Data saved. Goodbye, Li Ming."); return;
                    default: System.out.println("Choose 1 to 7.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Cannot continue: " + e.getMessage());
            } catch (NoSuchElementException e) {
                save();
                System.out.println("\nInput ended. Data saved.");
                return;
            } catch (IOException e) {
                // Stop immediately so the user can recover the current state before more changes.
                System.err.println("SAVE FAILED: " + e.getMessage());
                System.err.println("Unsaved changes were not stored. Check the data folder permissions before restarting.");
                throw e;
            }
        }
    }
    private void bookings() throws IOException {
        System.out.println("1. Create booking  2. View bookings  3. Cancel booking  4. Check availability");
        switch (number("Choose: ")) {
            case 1:
                String name = read("Customer name: ");
                String contact = read("Contact number: ");
                LocalDate date = date("Booking date (YYYY-MM-DD): ");
                LocalTime time = time();
                int people = number("Participants: ");
                showFacilities(date, time);
                Booking b = centre.book(name, contact, date, time, people, read("Facility code: "));
                save(); System.out.println("Booking created: " + b);
                break;
            case 2:
                if (centre.getBookings().isEmpty()) System.out.println("No bookings yet.");
                for (Booking booking : centre.getBookings()) System.out.println(booking);
                break;
            case 3:
                centre.cancelBooking(read("Booking ID: ")); save();
                System.out.println("Booking cancelled; linked unpaid transaction is void."); break;
            case 4:
                showFacilities(date("Booking date (YYYY-MM-DD): "), time()); break;
            default: throw new IllegalArgumentException("Choose 1 to 4.");
        }
    }
    private void showFacilities(LocalDate date, LocalTime time) {
        for (Facility f : centre.getFacilities())
            System.out.println(f + " | " + (centre.isAvailable(f, date, time) ? "AVAILABLE" : "OCCUPIED"));
    }
    private void catalogue() {
        System.out.printf("%-5s %-29s %-11s %10s  %s%n", "Code", "Item", "Category", "Price", "Rental unit");
        for (RentalItem item : centre.getCatalogue())
            System.out.printf("%-5s %-29s %-11s %10s  %s%n", item.getCode(), item.getName(),
                    item.getCategory(), Money.rm(item.getPrice()), item.getRentalUnit());
    }
    private RentalTransaction selectTransaction() {
        if (centre.getTransactions().isEmpty()) throw new IllegalArgumentException("Create a transaction first.");
        for (RentalTransaction t : centre.getTransactions())
            System.out.println(t.getId() + " | " + t.getCustomerName() + " | "
                    + (t.isPaid() ? "PAID" : t.isVoided() ? "VOID" : "OPEN"));
        return centre.findTransaction(read("Transaction ID: "));
    }
    private void transactions() throws IOException {
        System.out.println("1. Create  2. Update / add items  3. List transactions");
        int choice = number("Choose: ");
        if (choice == 3) {
            for (RentalTransaction t : centre.getTransactions())
                System.out.println(t.getId() + " | " + t.getCustomerName() + " | "
                        + (t.isPaid() ? "PAID" : t.isVoided() ? "VOID" : "OPEN") + " | subtotal " + Money.rm(t.subtotal()));
            return;
        }
        RentalTransaction t;
        if (choice == 1) {
            String id = read("Booking ID (or WALKIN for equipment/add-ons): ");
            Booking booking = id.equalsIgnoreCase("WALKIN") ? null : centre.findBooking(id);
            String name = booking == null ? read("Customer name: ") : booking.getCustomerName();
            t = centre.createTransaction(name, booking);
            if (booking != null) {
                for (RentalItem item : centre.getCatalogue())
                    if (item instanceof FacilityPackage && ((FacilityPackage) item).getFacilityType().equals(booking.getFacility().getType()))
                        t.addItem(item, 1);
            }
            save();
            System.out.println("Created " + t.getId() + ". Matching court charge added for bookings.");
        } else if (choice == 2) t = selectTransaction();
        else throw new IllegalArgumentException("Choose 1 to 3.");
        if (t.isPaid() || t.isVoided()) throw new IllegalArgumentException("Only open transactions can be updated.");
        catalogue();
        while (true) {
            String code = read("Item code (DONE to finish): ");
            if (code.equalsIgnoreCase("DONE")) break;
            try {
                RentalItem item = centre.findItem(code);
                t.addItem(item, number("Quantity: "));
                save();
                System.out.println("Item saved. Subtotal: " + Money.rm(t.subtotal()));
            } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        }
        printBill(t);
    }
    private void printBill(RentalTransaction t) {
        System.out.println("\n===== BILL " + t.getId() + " | " + t.getCustomerName() + " =====");
        if (t.isVoided()) { System.out.println("VOID - booking cancelled; payment unavailable."); return; }
        if (t.getBooking() != null) System.out.println(t.getBooking());
        for (RentalLine line : t.getLines()) System.out.println(line.getItem().getName() + " x "
                + line.getQuantity() + " @ " + Money.rm(line.getItem().getPrice()) + " = " + Money.rm(line.subtotal()));
        Bill bill = new Bill(t);
        System.out.println("Subtotal: " + Money.rm(bill.getSubtotal()));
        System.out.println("Eligible promotions:");
        if (bill.getEligible().isEmpty()) System.out.println("  None");
        for (Promotion p : bill.getEligible()) System.out.println("  " + p.getCode() + " - " + p.getName() + ": " + Money.rm(p.saving(t)));
        System.out.println("Selected promotion: " + (bill.getSelected() == null ? "None" : bill.getSelected().getCode() + " - " + bill.getSelected().getName()));
        System.out.println("Discount: " + Money.rm(bill.getDiscount()));
        System.out.println("10% service charge (after discount): " + Money.rm(bill.getServiceCharge()));
        System.out.println("Final payable: " + Money.rm(bill.getTotal()));
        if (t.isPaid()) System.out.println("PAID " + t.getPaidDate() + " via " + t.getPaymentMethod() + ": " + Money.rm(t.getPaidAmount()));
    }
    private void payment() throws IOException {
        RentalTransaction t = selectTransaction();
        if (t.isPaid()) throw new IllegalArgumentException("Already paid.");
        if (t.isVoided() || t.getLines().isEmpty()) throw new IllegalArgumentException("No payable items.");
        printBill(t);
        System.out.println("1. Cash  2. Credit/debit card  3. E-wallet");
        PaymentMethod method;
        switch (number("Payment method: ")) {
            case 1:
                BigDecimal cash;
                try { cash = Money.of(read("Cash tendered (RM): ")); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("Enter a valid cash amount."); }
                method = new CashPayment(cash); break;
            case 2:
                if (!read("Has the card payment succeeded? Type YES: ").equalsIgnoreCase("YES")) return;
                method = new CardPayment(); break;
            case 3:
                if (!read("Has the e-wallet payment succeeded? Type YES: ").equalsIgnoreCase("YES")) return;
                method = new EWalletPayment(); break;
            default: throw new IllegalArgumentException("Choose 1 to 3.");
        }
        String receipt = centre.pay(t, method, LocalDate.now());
        save();
        System.out.println(receipt);
        System.out.println("Payment saved for " + t.getId());
    }
}
