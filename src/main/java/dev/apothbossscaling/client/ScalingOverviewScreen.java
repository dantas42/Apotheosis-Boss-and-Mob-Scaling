package dev.apothbossscaling.client;

import dev.apothbossscaling.config.ScalingConfig;
import dev.apothbossscaling.net.ConfigSnapshotPayload;
import dev.apothbossscaling.net.ConfigUpdatePayload;
import dev.apothbossscaling.net.RequestSnapshotPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tela de configuração do scaling, organizada em abas. A aba "Mods" tem uma lista mestre-detalhe:
 * clicar em um mod mostra/edita o multiplicador dele à direita.
 */
@OnlyIn(Dist.CLIENT)
public final class ScalingOverviewScreen extends Screen {

    private static final String[] TABS = { "General", "Tiers", "Boss", "Player", "Mods" };
    private static final int BOX_W = 60;
    private static final int X_BOX1 = 110;
    private static final int X_BOX2 = 180;
    private static final int X_BOX3 = 250;
    private static final int HEADER_COLOR = 0xA0A0A0;
    private static final int LABEL_COLOR = 0xCCCCCC;
    private static final int TEXT_COLOR = 0xFFFFFF;

    private ConfigSnapshotPayload snapshot;
    private int currentTab;
    private String selectedMod;
    private int modScroll;
    private int yModsList;

    private final EditBox[] healthBoxes = new EditBox[ScalingConfig.TIERS.length];
    private final EditBox[] damageBoxes = new EditBox[ScalingConfig.TIERS.length];
    private final EditBox[] armorBoxes = new EditBox[ScalingConfig.TIERS.length];
    private EditBox thresholdBox;
    private EditBox armorCapBox;
    private EditBox excludedBox;
    private EditBox bossBox;
    private EditBox radiusBox;
    private EditBox exponentBox;
    private EditBox minecraftMultBox;
    private EditBox defaultMultBox;
    private EditBox apotheosisMultBox;
    private EditBox selectedModMultBox;

    public ScalingOverviewScreen() {
        super(Component.translatable("screen.apothboss_scaling.overview"));
    }

    @Override
    protected void init() {
        this.snapshot = ClientConfigCache.latest();

        for (int i = 0; i < TABS.length; i++) {
            int x = 10 + i * 78;
            int tab = i;
            Button btn = Button.builder(Component.literal(TABS[i]), b -> switchTab(tab)).bounds(x, 24, 72, 18).build();
            btn.active = i != currentTab;
            addRenderableWidget(btn);
        }

        switch (currentTab) {
            case 0 -> buildGeneral();
            case 1 -> buildTiers();
            case 2 -> buildBoss();
            case 3 -> buildPlayer();
            case 4 -> buildMods();
        }

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
            .bounds(this.width - 60, this.height - 28, 50, 20).build());
    }

    private void switchTab(int tab) {
        this.currentTab = tab;
        this.rebuildWidgets();
    }

    private void buildGeneral() {
        int y = 50;
        addToggle("enabled", "Enabled", 10, y, 100);
        addToggle("scale_common_mobs", "Common Mobs", 120, y, 110);
        addToggle("scale_bosses", "Bosses", 240, y, 90);
        y += 24;
        addToggle("scale_max_health", "Max Health", 10, y, 100);
        addToggle("scale_attack_damage", "Atk Damage", 120, y, 110);
        addToggle("scale_armor", "Armor", 240, y, 90);
        y += 24;
        addToggle("debug_log", "Debug Log", 10, y, 100);
    }

    private void buildTiers() {
        int y = 50;
        for (int i = 0; i < ScalingConfig.TIERS.length; i++) {
            int rowY = y + i * 22;
            healthBoxes[i] = addEditBox(X_BOX1, rowY, BOX_W, trim(mult(snapshot == null ? null : snapshot.maxHealthMultipliers(), i, 1.0)));
            damageBoxes[i] = addEditBox(X_BOX2, rowY, BOX_W, trim(mult(snapshot == null ? null : snapshot.attackDamageMultipliers(), i, 1.0)));
            armorBoxes[i] = addEditBox(X_BOX3, rowY, BOX_W, trim(mult(snapshot == null ? null : snapshot.armorMultipliers(), i, 1.0)));
        }
        addApplyButton(y + ScalingConfig.TIERS.length * 22 + 8, this::applyTiers);
    }

    private void buildBoss() {
        int y = 50;
        thresholdBox = addEditBox(X_BOX1, y, BOX_W, snapshot == null ? "100.0" : trim(snapshot.bossHealthThreshold()));
        armorCapBox = addEditBox(X_BOX3, y, BOX_W, snapshot == null ? "30.0" : trim(snapshot.armorCap()));
        y += 24;
        excludedBox = addEditBox(X_BOX1, y, 240, snapshot == null ? "" : snapshot.excludedEntities());
        y += 24;
        bossBox = addEditBox(X_BOX1, y, 240, snapshot == null ? "" : snapshot.bossEntities());
        addApplyButton(y + 28, this::applyBoss);
    }

    private void buildPlayer() {
        int y = 50;
        addToggle("player_scaling.enabled", "Player Scaling", 10, y, 120);
        addToggle("player_scaling.dynamic", "Dynamic", 140, y, 110);
        y += 24;
        radiusBox = addEditBox(X_BOX1, y, BOX_W, snapshot == null ? "100.0" : trim(snapshot.playerScalingRadius()));
        exponentBox = addEditBox(X_BOX3, y, BOX_W, snapshot == null ? "0.5" : trim(snapshot.playerScalingExponent()));
        addApplyButton(y + 28, this::applyPlayer);
    }

    private void buildMods() {
        int y = 50;
        minecraftMultBox = addEditBox(X_BOX1, y, BOX_W, snapshot == null ? "1.0" : trim(snapshot.modMultMinecraft()));
        defaultMultBox = addEditBox(X_BOX2, y, BOX_W, snapshot == null ? "1.5" : trim(snapshot.modMultDefault()));
        apotheosisMultBox = addEditBox(X_BOX3, y, BOX_W, snapshot == null ? "0.7" : trim(snapshot.modMultApotheosis()));
        addApplyButton(y + 26, this::applyFactors);

        yModsList = y + 60;
        List<String> namespaces = snapshot == null ? List.of() : snapshot.modNamespaces();
        int visible = Math.max(1, (this.height - yModsList - 36) / 22);
        int start = Math.max(0, Math.min(modScroll, Math.max(0, namespaces.size() - visible)));
        for (int i = start; i < Math.min(start + visible, namespaces.size()); i++) {
            String ns = namespaces.get(i);
            int rowY = yModsList + (i - start) * 22;
            Button btn = Button.builder(Component.literal(ns), b -> selectMod(ns)).bounds(10, rowY, 155, 20).build();
            btn.active = !ns.equals(selectedMod);
            addRenderableWidget(btn);
        }

        if (selectedMod != null) {
            int px = 180;
            selectedModMultBox = addEditBox(px, yModsList + 24, BOX_W, trim(currentModFactor(selectedMod)));
            Button apply = Button.builder(Component.literal("Apply"), b -> applySelectedMod()).bounds(px, yModsList + 50, 60, 20).build();
            apply.active = canEdit();
            addRenderableWidget(apply);
            Button reset = Button.builder(Component.literal("Reset"), b -> resetSelectedMod()).bounds(px + 70, yModsList + 50, 60, 20).build();
            reset.active = canEdit();
            addRenderableWidget(reset);
        }
    }

    private void selectMod(String ns) {
        this.selectedMod = ns;
        this.rebuildWidgets();
    }

    private double currentModFactor(String namespace) {
        if (snapshot != null) {
            for (String entry : snapshot.modMultOverrides().split(",")) {
                String e = entry.trim();
                int idx = e.indexOf(':');
                if (idx > 0 && e.substring(0, idx).equals(namespace)) {
                    try {
                        return Double.parseDouble(e.substring(idx + 1).trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return snapshot == null ? 1.0 : snapshot.modMultDefault();
    }

    private void applySelectedMod() {
        if (selectedMod == null || !canEdit()) {
            return;
        }
        double value;
        try {
            value = Double.parseDouble(selectedModMultBox.getValue());
        } catch (NumberFormatException e) {
            return;
        }
        send("mod_scaling.overrides", rebuildOverrides(selectedMod, trim(value)));
    }

    private void resetSelectedMod() {
        if (selectedMod == null || !canEdit()) {
            return;
        }
        send("mod_scaling.overrides", rebuildOverrides(selectedMod, null));
    }

    private String rebuildOverrides(String namespace, String newValue) {
        List<String> entries = new ArrayList<>();
        String current = snapshot == null ? "" : snapshot.modMultOverrides();
        if (!current.isBlank()) {
            for (String e : current.split(",")) {
                String entry = e.trim();
                int idx = entry.indexOf(':');
                if (!entry.isEmpty() && !(idx > 0 && entry.substring(0, idx).equals(namespace))) {
                    entries.add(entry);
                }
            }
        }
        if (newValue != null) {
            entries.add(namespace + ":" + newValue);
        }
        return String.join(",", entries);
    }

    private void addApplyButton(int y, Runnable action) {
        Button btn = Button.builder(Component.literal("Apply"), b -> action.run()).bounds(10, y, 60, 20).build();
        btn.active = canEdit();
        addRenderableWidget(btn);
    }

    private void applyTiers() {
        if (!canEdit()) {
            return;
        }
        for (int i = 0; i < ScalingConfig.TIERS.length; i++) {
            send("max_health." + ScalingConfig.TIERS[i], healthBoxes[i].getValue());
            send("attack_damage." + ScalingConfig.TIERS[i], damageBoxes[i].getValue());
            send("armor." + ScalingConfig.TIERS[i], armorBoxes[i].getValue());
        }
    }

    private void applyBoss() {
        if (!canEdit()) {
            return;
        }
        send("boss_health_threshold", thresholdBox.getValue());
        send("armor_cap", armorCapBox.getValue());
        send("excluded_entities", excludedBox.getValue());
        send("boss_entities", bossBox.getValue());
    }

    private void applyPlayer() {
        if (!canEdit()) {
            return;
        }
        send("player_scaling.radius", radiusBox.getValue());
        send("player_scaling.exponent", exponentBox.getValue());
    }

    private void applyFactors() {
        if (!canEdit()) {
            return;
        }
        send("mod_scaling.minecraft", minecraftMultBox.getValue());
        send("mod_scaling.default", defaultMultBox.getValue());
        send("mod_scaling.apotheosis", apotheosisMultBox.getValue());
    }

    private void addToggle(String field, String label, int x, int y, int width) {
        boolean value = bool(field);
        Button btn = Button.builder(Component.literal(label + ": " + (value ? "ON" : "OFF")), b -> {
            if (canEdit()) {
                PacketDistributor.sendToServer(new ConfigUpdatePayload(field, Boolean.toString(!value)));
            }
        }).bounds(x, y, width, 20).build();
        btn.active = canEdit();
        addRenderableWidget(btn);
    }

    private EditBox addEditBox(int x, int y, int width, String value) {
        EditBox box = new EditBox(this.font, x, y, width, 18, Component.empty());
        box.setMaxLength(256);
        box.setValue(value);
        box.setEditable(canEdit());
        addRenderableWidget(box);
        return box;
    }

    private boolean canEdit() {
        return this.minecraft != null && this.minecraft.player != null && this.minecraft.player.hasPermissions(2);
    }

    private boolean bool(String field) {
        if (snapshot == null) {
            return true;
        }
        return switch (field) {
            case "enabled" -> snapshot.enabled();
            case "scale_common_mobs" -> snapshot.scaleCommonMobs();
            case "scale_bosses" -> snapshot.scaleBosses();
            case "scale_max_health" -> snapshot.scaleMaxHealth();
            case "scale_attack_damage" -> snapshot.scaleAttackDamage();
            case "scale_armor" -> snapshot.scaleArmor();
            case "debug_log" -> snapshot.debugLog();
            case "player_scaling.enabled" -> snapshot.playerScalingEnabled();
            case "player_scaling.dynamic" -> snapshot.playerScalingDynamic();
            default -> true;
        };
    }

    private void send(String field, String value) {
        PacketDistributor.sendToServer(new ConfigUpdatePayload(field, value));
    }

    private static double mult(List<Double> list, int i, double fallback) {
        if (list == null || i >= list.size()) {
            return fallback;
        }
        return list.get(i);
    }

    private static String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    public void onSnapshotUpdate() {
        this.snapshot = ClientConfigCache.latest();
        if (this.minecraft != null) {
            this.rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        String tier = snapshot == null ? "?" : snapshot.currentTier();
        g.drawString(this.font, "Tier: " + tier, this.width - 70, 8, TEXT_COLOR);
        if (!canEdit()) {
            g.drawString(this.font, "(Read-only - requires operator)", 10, this.height - 10, 0xFF5555);
        }

        switch (currentTab) {
            case 1 -> {
                int headerY = healthBoxes[0].getY() - 12;
                g.drawString(this.font, "health", X_BOX1, headerY, LABEL_COLOR);
                g.drawString(this.font, "damage", X_BOX2, headerY, LABEL_COLOR);
                g.drawString(this.font, "armor", X_BOX3, headerY, LABEL_COLOR);
                for (int i = 0; i < ScalingConfig.TIERS.length; i++) {
                    g.drawString(this.font, ScalingConfig.TIERS[i], 10, healthBoxes[i].getY() + 5, TEXT_COLOR);
                }
            }
            case 2 -> {
                g.drawString(this.font, "Health thr:", 10, thresholdBox.getY() + 5, LABEL_COLOR);
                g.drawString(this.font, "Armor cap:", X_BOX2, armorCapBox.getY() + 5, LABEL_COLOR);
                g.drawString(this.font, "Excluded:", 10, excludedBox.getY() + 5, LABEL_COLOR);
                g.drawString(this.font, "Bosses:", 10, bossBox.getY() + 5, LABEL_COLOR);
            }
            case 3 -> {
                g.drawString(this.font, "Radius:", 10, radiusBox.getY() + 5, LABEL_COLOR);
                g.drawString(this.font, "Exponent:", X_BOX2, exponentBox.getY() + 5, LABEL_COLOR);
            }
            case 4 -> {
                g.drawString(this.font, "vanilla", X_BOX1, minecraftMultBox.getY() - 12, LABEL_COLOR);
                g.drawString(this.font, "others", X_BOX2, minecraftMultBox.getY() - 12, LABEL_COLOR);
                g.drawString(this.font, "apoth", X_BOX3, minecraftMultBox.getY() - 12, LABEL_COLOR);
                g.drawString(this.font, "Mods (clique para editar):", 10, yModsList - 12, HEADER_COLOR);
                if (selectedMod != null) {
                    g.drawString(this.font, selectedMod + " (multiplicador):", 180, selectedModMultBox.getY() - 12, LABEL_COLOR);
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        modScroll -= (int) verticalAmount;
        modScroll = Math.max(0, modScroll);
        return true;
    }
}
