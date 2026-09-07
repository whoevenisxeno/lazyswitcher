package dev.xeno.accountswitcher.gui;

import dev.xeno.accountswitcher.AccountSwitcherMod;
import dev.xeno.accountswitcher.account.AccountStorage;
import dev.xeno.accountswitcher.account.PrismAccountReader;
import dev.xeno.accountswitcher.account.SavedAccount;
import dev.xeno.accountswitcher.auth.AuthManager;
import dev.xeno.accountswitcher.auth.ServerReconnector;
import dev.xeno.accountswitcher.auth.SessionSwapper;
import net.minecraft.client.MinecraftClient;
//? if >=1.21.11 {
import net.minecraft.client.gui.Click;
//?}
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
    private static final int ENTRY_H = 26;
    private static final int LIST_TOP = 52;
    private static final int LIST_W = 320;
    private static final int FOOTER = 104;

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
        int revertY = this.height - 96; // Revert
        int rowY = this.height - 72;    // Add / Switch / Remove
        int backY = this.height - 48;   // Back

        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("accountswitcher.button.revert", AccountSwitcherMod.originalSession.getUsername()),
            btn -> revert()
        ).dimensions(cx - 154, revertY, 308, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("accountswitcher.button.add"),
            btn -> this.client.setScreen(new AddAccountScreen(this))
        ).dimensions(cx - 154, rowY, 100, 20).build());

        switchBtn = ButtonWidget.builder(
            Text.translatable("accountswitcher.button.switch"),
            btn -> switchToSelected()
        ).dimensions(cx - 50, rowY, 100, 20).build();
        switchBtn.active = false;
        this.addDrawableChild(switchBtn);

        removeBtn = ButtonWidget.builder(
            Text.translatable("accountswitcher.button.remove"),
            btn -> removeSelected()
        ).dimensions(cx + 54, rowY, 100, 20).build();
        removeBtn.active = false;
        this.addDrawableChild(removeBtn);

        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("accountswitcher.button.back"),
            btn -> this.client.setScreen(parent)
        ).dimensions(cx - 100, backY, 200, 20).build());
    }

    private int listBottom() {
        return this.height - FOOTER;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - LIST_TOP) / ENTRY_H);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int cx = this.width / 2;
        ctx.fill(0, 0, this.width, this.height, Palette.BG);

        ctx.drawCenteredTextWithShadow(this.textRenderer, "LazySwitcher", cx, 14, Palette.TITLE);
        String cur = "playing as " + this.client.getSession().getUsername();
        ctx.drawCenteredTextWithShadow(this.textRenderer, cur, cx, 26, Palette.TEXT_DIM);
        ctx.fill(cx - LIST_W / 2, 40, cx + LIST_W / 2, 41, Palette.DIVIDER);

        int lx = cx - LIST_W / 2;
        int rows = visibleRows();
        int shown = Math.min(accounts.size(), scrollOffset + rows);
        for (int i = scrollOffset; i < shown; i++) {
            SavedAccount acc = accounts.get(i);
            int y = LIST_TOP + (i - scrollOffset) * ENTRY_H;
            int rowH = ENTRY_H - 3;
            boolean sel = i == selectedIndex;
            boolean hover = mouseX >= lx && mouseX <= lx + LIST_W && mouseY >= y && mouseY < y + rowH;
            ctx.fill(lx, y, lx + LIST_W, y + rowH, sel ? Palette.ROW_SEL : Palette.ROW);
            if (hover && !sel) ctx.fill(lx, y, lx + LIST_W, y + rowH, Palette.ROW_HOVER);

            boolean isPrism = i < prismCount;
            int badge = Palette.badge(isPrism, acc.type);
            ctx.fill(lx, y, lx + 3, y + rowH, badge);

            ctx.drawTextWithShadow(this.textRenderer, Text.literal(acc.username), lx + 12, y + 3, Palette.TEXT);
            String sub = isPrism ? "Prism" : switch (acc.type) {
                case MICROSOFT   -> "Microsoft";
                case OFFLINE     -> "Offline";
                case ALT_SERVICE -> "Alt service";
            };
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(sub), lx + 12, y + 13, Palette.TEXT_DIM);

            String tag = isPrism ? "PRISM" : switch (acc.type) {
                case MICROSOFT   -> "MSA";
                case OFFLINE     -> "OFFLINE";
                case ALT_SERVICE -> "ALT";
            };
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(tag),
                lx + LIST_W - this.textRenderer.getWidth(tag) - 10, y + 8, badge);
        }

        if (accounts.isEmpty())
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                "No accounts yet — use Add Account", cx, LIST_TOP + 12, Palette.TEXT_DIM);

        if (!status.isEmpty())
            ctx.drawCenteredTextWithShadow(this.textRenderer, status, cx, this.height - 110, statusColor);

        super.render(ctx, mouseX, mouseY, delta);
    }

    //? if <1.21.11 {
    /*@Override
    public boolean mouseClicked(double mx, double my, int button) {
        int lx = this.width / 2 - LIST_W / 2;
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
        return super.mouseClicked(mx, my, button);
    }
    *///?} else {
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
    //?}

    //? if <1.20.5 {
    /*@Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        int max = Math.max(0, accounts.size() - visibleRows());
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) Math.signum(amount)));
        return true;
    }
    *///?} else {
    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        int max = Math.max(0, accounts.size() - visibleRows());
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) Math.signum(vAmt)));
        return true;
    }
    //?}

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
