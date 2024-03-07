package com.floweytf.tabscroll.scrollabletab.mixin;

import com.floweytf.tabscroll.scrollabletab.Config;
import com.floweytf.tabscroll.scrollabletab.IPlayerTabOverlay;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin implements IPlayerTabOverlay {
    @Shadow protected abstract List<PlayerInfo> getPlayerInfos();

    @Shadow @Final private Minecraft minecraft;

    @Shadow public abstract Component getNameForDisplay(PlayerInfo playerInfo);

    @Shadow private @Nullable Component header;

    @Shadow private @Nullable Component footer;

    @Shadow protected abstract void renderTablistScore(Objective objective, int i, String string, int j, int k, UUID uUID, PoseStack poseStack);

    @Shadow protected abstract void renderPingIcon(PoseStack poseStack, int i, int j, int k, PlayerInfo playerInfo);

    @Shadow @Final private Map<UUID, Object> healthStates;

    @Shadow private boolean visible;
    @Unique
    private int scrollable_tab$start = 0;

    @Unique
    private void scrollable_tab$normalizeStart() {
        scrollable_tab$start = Mth.clamp(scrollable_tab$start, 0, Math.max(0, getPlayerInfos().size() - Config.getInstance().maxRows));
    }

    /**
     * @author Flowey
     * @reason I can't be bothered to write a well targeted mixin, sorry!
     */
    @Overwrite
    public void render(PoseStack poseStack, int screenWidth, Scoreboard scoreboard, @Nullable Objective objective) {
        final var infos = this.getPlayerInfos();
        final int players = infos.size();
        final var scrollbarWidth = Config.getInstance().scrollbarWidth;

        scrollable_tab$normalizeStart();
        var currentTabSlice = infos.subList(scrollable_tab$start, Math.min(players, scrollable_tab$start + Config.getInstance().maxRows));

        int maxNameWidth = 0;
        int maxScoreWidth = 0;
        for (PlayerInfo info : infos) {
            maxNameWidth = Math.max(
                maxNameWidth,
                this.minecraft.font.width(this.getNameForDisplay(info))
            );

            if (objective == null || objective.getRenderType() == ObjectiveCriteria.RenderType.HEARTS)
                continue;

            maxScoreWidth = Math.max(
                maxScoreWidth,
                this.minecraft.font.width(" " + scoreboard.getOrCreatePlayerScore(info.getProfile().getName(), objective).getScore())
            );
        }

        if (!this.healthStates.isEmpty()) {
            var set = infos.stream().map(playerInfo -> playerInfo.getProfile().getId()).collect(Collectors.toSet());
            this.healthStates.keySet().removeIf(uuid -> !set.contains(uuid));
        }

        var showFace = this.minecraft.isLocalServer() || this.minecraft.getConnection().getConnection().isEncrypted();
        int objectiveWidth = objective != null ? (objective.getRenderType() == ObjectiveCriteria.RenderType.HEARTS ? 90 : maxScoreWidth) : 0;
        int entryWidth = Math.min(((showFace ? 9 : 0) + maxNameWidth + objectiveWidth + 13), screenWidth - 50);
        int height = 10;

        boolean hasScrollbar = false;
        if(currentTabSlice.size() < players) {
            entryWidth += scrollbarWidth; // add sufficient offset
            hasScrollbar = true;
        }

        int width = entryWidth;

        List<FormattedCharSequence> headerParts = null;
        if (this.header != null) {
            headerParts = this.minecraft.font.split(this.header, screenWidth - 50);
            for (FormattedCharSequence formattedCharSequence : headerParts) {
                width = Math.max(width, this.minecraft.font.width(formattedCharSequence));
            }
        }

        List<FormattedCharSequence> footerParts = null;
        if (this.footer != null) {
            footerParts = this.minecraft.font.split(this.footer, screenWidth - 50);
            for (FormattedCharSequence part : footerParts) {
                width = Math.max(width, this.minecraft.font.width(part));
            }
        }

        final int bgStartX = screenWidth / 2 - width / 2 - 1;
        final int bgEndX = screenWidth / 2 + width / 2 + 1;

        if (headerParts != null) {
            PlayerTabOverlay.fill(
                poseStack,
                bgStartX, height - 1,
                bgEndX, height + headerParts.size() * this.minecraft.font.lineHeight,
                Integer.MIN_VALUE
            );
            for (FormattedCharSequence part : headerParts) {
                int t = this.minecraft.font.width(part);
                this.minecraft.font.drawShadow(poseStack, part, (float) (screenWidth / 2 - t / 2), (float) height, -1);
                height += this.minecraft.font.lineHeight;
            }
            ++height;
        }

        // okay, if we have scroll, we must implement scroll bar
        if(hasScrollbar) {
            final int length = currentTabSlice.size() * 9 - 1;
            final int end = height + length;

            PlayerTabOverlay.fill(
                poseStack,
                bgStartX + 1, height,
                bgStartX + scrollbarWidth, end,
                0xff000000 | Config.getInstance().scrollbarBg
            );

            final int barLength = (currentTabSlice.size() * length + players / 2) / players;
            final int barStart = Mth.clamp((scrollable_tab$start * length + players / 2) / players, 0, length);
            final int barEnd = Mth.clamp(barStart + barLength, 0, length);

            PlayerTabOverlay.fill(
                poseStack,
                bgStartX + 1, barStart + height,
                bgStartX + scrollbarWidth, barEnd + height,
                0xff000000 | Config.getInstance().scrollbarFg
            );
        }

        PlayerTabOverlay.fill(
            poseStack,
            bgStartX, height - 1,
            bgEndX, height + currentTabSlice.size() * 9,
            Integer.MIN_VALUE
        );

        int bg = this.minecraft.options.getBackgroundColor(0x20FFFFFF);

        for (int i = 0; i < currentTabSlice.size(); i++) {
            int startX = screenWidth / 2 - entryWidth / 2 + (hasScrollbar ? scrollbarWidth : 0);
            final int startY = height + i * 9;
            int endX = screenWidth / 2 + entryWidth / 2;

            PlayerTabOverlay.fill(
                poseStack,
                startX, startY,
                endX, startY + 8,
                bg
            );

            RenderSystem.enableBlend();

            PlayerInfo info = currentTabSlice.get(i);
            GameProfile gameProfile = info.getProfile();

            if (showFace) {
                Player player = this.minecraft.level.getPlayerByUUID(gameProfile.getId());
                boolean isUpsideDown = player != null && LivingEntityRenderer.isEntityUpsideDown(player);
                boolean hasHat = player != null && player.isModelPartShown(PlayerModelPart.HAT);
                RenderSystem.setShaderTexture(0, info.getSkinLocation());
                PlayerFaceRenderer.draw(poseStack, startX, startY, 8, hasHat, isUpsideDown);
                startX += 9;
            }

            this.minecraft.font.drawShadow(
                poseStack,
                this.getNameForDisplay(info),
                (float) startX, (float) startY,
                info.getGameMode() == GameType.SPECTATOR ? -1862270977 : -1
            );

            int nameEnd;
            int objectiveEnd;

            if (objective != null && info.getGameMode() != GameType.SPECTATOR && (objectiveEnd = (nameEnd = startX + maxNameWidth + 1) + objectiveWidth) - nameEnd > 5) {
                this.renderTablistScore(objective, startY, gameProfile.getName(), nameEnd, objectiveEnd, gameProfile.getId(), poseStack);
            }

            this.renderPingIcon(poseStack, endX + 9, -(showFace ? 9 : 0), startY, info);
        }

        if (footerParts != null) {
            PlayerTabOverlay.fill(
                poseStack,
                bgStartX, (height += currentTabSlice.size() * 9 + 1) - 1,
                bgEndX, height + footerParts.size() * this.minecraft.font.lineHeight,
                Integer.MIN_VALUE
            );

            for (FormattedCharSequence part : footerParts) {
                int w = this.minecraft.font.width(part);
                this.minecraft.font.drawShadow(poseStack, part, (float) (screenWidth / 2 - w / 2), (float) height, -1);
                height += this.minecraft.font.lineHeight;
            }
        }
    }

    public void scrollable_tab$handle(int amount) {
        scrollable_tab$start += amount;
        scrollable_tab$normalizeStart();
    }

    public boolean scrollable_tab$isVisible() {
        return visible;
    }
}
