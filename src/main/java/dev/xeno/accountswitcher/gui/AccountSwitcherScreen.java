package dev.xeno.accountswitcher.gui;

import dev.xeno.accountswitcher.AccountSwitcherMod;
import dev.xeno.accountswitcher.account.AccountStorage;
import dev.xeno.accountswitcher.account.PrismAccountReader;
import dev.xeno.accountswitcher.account.SavedAccount;
import dev.xeno.accountswitcher.auth.AuthManager;
import dev.xeno.accountswitcher.auth.ServerReconnector;
import dev.xeno.accountswitcher.auth.SessionSwapper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AccountSwitcherScreen extends Screen {
    private static final int ENTRY_H = 24;
    private static final int LIST_TOP = 48;
    private static final int LIST_W = 320;

    private final Screen parent;
    private List<SavedAccount> accounts = new ArrayList<>();
    private int prismCount = 0;
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private ButtonWidget switchBtn;
    private ButtonWidget removeBtn;

    private String status = "";
    private int statusColor = Palette.TEXT;
    private boolean working = false;

    public AccountSwitcherScreen(Screen parent) {
        super(Text.translatable("accountswitcher.screen.title"));
        this.parent = parent;
    }

    private static List<SavedAccount> importedPrism(List<SavedAccount> prism, List<SavedAccount> local) {
        Set<String> known = new HashSet<>();
        for (SavedAccount s : local) if (s.uuid != null) known.add(s.uuid.toLowerCase());
        List<SavedAccount> out = new ArrayList<>();
        for (SavedAccount p : prism)
            if (p.uuid == null || !known.contains(p.uuid.toLowerCase())) out.add(p);
        return out;
    }

    private void reloadAccounts() {
        List<SavedAccount> local = AccountStorage.load();
        List<SavedAccount> prism = importedPrism(PrismAccountReader.load(), local);
        prismCount = prism.size();
        accounts = new ArrayList<>(prism);
        accounts.addAll(local);
    }

    @Override
    protected void init() {
        reloadAccounts();
        int cx = this.width / 2;
        int bottom = this.height - 28;

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("↺ " + AccountSwitcherMod.originalSession.getUsername()),
            btn -> revert()
        ).dimensions(cx - 100, bottom - 48, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("accountswitcher.button.add"),
            btn -> this.client.setScreen(new AddAccountScreen(this))
        ).dimensions(cx - 160, bottom - 24, 100, 20).build());

        switchBtn = ButtonWidget.builder(
            Text.translatable("accountswitcher.button.switch"),
            btn -> switchToSelected()
        ).dimensions(cx - 55, bottom - 24, 110, 20).build();
        switchBtn.active = false;
        this.addDrawableChild(switchBtn);

        removeBtn = ButtonWidget.builder(
            Text.translatable("accountswitcher.button.remove"),
            btn -> removeSelected()
        ).dimensions(cx + 60, bottom - 24, 100, 20).build();
        removeBtn.active = false;
        this.addDrawableChild(removeBtn);

        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("accountswitcher.button.back"),
            btn -> this.client.setScreen(parent)
        ).dimensions(cx - 100, bottom, 200, 20).build());
    }

    private int visibleRows() {
        return Math.max(1, (this.height - LIST_TOP - 62) / ENTRY_H);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int cx = this.width / 2;
        ctx.fill(0, 0, this.width, this.height, Palette.BG);

        ctx.drawCenteredTextWithShadow(this.textRenderer, "LazySwitcher", cx, 12, Palette.TITLE);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
            Text.translatable("accountswitcher.screen.tagline"), cx, 23, Palette.TEXT_DIM);
        ctx.fill(cx - LIST_W / 2, 36, cx + LIST_W / 2, 37, Palette.DIVIDER);

        String cur = "Playing as " + this.client.getSession().getUsername();
        int barY = this.height - 58;
        if (!status.isEmpty())
            ctx.drawCenteredTextWithShadow(this.textRenderer, status, cx, barY, statusColor);
        else
            ctx.drawCenteredTextWithShadow(this.textRenderer, cur, cx, barY, Palette.TEXT_DIM);

        int lx = cx - LIST_W / 2;
        int rows = visibleRows();
        for (int i = scrollOffset; i < Math.min(accounts.size(), scrollOffset + rows); i++) {
            SavedAccount acc = accounts.get(i);
            int y = LIST_TOP + (i - scrollOffset) * ENTRY_H;
            boolean sel = i == selectedIndex;
            boolean hover = mouseX >= lx && mouseX <= lx + LIST_W && mouseY >= y && mouseY < y + ENTRY_H - 2;
            ctx.fill(lx, y, lx + LIST_W, y + ENTRY_H - 2, sel ? Palette.ROW_SEL : Palette.ROW);
            if (hover && !sel) ctx.fill(lx, y, lx + LIST_W, y + ENTRY_H - 2, Palette.ROW_HOVER);

            boolean isPrism = i < prismCount;
            int badge = Palette.badge(isPrism, acc.type);
            ctx.fill(lx, y, lx + 3, y + ENTRY_H - 2, badge);

            ctx.drawTextWithShadow(this.textRenderer, Text.literal(acc.username), lx + 10, y + 4, Palette.TEXT);
            String sub = isPrism ? "Prism import" : switch (acc.type) {
                case MICROSOFT   -> "Microsoft";
                case OFFLINE     -> "Offline";
                case ALT_SERVICE -> "Alt service";
            };
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(sub), lx + 10, y + 13, Palette.TEXT_DIM);

            String tag = isPrism ? "PRISM" : switch (acc.type) {
                case MICROSOFT   -> "MSA";
                case OFFLINE     -> "OFFLINE";
                case ALT_SERVICE -> "ALT";
            };
            int tagW = this.textRenderer.getWidth(tag);
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(tag), lx + LIST_W - tagW - 8, y + 8, badge);
        }

        if (accounts.isEmpty())
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                "No accounts yet - use Add Account", cx, LIST_TOP + 10, Palette.TEXT_DIM);

        ctx.drawCenteredTextWithShadow(this.textRenderer,
            Text.translatable("accountswitcher.hint.reconnect"), cx, this.height - 48, Palette.TEXT_DIM);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        int lx = this.width / 2 - LIST_W / 2;
        double mx = click.x(), my = click.y();
        int rows = visibleRows();
        if (mx >= lx && mx <= lx + LIST_W && my >= LIST_TOP && my < LIST_TOP + rows * ENTRY_H) {
            int clicked = scrollOffset + (int) (my - LIST_TOP) / ENTRY_H;
            if (clicked < accounts.size()) {
                selectedIndex = clicked;
                switchBtn.active = !working;
                removeBtn.active = clicked >= prismCount;
                return true;
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        int max = Math.max(0, accounts.size() - visibleRows());
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) Math.signum(vAmt)));
        return true;
    }

    private void beginWork(String msg) {
        working = true;
        switchBtn.active = false;
        removeBtn.active = false;
        status = msg;
        statusColor = Palette.WARN;
    }

    private void onSwapDone(ServerInfo server) {
        working = false;
        if (server != null) {
            status = "";
            ServerReconnector.reconnect(this.client, server);
        } else {
            this.client.setScreen(parent);
        }
    }

    private void onSwapFailed(String err) {
        working = false;
        status = err;
        statusColor = Palette.ERROR;
        switchBtn.active = selectedIndex >= 0;
    }

    private void switchToSelected() {
        if (working || selectedIndex < 0 || selectedIndex >= accounts.size()) return;
        SavedAccount acc = accounts.get(selectedIndex);
        ServerInfo server = ServerReconnector.currentServer(this.client);
        beginWork("Signing in as " + acc.username + "…");
        AuthManager.applySession(acc, () -> onSwapDone(server), this::onSwapFailed);
    }

    private void revert() {
        if (working) return;
        ServerInfo server = ServerReconnector.currentServer(this.client);
        beginWork("Reverting to " + AccountSwitcherMod.originalSession.getUsername() + "…");
        SessionSwapper.revert(MinecraftClient.getInstance());
        onSwapDone(server);
    }

    private void removeSelected() {
        if (selectedIndex < prismCount || selectedIndex >= accounts.size()) return;
        accounts.remove(selectedIndex);
        AccountStorage.save(new ArrayList<>(accounts.subList(prismCount, accounts.size())));
        selectedIndex = -1;
        switchBtn.active = false;
        removeBtn.active = false;
    }

    public void refreshAccounts() {
        reloadAccounts();
        selectedIndex = -1;
        if (switchBtn != null) switchBtn.active = false;
        if (removeBtn != null) removeBtn.active = false;
    }
}
