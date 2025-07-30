package gg.nya.imagehosting.models;

public enum HostingMode {
    /**
     * Hosting links will be generated with a random UUID, e.g., 123e4567-e89b-12d3-a456-426614174000.png
     */
    UUID,
    /**
     * Hosting links will be generated with a random alphanumeric string, e.g., a3f0bk3s.png
     */
    ALPHANUMERIC,
    /**
     * Hosting links will be generated with a timestamp, e.g., 2021-01-01_12-00-00.png
     */
    TIMESTAMPED
}
