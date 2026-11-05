package activehub;

public final class Facility {
    private final String code;
    private final String type;
    private final String name;
    public Facility(String code, String type, String name) {
        this.code = code;
        this.type = type;
        this.name = name;
    }
    public String getCode() { return code; }
    public String getType() { return type; }
    public String getName() { return name; }
    @Override public String toString() { return code + " - " + name; }
}
