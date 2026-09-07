package dev.xeno.accountswitcher.gui;

import dev.xeno.accountswitcher.account.AccountStorage;
import dev.xeno.accountswitcher.account.AccountType;
import dev.xeno.accountswitcher.account.SavedAccount;
import dev.xeno.accountswitcher.auth.AltServiceAuthFlow;
import dev.xeno.accountswitcher.auth.AuthManager;
import dev.xeno.accountswitcher.auth.AuthResult;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddAccountScreen extends Screen {
    private final AccountSwitcherScreen parent;
    private AccountType selectedType = AccountType.OFFLINE;
    private String selectedService = "TheAltening";
    private TextFieldWidget aliasField;
    private TextFieldWidget usernameField;
    private TextFieldWidget tokenField;
    private TextFieldWidget clientIdField;
    private ButtonWidget confirmButton;
    private String status = "";
    private int statusColor = 0xFFFFFF;
    private boolean working = false;

    public AddAccountScreen(AccountSwitcherScreen parent) {
        super(Text.translatable("accountswitcher.add.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = 58;

        //? if <1.21.11 {
        /*this.addDrawableChild(CyclingButtonWidget.<AccountType>builder(
            t -> Text.literal(switch (t) {
                case MICROSOFT   -> "Microsoft (MSA)";
                case OFFLINE     -> "Offline / Cracked";
                case ALT_SERVICE -> "Alt Service";
            }))
            .values(AccountType.values())
            .initially(selectedType)
            .build(cx - 100, y, 200, 20, Text.literal("Type"),
                (btn, val) -> { selectedType = val; rebuildWidgets(); }));
        *///?} else {
        this.addDrawableChild(CyclingButtonWidget.<AccountType>builder(
            t -> Text.literal(switch (t) {
                case MICROSOFT   -> "Microsoft (MSA)";
                case OFFLINE     -> "Offline / Cracked";
                case ALT_SERVICE -> "Alt Service";
            }), () -> selectedType)
            .values(AccountType.values())
            .build(cx - 100, y, 200, 20, Text.literal("Type"),
                (btn, val) -> { selectedType = val; rebuildWidgets(); }));
        //?}
        y += 26;

        aliasField = new TextFieldWidget(this.textRenderer, cx - 100, y, 200, 20, Text.literal(""));
        aliasField.setPlaceholder(Text.literal("Alias (optional)"));
        this.addDrawableChild(aliasField);
        y += 26;

        if (selectedType == AccountType.OFFLINE) {
            usernameField = new TextFieldWidget(this.textRenderer, cx - 100, y, 200, 20, Text.literal(""));
            usernameField.setPlaceholder(Text.literal("Username"));
            this.addDrawableChild(usernameField);
        } else if (selectedType == AccountType.MICROSOFT) {
            clientIdField = new TextFieldWidget(this.textRenderer, cx - 100, y, 200, 20, Text.literal(""));
            clientIdField.setPlaceholder(Text.literal("Azure Client ID (leave blank = default)"));
            clientIdField.setMaxLength(64);
            this.addDrawableChild(clientIdField);
        } else if (selectedType == AccountType.ALT_SERVICE) {
            //? if <1.21.11 {
            /*this.addDrawableChild(CyclingButtonWidget.<String>builder(Text::literal)
                .values("TheAltening", "EasyMC")
                .initially(selectedService)
                .build(cx - 100, y, 200, 20, Text.literal("Service"),
                    (btn, val) -> selectedService = val));
            *///?} else {
            this.addDrawableChild(CyclingButtonWidget.<String>builder(Text::literal, () -> selectedService)
                .values("TheAltening", "EasyMC")
                .build(cx - 100, y, 200, 20, Text.literal("Service"),
                    (btn, val) -> selectedService = val));
            //?}
            y += 26;
            tokenField = new TextFieldWidget(this.textRenderer, cx - 100, y, 200, 20, Text.literal(""));
            tokenField.setPlaceholder(Text.literal("your_token@thealtening.com"));
            this.addDrawableChild(tokenField);
        }

        confirmButton = ButtonWidget.builder(
            Text.translatable("accountswitcher.add.confirm"),
            btn -> confirm()
        ).dimensions(cx - 100, this.height - 52, 200, 20).build();
        this.addDrawableChild(confirmButton);

        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("accountswitcher.button.back"),
            btn -> this.client.setScreen(parent)
        ).dimensions(cx - 100, this.height - 28, 200, 20).build());
    }

    private void rebuildWidgets() {
        this.clearChildren();
        this.init();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int cx = this.width / 2;
        ctx.fill(0, 0, this.width, this.height, Palette.BG);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "LazySwitcher", cx, 12, Palette.TITLE);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 24, Palette.TEXT_DIM);
        ctx.fill(cx - 120, 36, cx + 120, 37, Palette.DIVIDER);
        if (selectedType == AccountType.MICROSOFT) {
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                "Opens a browser sign-in. Blank Client ID uses the built-in default.",
                cx, this.height - 80, Palette.TEXT_DIM);
        }
        if (!status.isEmpty())
            ctx.drawCenteredTextWithShadow(this.textRenderer, status, cx, this.height - 66, statusColor);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void confirm() {
        if (working) return;
        working = true;
        confirmButton.active = false;
        status = "Working…";
        statusColor = Palette.WARN;

        String alias = aliasField != null ? aliasField.getText().trim() : "";
        Map<String, String> params = new HashMap<>();
        params.put("alias", alias);

        if (selectedType == AccountType.OFFLINE) {
            String uname = usernameField != null ? usernameField.getText().trim() : "";
            if (uname.isEmpty()) { error("Username required"); return; }
            params.put("username", uname);
        } else if (selectedType == AccountType.MICROSOFT) {
            String cid = clientIdField != null ? clientIdField.getText().trim() : "";
            params.put("clientId", cid);
        } else if (selectedType == AccountType.ALT_SERVICE) {
            String tok = tokenField != null ? tokenField.getText().trim() : "";
            if (tok.isEmpty()) { error("Token required"); return; }
            params.put("serviceToken", tok);
            params.put("serviceName", selectedService);
            params.put("authServer", AltServiceAuthFlow.KNOWN_SERVICES.get(selectedService));
        }

        AuthManager.authenticateAsync(selectedType, params,
            result -> saveAndReturn(result, alias, params),
            errMsg -> error("Error: " + errMsg),
            () -> this.client.execute(() ->
                this.client.setScreen(new MsaDeviceCodeScreen(this)))
        );
    }

    private void saveAndReturn(AuthResult result, String alias, Map<String, String> params) {
        SavedAccount acc = new SavedAccount();
        acc.type         = selectedType;
        acc.alias        = alias.isEmpty() ? result.username() : alias;
        acc.username     = result.username();
        acc.uuid         = result.uuid();
        acc.accessToken  = result.accessToken();
        acc.refreshToken = result.refreshToken();
        acc.tokenExpiry  = result.tokenExpiry();
        acc.authServer   = result.authServer();
        if (selectedType == AccountType.ALT_SERVICE)
            acc.serviceToken = params.get("serviceToken");

        List<SavedAccount> all = AccountStorage.load();
        all.add(acc);
        AccountStorage.save(all);
        parent.refreshAccounts();
        this.client.setScreen(parent);
    }

    private void error(String msg) {
        status = msg;
        statusColor = Palette.ERROR;
        working = false;
        if (confirmButton != null) confirmButton.active = true;
    }
}
