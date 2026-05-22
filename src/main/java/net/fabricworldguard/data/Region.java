package net.fabricworldguard.data;

import net.minecraft.util.math.BlockBox;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Region {
    private final String name;
    private final UUID owner;
    private final Set<UUID> owners; // Совладельцы
    private final Set<UUID> members; // Участники
    private final BlockBox box;
    private final String worldId;
    private final RegionFlags flags = new RegionFlags();

    private final long createdAt;

    public Region(String name, UUID owner, BlockBox box, String worldId) {
        this.name = name;
        this.owner = owner;
        this.box = box;
        this.worldId = worldId;
        this.owners = new HashSet<>();
        this.members = new HashSet<>();
        this.createdAt = System.currentTimeMillis();
    }

    // --- Геттеры для данных ---
    public String getName() { return name; }
    public UUID getOwner() { return owner; }
    public Set<UUID> getOwners() { return owners; }
    public Set<UUID> getMembers() { return members; }
    public BlockBox getBox() { return box; }
    public RegionFlags getFlags() { return this.flags; }
    public String getWorldId() { return this.worldId; }
    public long getCreatedAt() { return createdAt; }

    // --- Логика проверки прав ---

    // Проверка, является ли игрок "своим" (владелец, совладелец или участник)
    public boolean isAllowed(UUID player) {
        return player.equals(owner) || owners.contains(player) || members.contains(player);
    }

    // Проверка, является ли игрок владельцем или совладельцем (для команд addmember)
    public boolean isOwnerOrCoOwner(UUID player) {
        return player.equals(owner) || owners.contains(player);
    }

    // Удобный метод для проверки, является ли игрок владельцем
    public boolean isOwner(UUID player) {
        return player.equals(owner);
    }
}