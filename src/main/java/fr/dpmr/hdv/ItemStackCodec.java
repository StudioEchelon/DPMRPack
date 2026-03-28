package fr.dpmr.hdv;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class ItemStackCodec {

    private ItemStackCodec() {
    }

    public static String encode(ItemStack item) {
        if (item == null) {
            return null;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos)) {
                oos.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    public static ItemStack decode(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            try (BukkitObjectInputStream ois = new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
                Object o = ois.readObject();
                return (o instanceof ItemStack is) ? is : null;
            }
        } catch (IOException | ClassNotFoundException | IllegalArgumentException e) {
            return null;
        }
    }
}

