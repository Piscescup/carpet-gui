package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.piscescup.fabricmc.carpetgui.References;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static io.github.piscescup.fabricmc.carpetgui.References.LOGGER;

/**
 * Loads original installed-mod icons, with per-screen ownership and explicit GPU cleanup.
 */
final class ModIconStore
    implements AutoCloseable
{
    private static final AtomicLong NEXT_ID = new AtomicLong();
    private final long storeId = NEXT_ID.incrementAndGet();
    private final Map<String, Optional<Identifier>> textures = new LinkedHashMap<>();

    Optional<Identifier> icon(String modId) {
        return textures.computeIfAbsent(modId, this::load);
    }

    private Optional<Identifier> load(String modId) {
        var mod = FabricLoader.getInstance()
            .getModContainer(modId);
        if (mod.isEmpty()) return Optional.empty();
        Optional<Path> path = mod.get()
            .getMetadata()
            .getIconPath(64)
            .flatMap(mod.get()::findPath);
        if (path.isEmpty()) return Optional.empty();
        try (InputStream input = Files.newInputStream(path.get())) {
            NativeImage image = NativeImage.read(input);
            if (image.getWidth() > 2048 || image.getHeight() > 2048) {
                image.close();
                return Optional.empty();
            }
            Identifier id = References.fromPath("mod_icons/" + storeId + "/" + modId);
            DynamicTexture texture;
            try {
                texture = new DynamicTexture(() -> "Carpet GUI icon: " + modId, image);
            } catch (RuntimeException error) {
                image.close();
                throw error;
            }
            try {
                Minecraft.getInstance()
                    .getTextureManager()
                    .register(id, texture);
            } catch (RuntimeException error) {
                texture.close();
                throw error;
            }
            return Optional.of(id);
        } catch (Exception exception) {
            LOGGER.warn("Unable to load icon for {}", modId, exception);
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        textures.values()
            .forEach(icon -> icon.ifPresent(id -> Minecraft.getInstance()
                .getTextureManager()
                .release(id)));
        textures.clear();
    }
}
