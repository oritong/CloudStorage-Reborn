package com.github.alexthe668.cloudstorage.client.render;

import com.github.alexthe668.cloudstorage.entity.BalloonFace;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class BalloonTextures {
    public static final ResourceLocation STRING_TIE = ResourceLocation.parse("cloudstorage_reborn:textures/entity/balloon/balloon_string.png");

    public static final ResourceLocation BALLOON = ResourceLocation.parse("cloudstorage_reborn:textures/entity/balloon/balloon.png");
    public static final ResourceLocation BALLOON_SHEEN = ResourceLocation.parse("cloudstorage_reborn:textures/entity/balloon/balloon_sheen.png");
    public static final ResourceLocation POPPED = ResourceLocation.parse("cloudstorage_reborn:textures/entity/balloon/balloon_popped.png");
    public static final ResourceLocation LIGHTNING = ResourceLocation.parse("cloudstorage_reborn:textures/entity/balloon/balloon_lightning.png");
    public static final ResourceLocation LOOT = ResourceLocation.parse("cloudstorage_reborn:textures/entity/balloon/loot_balloon.png");
    private static final Map<BalloonFace, ResourceLocation> FACE_TEXTURES = new HashMap<>();

    public static ResourceLocation getTextureForFace(BalloonFace face) {
        if (FACE_TEXTURES.containsKey(face)) {
            return FACE_TEXTURES.get(face);
        } else {
            ResourceLocation res = ResourceLocation.parse("cloudstorage_reborn:textures/entity/balloon/balloon_face_" + face.getName() + ".png");
            FACE_TEXTURES.put(face, res);
            return res;
        }
    }
}
