package activehub;

public final class AddOn extends RentalItem {
    public AddOn(String code, String name, Category category, String price) {
        super(code, name, category, price);
        if (category != Category.ACCESSORY && category != Category.SERVICE)
            throw new IllegalArgumentException("An add-on must be an accessory or service.");
    }
    @Override public String getRentalUnit() { return "per unit / visit"; }
}
