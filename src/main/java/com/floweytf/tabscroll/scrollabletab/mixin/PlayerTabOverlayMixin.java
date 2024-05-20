package com.floweytf.tabscroll.scrollabletab.mixin;

import com.floweytf.tabscroll.scrollabletab.Config;
import com.floweytf.tabscroll.scrollabletab.IPlayerTabOverlay;
import com.floweytf.tabscroll.scrollabletab.compat.betterping.BetterPingCompatHandler;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin implements IPlayerTabOverlay {
    @Shadow
    protected abstract List<PlayerInfo> getPlayerInfos();

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public abstract Component getNameForDisplay(PlayerInfo playerInfo);

    @Shadow
    private @Nullable Component header;

    @Shadow
    private @Nullable Component footer;

    @Shadow
    private boolean visible;
    @Shadow
    @Final
    private Map<UUID, PlayerTabOverlay.HealthState> healthStates;

    @Shadow
    protected abstract void renderTablistScore(Objective objective, int i, PlayerTabOverlay.ScoreDisplayEntry scoreDisplayEntry, int j, int k, UUID uUID, GuiGraphics guiGraphics);

    @Shadow
    protected abstract void renderPingIcon(GuiGraphics guiGraphics, int i, int j, int k, PlayerInfo playerInfo);

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
    @Inject(
        method = "render",
        at = @At("HEAD"),
        cancellable = true
    )
    private void scrollable_tab$injectRender(GuiGraphics guiGraphics, int i, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
        scrollable_tab$doRender(guiGraphics, i, scoreboard, objective);
        ci.cancel();
    }

    @Unique
    private void scrollable_tab$doRender(GuiGraphics guiGraphics, int screenWidth, Scoreboard scoreboard, @Nullable Objective objective) {
        final var infos = this.getPlayerInfos();
        final int players = infos.size();
        final var scrollbarWidth = Config.getInstance().scrollbarWidth;
        final var spaceWidth = this.minecraft.font.width(" ");

        scrollable_tab$normalizeStart();
        var currentTabSlice = infos.subList(scrollable_tab$start, Math.min(players, scrollable_tab$start + Config.getInstance().maxRows));

        int maxNameWidth = 0;
        int maxScoreWidth = 0;

        final var scoreDisplayEntries = new ArrayList<PlayerTabOverlay.ScoreDisplayEntry>();

        for (final var info : infos) {
            final var displayName = this.getNameForDisplay(info);
            maxNameWidth = Math.max(
                maxNameWidth,
                this.minecraft.font.width(displayName)
            );

            if (objective == null || objective.getRenderType() == ObjectiveCriteria.RenderType.HEARTS)
                continue;

            final var scoreHolder = ScoreHolder.fromGameProfile(info.getProfile());
            final var scoreInfo = scoreboard.getPlayerScoreInfo(scoreHolder, objective);
            final var numberFormat = objective.numberFormatOrDefault(StyledFormat.PLAYER_LIST_DEFAULT);
            final var component = ReadOnlyScoreInfo.safeFormatValue(scoreInfo, numberFormat);
            final var width = this.minecraft.font.width(component);

            maxScoreWidth = Math.max(
                maxScoreWidth,
                width > 0 ? spaceWidth + this.minecraft.font.width(component) : 0
            );

            scoreDisplayEntries.add(new PlayerTabOverlay.ScoreDisplayEntry(displayName, scoreInfo == null ? 0 : scoreInfo.value(), component, width));
        }

        if (!this.healthStates.isEmpty()) {
            var set = infos.stream().map(playerInfo -> playerInfo.getProfile().getId()).collect(Collectors.toSet());
            this.healthStates.keySet().removeIf(uuid -> !set.contains(uuid));
        }

        final var showFace = this.minecraft.isLocalServer() || this.minecraft.getConnection().getConnection().isEncrypted();
        final var objectiveWidth = objective != null ? (objective.getRenderType() == ObjectiveCriteria.RenderType.HEARTS ? 90 : maxScoreWidth) : 0;
        var entryWidth = Math.min(((showFace ? 9 : 0) + maxNameWidth + objectiveWidth + 13), screenWidth - 50);
        var height = 10;

        var hasScrollbar = false;
        if (currentTabSlice.size() < players) {
            entryWidth += scrollbarWidth; // add sufficient offset
            hasScrollbar = true;
        }

        if (BetterPingCompatHandler.callback != null) {
            entryWidth += 45;
        }

        var width = entryWidth;

        List<FormattedCharSequence> headerParts = null;
        if (this.header != null) {
            headerParts = this.minecraft.font.split(this.header, screenWidth - 50);
            for (final var formattedCharSequence : headerParts) {
                width = Math.max(width, this.minecraft.font.width(formattedCharSequence));
            }
        }

        List<FormattedCharSequence> footerParts = null;
        if (this.footer != null) {
            footerParts = this.minecraft.font.split(this.footer, screenWidth - 50);
            for (final var part : footerParts) {
                width = Math.max(width, this.minecraft.font.width(part));
            }
        }

        final int bgStartX = screenWidth / 2 - width / 2 - 1;
        final int bgEndX = screenWidth / 2 + width / 2 + 1;

        if (headerParts != null) {
            guiGraphics.fill(
                bgStartX, height - 1,
                bgEndX, height + headerParts.size() * this.minecraft.font.lineHeight,
                Integer.MIN_VALUE
            );
            for (FormattedCharSequence part : headerParts) {
                int t = this.minecraft.font.width(part);
                guiGraphics.drawString(minecraft.font, part, screenWidth / 2 - t / 2, height, -1);
                height += this.minecraft.font.lineHeight;
            }
            ++height;
        }

        // okay, if we have scroll, we must implement scroll bar
        if (hasScrollbar) {
            final int length = currentTabSlice.size() * 9 - 1;
            final int end = height + length;

            guiGraphics.fill(
                bgStartX + 1, height,
                bgStartX + scrollbarWidth, end,
                0xff000000 | Config.getInstance().scrollbarBg
            );

            final int barLength = (currentTabSlice.size() * length + players / 2) / players;
            final int barStart = Mth.clamp((scrollable_tab$start * length + players / 2) / players, 0, length);
            final int barEnd = Mth.clamp(barStart + barLength, 0, length);

            guiGraphics.fill(
                bgStartX + 1, barStart + height,
                bgStartX + scrollbarWidth, barEnd + height,
                0xff000000 | Config.getInstance().scrollbarFg
            );
        }

        guiGraphics.fill(
            bgStartX, height - 1,
            bgEndX, height + currentTabSlice.size() * 9,
            Integer.MIN_VALUE
        );

        int bg = this.minecraft.options.getBackgroundColor(0x20FFFFFF);

        for (int i = 0; i < currentTabSlice.size(); i++) {
            int startX = screenWidth / 2 - entryWidth / 2 + (hasScrollbar ? scrollbarWidth : 0);
            final var startY = height + i * 9;
            var endX = screenWidth / 2 + entryWidth / 2;

            guiGraphics.fill(
                startX, startY,
                endX, startY + 8,
                bg
            );

            RenderSystem.enableBlend();

            final var info = currentTabSlice.get(i);
            final var gameProfile = info.getProfile();
            final var scoreDisplayEntry = scoreDisplayEntries.get(i);

            if (showFace) {
                Player player = this.minecraft.level.getPlayerByUUID(gameProfile.getId());
                boolean isUpsideDown = player != null && LivingEntityRenderer.isEntityUpsideDown(player);
                boolean hasHat = player != null && player.isModelPartShown(PlayerModelPart.HAT);
                PlayerFaceRenderer.draw(guiGraphics, info.getSkin().texture(), startX, startY, 8, hasHat, isUpsideDown);
                startX += 9;
            }

            guiGraphics.drawString(
                this.minecraft.font,
                scoreDisplayEntry.name(),
                startX, startY,
                info.getGameMode() == GameType.SPECTATOR ? 0x90ffffff : -1
            );

            if (objective != null && info.getGameMode() != GameType.SPECTATOR && objectiveWidth > 5) {
                this.renderTablistScore(
                    objective,
                    startY,
                    scoreDisplayEntry,
                    startX + maxNameWidth + 1, startX + maxNameWidth + 1 + objectiveWidth,
                    gameProfile.getId(),
                    guiGraphics
                );
            }

            if (BetterPingCompatHandler.callback != null) {
                BetterPingCompatHandler.callback.render(
                    minecraft,
                    (PlayerTabOverlay) (Object) this,
                    guiGraphics,
                    endX + 10, -(showFace ? 9 : 0), startY,
                    info
                );
            } else {
                this.renderPingIcon(guiGraphics, endX + 9, -(showFace ? 9 : 0), startY, info);
            }
        }

        if (footerParts != null) {
            guiGraphics.fill(
                bgStartX, (height += currentTabSlice.size() * 9 + 1) - 1,
                bgEndX, height + footerParts.size() * this.minecraft.font.lineHeight,
                Integer.MIN_VALUE
            );

            for (FormattedCharSequence part : footerParts) {
                int w = this.minecraft.font.width(part);
                guiGraphics.drawString(minecraft.font, part, screenWidth / 2 - w / 2, height, -1);
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
