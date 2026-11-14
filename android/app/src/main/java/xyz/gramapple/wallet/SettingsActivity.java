/*
✨ CoonDev • https://dev.coonlink.com/

 ▄█▄    ████▄ ████▄    ▄   ██▄   ▄███▄      ▄
 █▀ ▀▄  █   █ █   █     █  █  █  █▀   ▀      █
 █   ▀  █   █ █   █ ██   █ █   █ ██▄▄   █     █
 █▄  ▄▀ ▀████ ▀████ █ █  █ █  █  █▄   ▄▀ █    █
 ▀███▀              █  █ █ ███▀  ▀███▀    █  █
                    █   ██                 █▐
                                           ▐
*/

package xyz.gramapple.wallet;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.graphics.PorterDuff;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private static final int REQUEST_TON_CONNECT = 101;
    private EditText addressInput;
    private Button addressAction;
    private View ownPanel;
    private View otherPanel;
    private TextView ownStatus;
    private TextView ownAddress;
    private Button ownAction;
    private Button modeOwn;
    private Button modeOther;
    private Switch notifySwitch;
    private Switch notifyPayments;
    private Switch notifyGifts;
    private Switch notifyUpdates;
    private View notifyTypes;
    private String currentAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupEdgeToEdge();
        setContentView(R.layout.activity_settings);
        applySystemBarInsets(findViewById(R.id.settings_root));
        findViewById(R.id.settings_back).setOnClickListener(v -> finish());
        addressInput = findViewById(R.id.address_input);
        addressAction = findViewById(R.id.save_address);
        ownPanel = findViewById(R.id.own_panel);
        otherPanel = findViewById(R.id.other_panel);
        ownStatus = findViewById(R.id.own_status);
        ownAddress = findViewById(R.id.own_address);
        ownAction = findViewById(R.id.own_action);
        modeOwn = findViewById(R.id.mode_own);
        modeOther = findViewById(R.id.mode_other);
        notifySwitch = findViewById(R.id.notify_switch);
        notifyTypes = findViewById(R.id.notify_types);
        notifyPayments = findViewById(R.id.notify_payments);
        notifyGifts = findViewById(R.id.notify_gifts);
        notifyUpdates = findViewById(R.id.notify_updates);
        addressInput.setTextColor(0xFF0088FF);
        addressInput.setHintTextColor(0x800C83FF);
        addressAction.setOnClickListener(view -> onAddressAction());
        ownAction.setOnClickListener(view -> onOwnAction());
        applyMode(Store.getMode(this));
        modeOwn.setOnClickListener(view -> selectMode("own"));
        modeOther.setOnClickListener(view -> selectMode("other"));
        notifySwitch.setChecked(Store.isNotifyEnabled(this));
        notifyPayments.setChecked(Store.isNotifyPayments(this));
        notifyGifts.setChecked(Store.isNotifyGifts(this));
        notifyUpdates.setChecked(Store.isNotifyUpdates(this));
        refreshNotifyTypes();
        notifySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> onNotificationsChanged(isChecked));
        notifyPayments.setOnCheckedChangeListener((b, on) -> {Store.setNotifyPayments(this, on); Notifications.schedule(this);});
        notifyGifts.setOnCheckedChangeListener((b, on) -> {Store.setNotifyGifts(this, on); Notifications.schedule(this);});
        notifyUpdates.setOnCheckedChangeListener((b, on) -> {Store.setNotifyUpdates(this, on); Notifications.schedule(this);});
        if (Store.wantsAnyNotify(this)) {Notifications.ensureChannel(this); Notifications.schedule(this);}
        findViewById(R.id.refresh_now).setOnClickListener(view -> refreshNow());
        View addPanel = findViewById(R.id.add_panel);
        findViewById(R.id.wallet_add).setOnClickListener(v -> {
            if (!Store.canAddWallet(this)) {return;}
            boolean open = addPanel.getVisibility() != View.VISIBLE;
            addPanel.setVisibility(open ? View.VISIBLE : View.GONE);
            if (open) {
                addressInput.setText("");
                updateAddressAction();
                Store.setMode(this, "other");
                applyMode("other");
                addressInput.requestFocus();
            }
        });
        bindWalletsList();
        addressInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (!isPasteMode()) {saveAddress();}
                return true;
            }
            return false;
        });
        addressInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {updateAddressAction();}
        });
        currentAddress = Store.getAddress(this);
        addressInput.setText("");
        updateAddressAction();
        updateOwnPanel();
    }

    private boolean isPasteMode() {return addressInput.getText().toString().trim().isEmpty();}
    private void updateAddressAction() {addressAction.setText(isPasteMode() ? R.string.paste_address : R.string.save_address);}
    private void onAddressAction() {if (isPasteMode()) {pasteAddress();} else {saveAddress();}}

    private void onOwnAction() {
        if (currentAddress == null) {openTonConnect(); return;}
        disconnectWallet();
    }

    private void selectMode(String mode) {
        Store.setMode(this, mode);
        applyMode(mode);
        updateAddressAction();
        updateOwnPanel();
        if ("own".equals(mode) && currentAddress == null) {openTonConnect();}
    }

    private void openTonConnect() {startActivityForResult(new Intent(this, TonConnectActivity.class), REQUEST_TON_CONNECT);}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_TON_CONNECT || resultCode != RESULT_OK || data == null) {return;}
        String address = TonAddress.normalize(data.getStringExtra(TonConnectActivity.EXTRA_ADDRESS));
        if (address == null) {return;}
        Store.setMode(this, "own");
        applyMode("own");
        connectAddress(address);
    }

    private void applyMode(String mode) {
        boolean other = "other".equals(mode);
        modeOwn.setBackgroundResource(other ? R.drawable.button_mode_off : R.drawable.button_mode_on);
        modeOther.setBackgroundResource(other ? R.drawable.button_mode_on : R.drawable.button_mode_off);
        modeOwn.setTextColor(other ? 0xFFFFFFFF : getColor(R.color.gram_blue));
        modeOther.setTextColor(other ? getColor(R.color.gram_blue) : 0xFFFFFFFF);
        ownPanel.setVisibility(other ? View.GONE : View.VISIBLE);
        otherPanel.setVisibility(other ? View.VISIBLE : View.GONE);
        if (!other) {hideKeyboard();}
        updateOwnPanel();
    }

    private void updateOwnPanel() {
        if (currentAddress == null) {
            ownStatus.setText(R.string.own_connect_hint);
            ownAddress.setText("");
            ownAddress.setVisibility(View.GONE);
            ownAction.setText(R.string.connect_wallet);
            return;
        }
        ownStatus.setText(R.string.own_connected);
        ownAddress.setText(halfAddress(currentAddress));
        ownAddress.setVisibility(View.VISIBLE);
        ownAction.setText(R.string.disconnect_wallet);
    }

    private static String halfAddress(String address) {
        if (address == null || address.isEmpty()) {return "";}
        if (address.length() <= 20) {return address;}
        int half = Math.max(10, address.length() / 2);
        return address.substring(0, half) + "…";
    }

    private void disconnectWallet() {
        Store.clearAddress(this);
        currentAddress = null;
        addressInput.setText("");
        updateAddressAction();
        updateOwnPanel();
        GramWidgetProvider.renderAll(getApplicationContext());
        Toast.makeText(this, R.string.disconnect_wallet, Toast.LENGTH_SHORT).show();
        bindWalletsList();
    }

    private void refreshNotifyTypes() {if (notifyTypes != null) {notifyTypes.setVisibility(Store.isNotifyEnabled(this) ? View.VISIBLE : View.GONE);}}

    private void onNotificationsChanged(boolean enabled) {
        if (enabled && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 200);
            return;
        }
        enableNotifications(enabled);
    }

    private void enableNotifications(boolean enabled) {
        Store.setNotifyEnabled(this, enabled);
        refreshNotifyTypes();
        if (enabled) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null && !nm.areNotificationsEnabled()) {Toast.makeText(this, R.string.notifications_denied, Toast.LENGTH_LONG).show();}
            Notifications.ensureChannel(this);
            Notifications.schedule(this);
            Notifications.pollAsync(this);
            if (currentAddress == null && (Store.isNotifyPayments(this) || Store.isNotifyGifts(this))) {Toast.makeText(this, R.string.save_address_first, Toast.LENGTH_SHORT).show();}
        } else {Notifications.cancel(this);}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != 200) {return;}
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        notifySwitch.setChecked(granted);
        if (granted) {enableNotifications(true);}
        else {
            Store.setNotifyEnabled(this, false);
            Toast.makeText(this, R.string.notifications_denied, Toast.LENGTH_LONG).show();
        }
    }

    private void pasteAddress() {
        ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = manager == null ? null : manager.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence text = clip.getItemAt(0).coerceToText(this);
        String raw = text == null ? "" : text.toString().trim().replaceAll("\\s+", "");
        if (raw.isEmpty()) {
            Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        addressInput.setText(raw);
        addressInput.setSelection(addressInput.getText().length());
        updateAddressAction();
    }

    private void saveAddress() {
        String raw = addressInput.getText().toString().trim();
        if (raw.replaceAll("\\s+", "").isEmpty()) {
            Toast.makeText(this, R.string.address_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        String address = TonAddress.normalize(raw);
        if (address == null) {
            Toast.makeText(this, R.string.address_invalid, Toast.LENGTH_LONG).show();
            return;
        }
        addressInput.setText(address);
        addressInput.setSelection(address.length());
        hideKeyboard();
        connectAddress(address);
    }

    private void connectAddress(String address) {
        if (!Store.setAddress(this, address)) {
            Toast.makeText(this, getString(R.string.wallet_limit_free, Store.FREE_WALLET_LIMIT), Toast.LENGTH_LONG).show();
            return;
        }
        currentAddress = address;
        if (Store.wantsAnyNotify(this)) {Notifications.schedule(this);}
        GramWidgetProvider.renderAll(getApplicationContext());
        addressInput.setText("");
        updateAddressAction();
        updateOwnPanel();
        Toast.makeText(this, R.string.wallet_connected, Toast.LENGTH_SHORT).show();
        bindWalletsList();
        findViewById(R.id.add_panel).setVisibility(View.GONE);
    }

    private void refreshNow() {
        String address = currentAddress;
        if (address == null) {
            address = TonAddress.normalize(addressInput.getText().toString());
            if (address == null) {
                String raw = addressInput.getText().toString().trim();
                boolean empty = raw.replaceAll("\\s+", "").isEmpty();
                Toast.makeText(this, empty ? R.string.address_empty : R.string.address_invalid, Toast.LENGTH_SHORT).show();
                addressInput.requestFocus();
                return;
            }
            if (!Store.setAddress(this, address)) {
                Toast.makeText(this, getString(R.string.wallet_limit_free, Store.FREE_WALLET_LIMIT), Toast.LENGTH_LONG).show();
                return;
            }
            currentAddress = address;
            updateOwnPanel();
        }
        GramWidgetProvider.renderAll(getApplicationContext());
        Toast.makeText(this, R.string.refresh_now, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void updateWalletAddRow() {
        TextView add = findViewById(R.id.wallet_add);
        View addPanel = findViewById(R.id.add_panel);
        if (add == null) {return;}
        boolean can = Store.canAddWallet(this);
        if (can) {
            add.setText(R.string.wallet_add);
            add.setTextColor(0xFFFFFFFF);
            add.setTextSize(17f);
            add.setClickable(true);
            add.setEnabled(true);
        } else {
            add.setText(getString(R.string.wallet_limit_free, Store.FREE_WALLET_LIMIT));
            add.setTextColor(0x99FFFFFF);
            add.setTextSize(14f);
            add.setClickable(false);
            add.setEnabled(false);
            if (addPanel != null) {addPanel.setVisibility(View.GONE);}
        }
    }

    private void bindWalletsList() {
        LinearLayout list = findViewById(R.id.wallets_list);
        if (list == null) {return;}
        list.removeAllViews();
        updateWalletAddRow();
        String active = Store.getAddress(this);
        java.util.List<Store.WalletEntry> wallets = Store.getWallets(this);
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < wallets.size(); i++) {
            Store.WalletEntry e = wallets.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(Math.round(16 * d), Math.round(12 * d), Math.round(12 * d), Math.round(12 * d));
            row.setMinimumHeight(Math.round(52 * d));
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView title = new TextView(this);
            boolean isActive = e.address.equals(active);
            title.setText(e.label);
            title.setTextColor(0xFFFFFFFF);
            title.setTextSize(17f);
            TextView sub = new TextView(this);
            sub.setText(isActive ? getString(R.string.wallet_active) : (e.address.length() > 16 ? e.address.substring(0, 4) + "…" + e.address.substring(e.address.length() - 4) : e.address));
            sub.setTextColor(isActive ? 0xCCFFFFFF : 0x99FFFFFF);
            sub.setTextSize(13f);
            col.addView(title);
            col.addView(sub);
            row.addView(col);
            ImageView remove = new ImageView(this);
            int box = Math.round(40 * d);
            remove.setLayoutParams(new LinearLayout.LayoutParams(box, box));
            remove.setPadding(Math.round(9 * d), Math.round(9 * d), Math.round(9 * d), Math.round(9 * d));
            remove.setImageResource(R.drawable.ic_delete);
            remove.setContentDescription(getString(R.string.wallet_remove));
            remove.setScaleType(ImageView.ScaleType.FIT_CENTER);
            remove.setOnTouchListener((v, ev) -> {
                int a = ev.getActionMasked();
                if (a == MotionEvent.ACTION_DOWN) {remove.setColorFilter(0xFFFF453A, PorterDuff.Mode.SRC_IN);}
                else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {remove.clearColorFilter();}
                return false;
            });
            final String addr = e.address;
            row.setOnClickListener(v -> {
                Store.selectWallet(this, addr);
                currentAddress = addr;
                addressInput.setText(addr);
                updateOwnPanel();
                bindWalletsList();
                Toast.makeText(this, R.string.wallet_active, Toast.LENGTH_SHORT).show();
            });
            remove.setOnClickListener(v -> {
                GramWidgetProvider.removeForAddress(getApplicationContext(), addr);
                Store.removeWallet(this, addr);
                currentAddress = Store.getAddress(this);
                addressInput.setText(currentAddress == null ? "" : currentAddress);
                updateAddressAction();
                updateOwnPanel();
                bindWalletsList();
                GramWidgetProvider.renderAll(getApplicationContext());
            });
            row.addView(remove);
            list.addView(row);
            if (i < wallets.size() - 1) {
                View line = new View(this);
                line.setBackgroundColor(0x33FFFFFF);
                list.addView(line, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, Math.round(0.5f * d))));
            }
        }
        if (wallets.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.enter_address);
            empty.setTextColor(0x99FFFFFF);
            empty.setPadding(Math.round(16 * d), Math.round(16 * d), Math.round(16 * d), Math.round(16 * d));
            list.addView(empty);
        }
    }

    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (manager != null) {manager.hideSoftInputFromWindow(addressInput.getWindowToken(), 0);}
        addressInput.clearFocus();
    }

    private void setupEdgeToEdge() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {window.setDecorFitsSystemWindows(false);}
        else {window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);}
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) {window.setNavigationBarContrastEnforced(false);}
    }

    private void applySystemBarInsets(View root) {
        final int baseLeft = root.getPaddingLeft();
        final int baseTop = root.getPaddingTop();
        final int baseRight = root.getPaddingRight();
        final int baseBottom = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int left; int top; int right; int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                left = bars.left; top = bars.top; right = bars.right; bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(baseLeft + left, baseTop + top, baseRight + right, baseBottom + bottom);
            return insets;
        });
        root.requestApplyInsets();
    }
}
