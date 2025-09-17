package gg.nya.imagehosting.models;

/**
 * Enum representing the different hosting modes for file uploads.
 */
public enum HostingMode {
    /**
     * Hosting links will be generated with a random UUID, e.g., 123e4567-e89b-12d3-a456-426614174000.png
     */
    UUID("Random UUID"),
    /**
     * Hosting links will be generated with a random alphanumeric string, e.g., a3f0bk3s.png
     */
    ALPHANUMERIC("Random Character String"),
    /**
     * Hosting links will be generated with a timestamp, e.g., 2021-01-01_12-00-00.png
     */
    TIMESTAMPED("Timestamp");

   private final String description;

    HostingMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
