package hr.brajnovic.td.map;

public enum TileType {
    BUILDABLE,
    PATH,
    BLOCKED;

    public static TileType fromTiledProperty(String value) {
        switch (value) {
            case "buildable":
                return BUILDABLE;
            case "path":
                return PATH;
            case "blocked":
                return BLOCKED;
            default:
                throw new IllegalArgumentException("Unknown tile type: " + value);
        }
    }
}
