package fr.dpmr.hdv;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public record HdvListing(
        String id,
        UUID seller,
        int price,
        long createdAtMs,
        ItemStack item
) {
}

