package com.floweytf.tabscroll.scrollabletab;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModMenuImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        try {
            Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
        } catch (ClassNotFoundException e) {
            return parent -> null;
        }
        return ConfigScreen::new;
    }

    private static class ConfigScreen extends Screen {
        private final Screen parent;

        protected ConfigScreen(Screen parent) {
            super(Component.translatable("scrollable-tab.config.title"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            super.init();
            ConfigBuilder config = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("scrollable-tab.config.title"))
                .setSavingRunnable(() -> Config.getInstance().save());

            config.getOrCreateCategory(Component.translatable("scrollable-tab.config.category.main"))
                .addEntry(
                    config.entryBuilder()
                        .startColorField(Component.translatable("scrollable-tab.config.scrollbarbg"), Config.getInstance().scrollbarBg)
                        .setDefaultValue(0xffffff)
                        .setTooltip(
                            Component.translatable("scrollable-tab.config.scrollbarbg.tooltip")
                        )
                        .setSaveConsumer((v) -> Config.getInstance().scrollbarBg = v)
                        .build()
                ).addEntry(
                    config.entryBuilder()
                        .startColorField(Component.translatable("scrollable-tab.config.scrollbarfg"), Config.getInstance().scrollbarFg)
                        .setDefaultValue(0xaaaaaa)
                        .setTooltip(
                            Component.translatable("scrollable-tab.config.scrollbarfg.tooltip")
                        )
                        .setSaveConsumer((v) -> Config.getInstance().scrollbarFg = v)
                        .build()
                ).addEntry(
                    config.entryBuilder()
                        .startIntField(Component.translatable("scrollable-tab.config.maxrows"), Config.getInstance().maxRows)
                        .setDefaultValue(20)
                        .setMin(5)
                        .setMax(100)
                        .setTooltip(
                            Component.translatable("scrollable-tab.config.maxrows.tooltip")
                        )
                        .setSaveConsumer((v) -> Config.getInstance().maxRows = v)
                        .build()
                ).addEntry(
                    config.entryBuilder()
                        .startIntField(Component.translatable("scrollable-tab.config.scrollbarwidth"), Config.getInstance().scrollbarWidth)
                        .setDefaultValue(3)
                        .setMin(2)
                        .setMax(10)
                        .setTooltip(
                            Component.translatable("scrollable-tab.config.scrollbarwidth.tooltip")
                        )
                        .setSaveConsumer((v) -> Config.getInstance().scrollbarWidth = v)
                        .build()
                ).addEntry(
                    config.entryBuilder()
                        .startIntField(Component.translatable("scrollable-tab.config.scrollbarincr"), Config.getInstance().scrollbarIncrement)
                        .setDefaultValue(3)
                        .setMin(1)
                        .setMax(10)
                        .setTooltip(
                            Component.translatable("scrollable-tab.config.scrollbarincr.tooltip")
                        )
                        .setSaveConsumer((v) -> Config.getInstance().scrollbarIncrement = v)
                        .build()
                );

            if (minecraft != null) {
                minecraft.forceSetScreen(config.build());
            }
        }

        @Override
        public void onClose() {
            if (minecraft != null) {
                minecraft.forceSetScreen(parent);
            }
        }
    }
}
