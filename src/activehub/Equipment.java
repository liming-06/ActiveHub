package activehub;

public final class Equipment extends RentalItem {
    public Equipment(String code, String name, String price) {
        super(code, name, Category.EQUIPMENT, price);
    }
    @Override public String getRentalUnit() { return "per unit / visit"; }
}
