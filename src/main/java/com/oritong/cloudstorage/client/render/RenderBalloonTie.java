package com.oritong.cloudstorage.client.render;

import com.oritong.cloudstorage.entity.BalloonTieEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.LeashKnotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;

public class RenderBalloonTie extends EntityRenderer<BalloonTieEntity> {
    private static final ResourceLocation KNOT_LOCATION = ResourceLocation.parse("cloudstorage_reborn:textures/entity/balloon/tie.png");
    private final LeashKnotModel<LeashFenceKnotEntity> model;

    public RenderBalloonTie(EntityRendererProvider.Context p_174284_) {
        super(p_174284_);
        this.model = new LeashKnotModel<>(p_174284_.bakeLayer(ModelLayers.LEASH_KNOT));
    }

    public void render(BalloonTieEntity entity, float f1, float f2, PoseStack pose, MultiBufferSource source, int light) {
        pose.pushPose();
        pose.scale(-0.9F, -0.9F, 0.9F);
        this.model.setupAnim(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        VertexConsumer vertexconsumer = source.getBuffer(this.model.renderType(KNOT_LOCATION));
        this.model.renderToBuffer(pose, vertexconsumer, light, OverlayTexture.NO_OVERLAY, net.minecraft.util.FastColor.ARGB32.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F));
        pose.popPose();
        super.render(entity, f1, f2, pose, source, light);
    }

    public ResourceLocation getTextureLocation(BalloonTieEntity entity) {
        return KNOT_LOCATION;
    }
}