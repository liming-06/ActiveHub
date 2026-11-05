package activehub;

import java.time.LocalDate;
import java.time.LocalTime;

public final class Booking {
    private final String id;
    private final String customerName;
    private final String contact;
    private final LocalDate date;
    private final LocalTime time;
    private final int participants;
    private final Facility facility;
    private boolean cancelled;

    public Booking(String id, String customerName, String contact, LocalDate date,
                   LocalTime time, int participants, Facility facility) {
        if (customerName == null || customerName.trim().isEmpty())
            throw new IllegalArgumentException("Customer name is required.");
        if (contact == null || !contact.matches("[+0-9() -]{6,25}"))
            throw new IllegalArgumentException("Enter a contact number of 6 to 25 characters.");
        if (date == null || time == null || facility == null || participants < 1)
            throw new IllegalArgumentException("Date, time, facility and positive participants required.");
        if (time.getSecond() != 0 || time.getNano() != 0 || time.isAfter(LocalTime.of(23, 0)))
            throw new IllegalArgumentException("Use minute precision; last one-hour slot starts at 23:00.");
        this.id = id;
        this.customerName = customerName.trim();
        this.contact = contact.trim();
        this.date = date;
        this.time = time;
        this.participants = participants;
        this.facility = facility;
    }
    public String getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getContact() { return contact; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public int getParticipants() { return participants; }
    public Facility getFacility() { return facility; }
    public boolean isCancelled() { return cancelled; }
    public void cancel() { cancelled = true; }
    public boolean overlaps(LocalDate otherDate, LocalTime otherTime) {
        // Every booking lasts exactly one hour. Endpoints may touch.
        int start = time.toSecondOfDay();
        int otherStart = otherTime.toSecondOfDay();
        return !cancelled && date.equals(otherDate)
                && start < otherStart + 3600 && otherStart < start + 3600;
    }
    @Override public String toString() {
        return id + " | " + customerName + " | " + contact + " | " + date + " " + time
                + " | " + facility.getCode() + " | " + participants + " people | "
                + (cancelled ? "CANCELLED" : "ACTIVE");
    }
}
