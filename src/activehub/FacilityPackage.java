package activehub;

public final class FacilityPackage extends RentalItem {
    private final String facilityType;
    public FacilityPackage(String code, String name, String price, String facilityType) {
        super(code, name, Category.FACILITY, price);
        this.facilityType = facilityType;
    }
    public String getFacilityType() { return facilityType; }
    @Override public String getRentalUnit() { return "one booked court / hour"; }
}
