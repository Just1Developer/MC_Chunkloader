package net.justonedev.mc.chunkloader;

import org.bukkit.Location;

import java.util.Objects;

public class Chunkloader {

    private final Location location;
    private boolean active;

    public Chunkloader(Location location) {
        this.location = location;
    }

    public Chunkloader(Location location, boolean active) {
        this(location);
        this.active = active;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Chunkloader that = (Chunkloader) o;
        return Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(location);
    }

    @Override
    public String toString() {
        return "Chunkloader={%s, Active=%b}".formatted(location, active);
    }
}
