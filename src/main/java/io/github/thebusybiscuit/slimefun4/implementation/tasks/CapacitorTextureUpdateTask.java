package io.github.thebusybiscuit.slimefun4.implementation.tasks;

import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;

import io.github.bakedlibs.dough.skins.PlayerHead;
import io.github.bakedlibs.dough.skins.PlayerSkin;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.Capacitor;
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture;
import io.papermc.lib.PaperLib;

/**
 * This task is run whenever a {@link Capacitor} needs to update their texture.
 * <strong>This must be executed on the main {@link Server} {@link Thread}!</strong>
 * 
 * @author TheBusyBiscuit
 *
 */
public class CapacitorTextureUpdateTask implements Runnable {

    /**
     * Tracks whether skin setting is available on this platform.
     * Set to false if an UnsupportedOperationException is caught.
     */
    private static volatile boolean skinSettingAvailable = true;

    /**
     * Tracks whether we've already logged a warning about skin setting being unavailable.
     */
    private static volatile boolean warningLogged = false;

    /**
     * The {@link Location} of the {@link Capacitor}.
     */
    private final Location l;

    /**
     * The level of how "full" this {@link Capacitor} is.
     * From 0.0 to 1.0.
     */
    private final double filledPercentage;

    /**
     * This creates a new {@link CapacitorTextureUpdateTask} with the given parameters.
     * 
     * @param l
     *            The {@link Location} of the {@link Capacitor}
     * @param charge
     *            The amount of charge in this {@link Capacitor}
     * @param capacity
     *            The capacity of this {@link Capacitor}
     */
    public CapacitorTextureUpdateTask(@Nonnull Location l, double charge, double capacity) {
        Validate.notNull(l, "The Location cannot be null");

        this.l = l;
        this.filledPercentage = charge / capacity;
    }

    @Override
    public void run() {
        // Skip texture updates if skin setting is not available on this platform
        if (!skinSettingAvailable) {
            return;
        }

        Block b = l.getBlock();
        Material type = b.getType();

        // Ensure that this Block is still a Player Head
        if (type == Material.PLAYER_HEAD || type == Material.PLAYER_WALL_HEAD) {
            if (filledPercentage <= 0.25) {
                // 0-25% capacity
                setTexture(b, HeadTexture.CAPACITOR_25);
            } else if (filledPercentage <= 0.5) {
                // 25-50% capacity
                setTexture(b, HeadTexture.CAPACITOR_50);
            } else if (filledPercentage <= 0.75) {
                // 50-75% capacity
                setTexture(b, HeadTexture.CAPACITOR_75);
            } else {
                // 75-100% capacity
                setTexture(b, HeadTexture.CAPACITOR_100);
            }
        }
    }

    private void setTexture(@Nonnull Block b, @Nonnull HeadTexture texture) {
        try {
            PlayerSkin skin = PlayerSkin.fromHashCode(texture.getUniqueId(), texture.getTexture());
            PlayerHead.setSkin(b, skin, false);

            PaperLib.getBlockState(b, false).getState().update(true, false);
        } catch (UnsupportedOperationException e) {
            // No compatible skin adapter on this platform/version
            // Disable future texture updates to avoid repeated exceptions
            skinSettingAvailable = false;

            // Log a one-time warning to inform server admins
            if (!warningLogged) {
                warningLogged = true;
                try {
                    Slimefun.logger().log(Level.WARNING, "Capacitor texture updates are disabled: No compatible skin adapter found for this server version. Capacitor functionality is unaffected, but textures will not update.");
                } catch (IllegalStateException ignored) {
                    // Slimefun instance not available, skip logging
                }
            }
        }
    }

}
