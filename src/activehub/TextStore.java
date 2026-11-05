package activehub;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** One versioned UTF-8 text snapshot; fields are URL-escaped, separated by |. */
public final class TextStore {
    private final Path file;
    public TextStore(Path file) { this.file = file.toAbsolutePath(); }
    public Path getFile() { return file; }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private String decode(String value) { return URLDecoder.decode(value, StandardCharsets.UTF_8); }
    private String record(String... fields) {
        List<String> encoded = new ArrayList<>();
        for (String field : fields) encoded.add(encode(field));
        return String.join("|", encoded);
    }
    public void save(SportsCentre centre) throws IOException {
        Files.createDirectories(file.getParent());
        List<String> rows = new ArrayList<>();
        rows.add("ACTIVEHUB|1");
        for (Booking b : centre.getBookings()) rows.add(record("B", b.getId(), b.getCustomerName(), b.getContact(),
                b.getDate().toString(), b.getTime().toString(), Integer.toString(b.getParticipants()),
                b.getFacility().getCode(), Boolean.toString(b.isCancelled())));
        for (RentalTransaction t : centre.getTransactions()) {
            rows.add(record("T", t.getId(), t.getCustomerName(), t.getBooking() == null ? "" : t.getBooking().getId()));
            for (RentalLine line : t.getLines()) rows.add(record("L", t.getId(), line.getItem().getCode(), Integer.toString(line.getQuantity())));
            if (t.isPaid()) rows.add(record("P", t.getId(), t.getPaidDate().toString(), t.getPaymentMethod(), t.getPaidAmount().toPlainString()));
        }
        Path temp = Files.createTempFile(file.getParent(), "activehub-", ".tmp");
        try {
            Files.write(temp, rows, StandardCharsets.UTF_8);
            try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    public SportsCentre load() throws IOException {
        SportsCentre centre = new SportsCentre();
        if (!Files.exists(file)) return centre;
        List<String> rows = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (rows.isEmpty() || !rows.get(0).equals("ACTIVEHUB|1")) throw new IOException("Unknown data-file version.");
        Set<String> ids = new HashSet<>();
        List<Booking> cancelled = new ArrayList<>();
        int lineNumber = 1;
        try {
            for (String row : rows.subList(1, rows.size())) {
                lineNumber++;
                String[] f = row.split("\\|", -1);
                for (int i = 0; i < f.length; i++) f[i] = decode(f[i]);
                switch (f[0]) {
                    case "B":
                        require(f, 9);
                        if (!ids.add(f[1])) throw new IllegalArgumentException("Duplicate ID.");
                        Booking b = new Booking(f[1], f[2], f[3], LocalDate.parse(f[4]), LocalTime.parse(f[5]), Integer.parseInt(f[6]), centre.findFacility(f[7]));
                        if (!f[8].equals("true") && !f[8].equals("false")) throw new IllegalArgumentException("Bad cancellation flag.");
                        // Defer cancellation until lines have been restored.
                        if (Boolean.parseBoolean(f[8])) cancelled.add(b);
                        centre.restoreBooking(b);
                        break;
                    case "T":
                        require(f, 4);
                        if (!ids.add(f[1])) throw new IllegalArgumentException("Duplicate ID.");
                        centre.restoreTransaction(new RentalTransaction(f[1], f[2], f[3].isEmpty() ? null : centre.findBooking(f[3])));
                        break;
                    case "L":
                        require(f, 4);
                        centre.findTransaction(f[1]).addItem(centre.findItem(f[2]), Integer.parseInt(f[3]));
                        break;
                    case "P":
                        require(f, 5);
                        if (!Set.of("CASH", "CARD", "E_WALLET").contains(f[3])) throw new IllegalArgumentException("Bad payment method.");
                        RentalTransaction t = centre.findTransaction(f[1]);
                        if (Money.of(f[4]).compareTo(new Bill(t).getTotal()) != 0) throw new IllegalArgumentException("Payment total mismatch.");
                        t.markPaid(LocalDate.parse(f[2]), f[3], Money.of(f[4]));
                        break;
                    default: throw new IllegalArgumentException("Unknown record type.");
                }
            }
            for (Booking b : cancelled) {
                for (RentalTransaction t : centre.getTransactions())
                    if (t.getBooking() == b && t.isPaid()) throw new IllegalArgumentException("Cancelled booking has payment.");
                b.cancel();
            }
        } catch (RuntimeException e) {
            throw new IOException("Invalid data near line " + lineNumber + ": " + e.getMessage(), e);
        }
        return centre;
    }
    private void require(String[] fields, int count) {
        if (fields.length != count) throw new IllegalArgumentException("Wrong field count.");
    }
}
