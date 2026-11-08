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
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_TON_CONNECT = 101;
    private WalletCardView cardView;
    private TextView updatedView;
    private TextView statusView;
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
    private View updateBanner;
    private TextView updateBannerTitle;
    private TextView updateBannerSize;
    private ImageView headerDonor;
    private String currentAddress;
    private String pendingApkUrl;
    private Api.AppRelease pendingRelease;
    private int versionTaps;
    private Toast wayneToast;
    private long versionTapAt;
    private boolean wayneOpen;
    private int massTaps;
    private long massTapAt;
    private boolean picoOpen;
    private static final String PICO_GALLERY = "https://virtualyoutuber.fandom.com/wiki/Gram_Pico/Gallery";
    private boolean updateDialogShown;
    private boolean apkDownloading;
    private long apkDownloadId = -1L;
    private Uri pendingApkUri;
    private BroadcastReceiver apkDownloadReceiver;
    private final Handler apkHandler = new Handler(Looper.getMainLooper());
    private final Runnable apkProgressTick = new Runnable() {
        @Override public void run() {
            if (!apkDownloading || apkDownloadId < 0L) {return;}
            pollApkDownloadProgress();
            apkHandler.postDelayed(this, 400L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupEdgeToEdge();
        setContentView(R.layout.activity_main);
        applySystemBarInsets(findViewById(R.id.root_scroll));
        applyUpdateBannerInsets(findViewById(R.id.update_banner));
        cardView = findViewById(R.id.card);
        updatedView = findViewById(R.id.updated);
        statusView = findViewById(R.id.status);
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
        headerDonor = findViewById(R.id.header_donor);
        View headerRow = findViewById(R.id.header_row);
        if (headerRow != null) {headerRow.setOnLongClickListener(v -> {showDonorClaim(); return true;});}
        findViewById(R.id.header_logo).setOnLongClickListener(v -> {showDonorClaim(); return true;});
        applyDonorBadge(Store.isDonor(this));
        addressInput.setTextColor(0xFF0088FF);
        addressInput.setHintTextColor(0x800C83FF);
        addressAction.setOnClickListener(view -> onAddressAction());
        ownAction.setOnClickListener(view -> onOwnAction());
        applyMode(Store.getMode(this));
        modeOwn.setOnClickListener(view -> selectMode("own"));
        modeOther.setOnClickListener(view -> selectMode("other"));
        notifySwitch.setChecked(Store.isNotifyEnabled(this));
        notifySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> onNotificationsChanged(isChecked));
        if (Store.isNotifyEnabled(this)) {Notifications.ensureChannel(this); Notifications.schedule(this);}
        findViewById(R.id.history_open).setOnClickListener(view -> openHistory());
        findViewById(R.id.tokens_open).setOnClickListener(view -> openTokens());
        findViewById(R.id.nfts_open).setOnClickListener(view -> openNfts());
        findViewById(R.id.refresh_now).setOnClickListener(view -> refreshNow());
        findViewById(R.id.add_widget).setOnClickListener(view -> pinWidget());
        cardView.setOnQrClickListener(this::showReceiveQr);
        cardView.setOnMassClickListener(this::onMassTap);
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
        showVersion();
        currentAddress = Store.getAddress(this);
        if (currentAddress != null) {
            addressInput.setText(currentAddress);
            addressInput.setSelection(currentAddress.length());
        }
        updateAddressAction();
        updateOwnPanel();
        showCardAddress(currentAddress);
        WalletData cached = Store.load(this);
        if (cached != null) {
            bind(cached);
            String error = Store.getError(this);
            if (error != null) {showFailure(error, "");}
        } else if (currentAddress == null) {
            statusView.setText(R.string.enter_address);
            statusView.setVisibility(View.VISIBLE);
        } else {
            statusView.setText(R.string.loading);
            statusView.setVisibility(View.VISIBLE);
        }
        CardDecor cachedCard = Store.loadCard(this);
        if (cachedCard != null && !cachedCard.isEmpty()) {cardView.setDecor(cachedCard);}
        if (currentAddress != null) {refresh(currentAddress);}
        GramWidgetProvider.renderAll(getApplicationContext());
    }

    private boolean isOwnMode() {return "own".equals(Store.getMode(this));}
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

    private void openTonConnect() {
        startActivityForResult(new Intent(this, TonConnectActivity.class), REQUEST_TON_CONNECT);
    }

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
        showCardAddress(null);
        cardView.setValues("—", "—");
        cardView.setDecor(CardDecor.EMPTY);
        cardView.setLoading(false);
        applyDonorBadge(false);
        statusView.setText(R.string.enter_address);
        statusView.setVisibility(View.VISIBLE);
        updatedView.setVisibility(View.INVISIBLE);
        GramWidgetProvider.renderAll(getApplicationContext());
        Toast.makeText(this, R.string.disconnect_wallet, Toast.LENGTH_SHORT).show();
    }

    private void onNotificationsChanged(boolean enabled) {
        if (enabled && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 200);
            return;
        }
        enableNotifications(enabled);
    }

    private void enableNotifications(boolean enabled) {
        Store.setNotifyEnabled(this, enabled);
        if (enabled) {
            Notifications.ensureChannel(this);
            Notifications.schedule(this);
            Notifications.pollAsync(this);
            if (currentAddress == null) {Toast.makeText(this, R.string.save_address_first, Toast.LENGTH_SHORT).show();}
        } else {Notifications.cancel(this);}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != 200) {return;}
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        notifySwitch.setChecked(granted);
        if (granted) {
            enableNotifications(true);
        } else {
            Store.setNotifyEnabled(this, false);
            Toast.makeText(this, R.string.notifications_denied, Toast.LENGTH_LONG).show();
        }
    }

    private void openHistory() {
        if (currentAddress == null) {
            Toast.makeText(this, R.string.save_address_first, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, HistoryActivity.class));
    }

    private void openTokens() {
        if (currentAddress == null) {
            Toast.makeText(this, R.string.save_address_first, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, TokensActivity.class));
    }

    private void openNfts() {
        if (currentAddress == null) {
            Toast.makeText(this, R.string.save_address_first, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, NftsActivity.class));
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

    private void showVersion() {
        TextView versionView = findViewById(R.id.version);
        updateBanner = findViewById(R.id.update_banner);
        updateBannerTitle = findViewById(R.id.update_banner_title);
        updateBannerSize = findViewById(R.id.update_banner_size);
        updateBanner.setVisibility(View.GONE);
        updateBanner.setOnClickListener(view -> {
            versionTaps = 0;
            if (pendingApkUri != null) {installDownloadedApk(pendingApkUri); return;}
            openUpdate();
        });
        versionView.setClickable(true);
        versionView.setFocusable(true);
        versionView.setOnClickListener(view -> onVersionTap());
        long code = 0L;
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            code = android.os.Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
            versionView.setText(getString(R.string.version_label, info.versionName, code));
        } catch (Exception ignored) {versionView.setText("");}
        checkForUpdate(code);
    }

    private void onVersionTap() {
        if (wayneOpen || picoOpen) {return;}
        long now = System.currentTimeMillis();
        if (now - versionTapAt > 3000L) {versionTaps = 0;}
        versionTapAt = now;
        versionTaps += 1;
        int left = 3 - versionTaps;
        if (left > 0) {
            if (wayneToast != null) {wayneToast.cancel();}
            wayneToast = Toast.makeText(this, getResources().getQuantityString(R.plurals.wayne_steps_left, left, left), Toast.LENGTH_SHORT);
            wayneToast.show();
            return;
        }
        versionTaps = 0;
        if (wayneToast != null) {wayneToast.cancel(); wayneToast = null;}
        showWayneEgg();
    }

    private void onMassTap() {
        if (wayneOpen || picoOpen) {return;}
        long now = System.currentTimeMillis();
        if (now - massTapAt > 3000L) {massTaps = 0;}
        massTapAt = now;
        massTaps += 1;
        int left = 3 - massTaps;
        if (left > 0) {
            if (wayneToast != null) {wayneToast.cancel();}
            wayneToast = Toast.makeText(this, getResources().getQuantityString(R.plurals.wayne_steps_left, left, left), Toast.LENGTH_SHORT);
            wayneToast.show();
            return;
        }
        massTaps = 0;
        if (wayneToast != null) {wayneToast.cancel(); wayneToast = null;}
        showPicoEgg();
    }

    private void showPicoEgg() {
        if (picoOpen || isFinishing()) {return;}
        picoOpen = true;
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        final float density = getResources().getDisplayMetrics().density;
        final PathInterpolator ease = new PathInterpolator(0.22f, 1f, 0.36f, 1f);
        final FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.TRANSPARENT);
        root.setAlpha(0f);
        final ImageView blurBg = new ImageView(this);
        blurBg.setScaleType(ImageView.ScaleType.FIT_XY);
        Bitmap snap = captureActivityBitmap();
        if (snap != null) {
            blurBg.setImageBitmap(snap);
            if (Build.VERSION.SDK_INT >= 31) {
                try {blurBg.setRenderEffect(RenderEffect.createBlurEffect(28f, 28f, Shader.TileMode.CLAMP));}
                catch (Exception ignored) {}
            }
        } else {blurBg.setBackgroundColor(0xCC000000);}
        root.addView(blurBg, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        final View dim = new View(this);
        dim.setBackgroundColor(0x44000000);
        root.addView(dim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Math.round(28 * density);
        panel.setPadding(pad, pad, pad, pad);
        ImageView gem = new ImageView(this);
        gem.setImageResource(R.drawable.white_diamond);
        gem.setAdjustViewBounds(true);
        int gemSize = Math.round(120 * density);
        panel.addView(gem, new LinearLayout.LayoutParams(gemSize, gemSize));
        Typeface matumemozi;
        try {matumemozi = getResources().getFont(R.font.matumemozi);} catch (Exception error) {matumemozi = Typeface.SANS_SERIF;}
        TextView text = new TextView(this);
        text.setText(R.string.pico_egg);
        text.setTextColor(0xFFFFFFFF);
        text.setGravity(Gravity.CENTER);
        text.setTypeface(matumemozi);
        text.setLineSpacing(8f * density, 1f);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
        applyCssTextShadow(text);
        text.setPadding(0, Math.round(18 * density), 0, 0);
        panel.addView(text, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView link = new TextView(this);
        link.setText(R.string.pico_gallery);
        link.setTextColor(0xFFFFFFFF);
        link.setGravity(Gravity.CENTER);
        link.setTypeface(matumemozi);
        link.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        applyCssTextShadow(link);
        link.setPadding(0, Math.round(10 * density), 0, 0);
        link.setOnClickListener(view -> {
            try {startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PICO_GALLERY)));}
            catch (Exception ignored) {}
        });
        panel.addView(link, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.setAlpha(0f);
        root.addView(panel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        final boolean[] closing = {false};
        Runnable finishClose = () -> {
            picoOpen = false;
            try {if (dialog.isShowing()) {dialog.dismiss();}} catch (Exception ignored) {}
        };
        Runnable close = () -> {
            if (closing[0]) {return;}
            closing[0] = true;
            root.animate().cancel();
            panel.animate().cancel();
            root.animate().alpha(0f).setDuration(520).setInterpolator(ease).withEndAction(finishClose).start();
            panel.animate().alpha(0f).setDuration(420).setInterpolator(ease).start();
        };
        root.setOnClickListener(view -> close.run());
        text.setOnClickListener(view -> close.run());
        dialog.setCancelable(true);
        dialog.setOnCancelListener(d -> close.run());
        dialog.setOnDismissListener(d -> picoOpen = false);
        dialog.setContentView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            window.setWindowAnimations(0);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            if (Build.VERSION.SDK_INT >= 30) {window.setDecorFitsSystemWindows(false);}
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.dimAmount = 0f;
            if (Build.VERSION.SDK_INT >= 28) {attrs.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;}
            window.setAttributes(attrs);
        }
        dialog.setOnShowListener(d -> {
            root.animate().alpha(1f).setDuration(520).setInterpolator(ease).start();
            panel.animate().alpha(1f).setDuration(560).setInterpolator(ease).start();
        });
        try {dialog.show();} catch (Exception error) {picoOpen = false;}
    }

    private void showWayneEgg() {
        if (wayneOpen || picoOpen || isFinishing()) {return;}
        wayneOpen = true;
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        final float density = getResources().getDisplayMetrics().density;
        final PathInterpolator ease = new PathInterpolator(0.22f, 1f, 0.36f, 1f);
        final FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.TRANSPARENT);
        root.setAlpha(0f);

        final ImageView blurBg = new ImageView(this);
        blurBg.setScaleType(ImageView.ScaleType.FIT_XY);
        Bitmap snap = captureActivityBitmap();
        if (snap != null) {
            blurBg.setImageBitmap(snap);
            if (Build.VERSION.SDK_INT >= 31) {
                try {blurBg.setRenderEffect(RenderEffect.createBlurEffect(28f, 28f, Shader.TileMode.CLAMP));}
                catch (Exception ignored) {}
            }
        } else {blurBg.setBackgroundColor(0xCC000000);}
        root.addView(blurBg, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        final View dim = new View(this);
        dim.setBackgroundColor(0x44000000);
        root.addView(dim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Math.round(28 * density);
        panel.setPadding(pad, pad, pad, pad);
        int side = Math.round(Math.min(getResources().getDisplayMetrics().widthPixels * 0.72f, getResources().getDisplayMetrics().heightPixels * 0.38f));
        final float radius = 22f * density;
        final boolean[] closing = {false};
        final FrameLayout card = new FrameLayout(this);
        card.setBackgroundColor(0xFF111111);
        card.setClipToOutline(true);
        card.setOutlineProvider(new ViewOutlineProvider() {@Override public void getOutline(View view, Outline outline) {outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);}});
        final CoverVideoView video = new CoverVideoView(this);
        video.setAlpha(0f);
        video.setOnErrorListener((player, what, extra) -> true);
        video.setOnPreparedListener(player -> {
            try {
                player.setLooping(true);
                player.setVolume(1f, 1f);
                if (!wayneOpen || closing[0] || !dialog.isShowing()) {return;}
                video.start();
                video.animate().cancel();
                video.animate().alpha(1f).setDuration(560).setInterpolator(ease).start();
            } catch (Exception ignored) {}
        });
        card.addView(video, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(side, side);
        cardLp.gravity = Gravity.CENTER_HORIZONTAL;
        panel.addView(card, cardLp);
        Typeface matumemozi;
        try {matumemozi = getResources().getFont(R.font.matumemozi);} catch (Exception error) {matumemozi = Typeface.SANS_SERIF;}
        TextView text = new TextView(this);
        text.setText(wayneEggWithSticker(density));
        text.setTextColor(0xFFFFFFFF);
        text.setGravity(Gravity.CENTER);
        text.setTypeface(matumemozi);
        text.setLineSpacing(8f * density, 1f);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
        applyCssTextShadow(text);
        text.setPadding(0, Math.round(18 * density), 0, 0);
        panel.addView(text, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView tiktok = new TextView(this);
        tiktok.setText(R.string.wayne_tiktok);
        tiktok.setTextColor(0xFFFFFFFF);
        tiktok.setGravity(Gravity.CENTER);
        tiktok.setTypeface(matumemozi);
        tiktok.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        applyCssTextShadow(tiktok);
        tiktok.setPadding(0, Math.round(10 * density), 0, 0);
        tiktok.setOnClickListener(view -> {
            try {startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/@waynee663")));}
            catch (Exception ignored) {}
        });
        panel.addView(tiktok, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.setAlpha(0f);
        root.addView(panel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        Runnable releaseVideo = () -> {
            try {video.animate().cancel();} catch (Exception ignored) {}
            try {video.stopPlayback();} catch (Exception ignored) {}
            try {video.suspend();} catch (Exception ignored) {}
            try {video.setOnPreparedListener(null); video.setOnErrorListener(null);} catch (Exception ignored) {}
        };
        Runnable finishClose = () -> {
            releaseVideo.run();
            wayneOpen = false;
            try {if (dialog.isShowing()) {dialog.dismiss();}} catch (Exception ignored) {}
        };
        Runnable close = () -> {
            if (closing[0]) {return;}
            closing[0] = true;
            try {video.pause();} catch (Exception ignored) {}
            root.animate().cancel();
            panel.animate().cancel();
            video.animate().cancel();
            root.animate().alpha(0f).setDuration(520).setInterpolator(ease).withEndAction(finishClose).start();
            panel.animate().alpha(0f).setDuration(420).setInterpolator(ease).start();
            video.animate().alpha(0f).setDuration(360).setInterpolator(ease).start();
        };
        root.setOnClickListener(view -> close.run());
        text.setOnClickListener(view -> close.run());
        dialog.setCancelable(true);
        dialog.setOnCancelListener(d -> close.run());
        dialog.setOnDismissListener(d -> {
            wayneOpen = false;
            releaseVideo.run();
        });
        dialog.setContentView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            window.setWindowAnimations(0);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            if (Build.VERSION.SDK_INT >= 30) {window.setDecorFitsSystemWindows(false);}
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.dimAmount = 0f;
            if (Build.VERSION.SDK_INT >= 28) {attrs.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;}
            window.setAttributes(attrs);
        }
        dialog.setOnShowListener(d -> {
            try {video.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.wayne));}
            catch (Exception ignored) {}
            root.animate().alpha(1f).setDuration(520).setInterpolator(ease).start();
            panel.animate().alpha(1f).setDuration(560).setInterpolator(ease).start();
        });
        try {dialog.show();} catch (Exception error) {wayneOpen = false;}
    }

    private static void applyCssTextShadow(TextView view) {
        float em = view.getTextSize();
        view.setShadowLayer(0.01f, 0.05f * em, 0.05f * em, 0xFF000000);
    }

    private CharSequence wayneEggWithSticker(float density) {
        SpannableStringBuilder text = new SpannableStringBuilder(getString(R.string.wayne_egg));
        text.append(' ');
        int start = text.length();
        text.append('\uFFFC');
        try {
            Drawable sticker = getDrawable(R.drawable.sticker);
            if (sticker != null) {
                int size = Math.round(30 * density);
                Drawable copy = sticker.mutate();
                copy.setBounds(0, 0, size, size);
                text.setSpan(new ImageSpan(copy, ImageSpan.ALIGN_BOTTOM), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } catch (Exception ignored) {}
        return text;
    }

    private Bitmap captureActivityBitmap() {
        try {
            View target = getWindow().getDecorView();
            if (target.getWidth() <= 0 || target.getHeight() <= 0) {return null;}
            Bitmap full = Bitmap.createBitmap(target.getWidth(), target.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(full);
            target.draw(canvas);
            int w = Math.max(1, full.getWidth() / 3);
            int h = Math.max(1, full.getHeight() / 3);
            Bitmap small = Bitmap.createScaledBitmap(full, w, h, true);
            if (small != full) {full.recycle();}
            return small;
        } catch (Exception error) {return null;}
    }

    private void checkForUpdate(final long currentCode) {
        new Thread(() -> {
            try {
                Api.AppRelease release = Api.fetchLatestRelease();
                if (release == null || release.apkUrl.isEmpty() || release.code <= currentCode) {return;}
                runOnUiThread(() -> {
                    if (isFinishing()) {return;}
                    showUpdateAvailable(release);
                });
            } catch (Exception ignored) {}
        }, "gram-update-check").start();
    }

    private void showUpdateAvailable(Api.AppRelease release) {
        pendingRelease = release;
        pendingApkUrl = release.apkUrl;
        String sizeText = formatApkSize(release.sizeBytes);
        if (updateBannerSize != null) {updateBannerSize.setText(sizeText);}
        if (updateBanner != null && updateBanner.getVisibility() != View.VISIBLE) {
            updateBanner.setTranslationY(Math.round(72 * getResources().getDisplayMetrics().density));
            updateBanner.setAlpha(0f);
            updateBanner.setVisibility(View.VISIBLE);
            updateBanner.animate().translationY(0f).alpha(1f).setDuration(420).setInterpolator(new PathInterpolator(0.22f, 1f, 0.36f, 1f)).start();
        }
        if (!updateDialogShown) {
            updateDialogShown = true;
            showUpdateDialog(release, sizeText);
        }
    }

    private void showUpdateDialog(Api.AppRelease release, String sizeText) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        final float density = getResources().getDisplayMetrics().density;
        final PathInterpolator ease = new PathInterpolator(0.22f, 1f, 0.36f, 1f);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0x99000000);
        root.setAlpha(0f);
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setBackgroundResource(R.drawable.update_sheet_bg);
        int pad = Math.round(22 * density);
        sheet.setPadding(pad, pad, pad, pad);
        sheet.setTranslationY(Math.round(40 * density));
        sheet.setAlpha(0f);
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.mipmap.ic_launcher);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        int iconSize = Math.round(72 * density);
        final float iconRadius = 18f * density;
        icon.setOutlineProvider(new ViewOutlineProvider() {@Override public void getOutline(View view, Outline outline) {outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), iconRadius);}});
        icon.setClipToOutline(true);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconLp.gravity = Gravity.CENTER_HORIZONTAL;
        sheet.addView(icon, iconLp);
        TextView title = new TextView(this);
        title.setText(R.string.update_available_title);
        title.setTextColor(0xFF000000);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, Math.round(16 * density), 0, 0);
        sheet.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView meta = new TextView(this);
        meta.setText(getString(R.string.update_meta, formatReleaseVersion(release), sizeText));
        meta.setTextColor(0xFF8E8E93);
        meta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        meta.setGravity(Gravity.CENTER);
        meta.setPadding(0, Math.round(6 * density), 0, 0);
        sheet.addView(meta, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView notes = new TextView(this);
        notes.setText(formatReleaseNotes(release.body, getString(R.string.update_no_notes)));
        notes.setTextColor(0xFF222222);
        notes.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        notes.setLineSpacing(4f * density, 1f);
        notes.setPadding(0, Math.round(14 * density), 0, 0);
        notes.setMaxLines(10);
        sheet.addView(notes, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Button download = new Button(this);
        download.setText(R.string.update_download_now);
        download.setAllCaps(false);
        download.setTextColor(Color.WHITE);
        download.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        download.setBackgroundResource(R.drawable.update_primary_btn);
        download.setStateListAnimator(null);
        LinearLayout.LayoutParams downloadLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(48 * density));
        downloadLp.topMargin = Math.round(18 * density);
        sheet.addView(download, downloadLp);
        TextView later = new TextView(this);
        later.setText(R.string.update_later);
        later.setTextColor(0xFF0088FF);
        later.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        later.setGravity(Gravity.CENTER);
        later.setPadding(0, Math.round(14 * density), 0, Math.round(4 * density));
        later.setClickable(true);
        later.setFocusable(true);
        sheet.addView(later, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        sheetLp.leftMargin = Math.round(28 * density);
        sheetLp.rightMargin = Math.round(28 * density);
        root.addView(sheet, sheetLp);
        Runnable dismiss = () -> {
            root.animate().cancel();
            sheet.animate().cancel();
            root.animate().alpha(0f).setDuration(220).setInterpolator(ease).start();
            sheet.animate().alpha(0f).translationY(Math.round(24 * density)).setDuration(240).setInterpolator(ease).withEndAction(() -> {
                try {if (dialog.isShowing()) {dialog.dismiss();}} catch (Exception ignored) {}
            }).start();
        };
        root.setOnClickListener(view -> dismiss.run());
        sheet.setOnClickListener(view -> {});
        download.setOnClickListener(view -> {dismiss.run(); openUpdate();});
        later.setOnClickListener(view -> dismiss.run());
        dialog.setContentView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dialog.setCancelable(true);
        dialog.setOnCancelListener(d -> {});
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setWindowAnimations(0);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        try {
            dialog.show();
            root.animate().alpha(1f).setDuration(280).setInterpolator(ease).start();
            sheet.animate().alpha(1f).translationY(0f).setDuration(360).setInterpolator(ease).start();
        } catch (Exception ignored) {}
    }

    private static String formatApkSize(long bytes) {
        if (bytes <= 0L) {return "—";}
        double mb = bytes / (1024d * 1024d);
        return String.format(Locale.getDefault(), "%.1f MB", mb).replace('.', ',');
    }

    private static String formatReleaseVersion(Api.AppRelease release) {
        String raw = release.tag != null && !release.tag.isEmpty() ? release.tag : (release.name == null ? "" : release.name);
        raw = raw.trim();
        if (raw.regionMatches(true, 0, "GramWallet.", 0, 11)) {raw = raw.substring(11);}
        int paren = raw.indexOf('(');
        if (paren > 0) {raw = raw.substring(0, paren).trim();}
        return raw.isEmpty() ? "—" : raw;
    }

    private static String formatReleaseNotes(String body, String fallback) {
        if (body == null || body.trim().isEmpty()) {return fallback;}
        String[] lines = body.replace("\r\n", "\n").split("\n");
        StringBuilder out = new StringBuilder();
        String pending = null;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {continue;}
            while (line.startsWith("#")) {line = line.substring(1).trim();}
            line = line.replace("**", "").replace("__", "");
            if (line.startsWith("-") || line.startsWith("*")) {
                String item = line.substring(1).trim();
                if (item.isEmpty()) {continue;}
                if (pending != null) {
                    if (out.length() > 0) {out.append('\n');}
                    out.append(pending);
                    pending = null;
                }
                if (out.length() > 0) {out.append('\n');}
                out.append("• ").append(item);
            } else {pending = line;}
        }
        String text = out.toString().trim();
        return text.isEmpty() ? fallback : text;
    }

    private void openUpdate() {
        if (pendingApkUri != null) {installDownloadedApk(pendingApkUri); return;}
        if (pendingApkUrl == null || pendingApkUrl.isEmpty() || apkDownloading) {return;}
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            try {startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));}
            catch (Exception ignored) {}
            Toast.makeText(this, R.string.update_allow_install, Toast.LENGTH_LONG).show();
            return;
        }
        DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (manager == null) {Toast.makeText(this, R.string.error_loading, Toast.LENGTH_SHORT).show(); return;}
        try {
            ensureApkDownloadReceiver();
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(pendingApkUrl));
            request.setTitle(getString(R.string.app_name));
            request.setDescription(getString(R.string.update_downloading));
            request.setMimeType("application/vnd.android.package-archive");
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "GramWallet-update.apk");
            request.addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 GramWallet");
            pendingApkUri = null;
            apkDownloading = true;
            apkDownloadId = manager.enqueue(request);
            setUpdateBannerProgress(0);
            apkHandler.removeCallbacks(apkProgressTick);
            apkHandler.post(apkProgressTick);
            Toast.makeText(this, R.string.update_downloading, Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            apkDownloading = false;
            Toast.makeText(this, R.string.error_loading, Toast.LENGTH_SHORT).show();
        }
    }

    private void setUpdateBannerProgress(int percent) {
        if (updateBannerTitle != null) {updateBannerTitle.setText(getString(R.string.update_progress, Math.max(0, Math.min(100, percent))));}
        if (updateBannerSize != null) {updateBannerSize.setText(Math.max(0, Math.min(100, percent)) + "%");}
    }

    private void setUpdateBannerReadyToInstall() {
        if (updateBannerTitle != null) {updateBannerTitle.setText(R.string.update_tap_install);}
        if (updateBannerSize != null && pendingRelease != null) {updateBannerSize.setText(formatApkSize(pendingRelease.sizeBytes));}
    }

    private void pollApkDownloadProgress() {
        DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (manager == null || apkDownloadId < 0L) {return;}
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(apkDownloadId);
        try (Cursor cursor = manager.query(query)) {
            if (cursor == null || !cursor.moveToFirst()) {return;}
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            long soFar = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            long total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            if (total > 0L) {setUpdateBannerProgress((int) (soFar * 100L / total));}
            if (status == DownloadManager.STATUS_SUCCESSFUL) {onApkDownloadFinished(apkDownloadId, true);}
            else if (status == DownloadManager.STATUS_FAILED) {onApkDownloadFinished(apkDownloadId, false);}
        } catch (Exception ignored) {}
    }

    private void onApkDownloadFinished(long id, boolean ok) {
        if (id < 0L || id != apkDownloadId) {return;}
        apkDownloadId = -1L;
        apkHandler.removeCallbacks(apkProgressTick);
        apkDownloading = false;
        if (!ok) {
            pendingApkUri = null;
            if (updateBannerTitle != null) {updateBannerTitle.setText(R.string.update_banner_title);}
            Toast.makeText(this, R.string.error_loading, Toast.LENGTH_SHORT).show();
            return;
        }
        DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        Uri uri = manager == null ? null : manager.getUriForDownloadedFile(id);
        if (uri == null) {
            Toast.makeText(this, R.string.error_loading, Toast.LENGTH_SHORT).show();
            return;
        }
        pendingApkUri = uri;
        setUpdateBannerReadyToInstall();
        installDownloadedApk(uri);
    }

    private void installDownloadedApk(Uri uri) {
        if (uri == null) {return;}
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            try {startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));}
            catch (Exception ignored) {}
            Toast.makeText(this, R.string.update_allow_install, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(uri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(install);
        } catch (Exception error) {Toast.makeText(this, R.string.error_loading, Toast.LENGTH_SHORT).show();}
    }

    private void ensureApkDownloadReceiver() {
        if (apkDownloadReceiver != null) {return;}
        apkDownloadReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
                if (id < 0 || id != apkDownloadId) {return;}
                DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                boolean ok = false;
                if (manager != null) {try (Cursor cursor = manager.query(new DownloadManager.Query().setFilterById(id))) {if (cursor != null && cursor.moveToFirst()) {ok = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL;}} catch (Exception ignored) {}}
                onApkDownloadFinished(id, ok);
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) {registerReceiver(apkDownloadReceiver, filter, Context.RECEIVER_EXPORTED);}
        else {registerReceiver(apkDownloadReceiver, filter);}
    }

    @Override
    protected void onDestroy() {
        apkHandler.removeCallbacks(apkProgressTick);
        if (apkDownloadReceiver != null) {
            try {unregisterReceiver(apkDownloadReceiver);} catch (Exception ignored) {}
            apkDownloadReceiver = null;
        }
        super.onDestroy();
    }

    private void saveAddress() {
        String raw = addressInput.getText().toString().trim();
        if (raw.replaceAll("\\s+", "").isEmpty()) {
            Toast.makeText(this, R.string.address_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        String address = TonAddress.normalize(raw);
        if (address == null) {
            statusView.setText(R.string.address_invalid_hint);
            statusView.setVisibility(View.VISIBLE);
            Toast.makeText(this, R.string.address_invalid, Toast.LENGTH_LONG).show();
            return;
        }
        addressInput.setText(address);
        addressInput.setSelection(address.length());
        hideKeyboard();
        connectAddress(address);
    }

    private void connectAddress(String address) {
        boolean changed = !address.equals(currentAddress);
        currentAddress = address;
        addressInput.setText(address);
        addressInput.setSelection(address.length());
        showCardAddress(address);
        updateAddressAction();
        updateOwnPanel();
        if (changed) {
            Store.setAddress(this, address);
            applyDonorBadge(false);
            if (Store.isNotifyEnabled(this)) {Notifications.schedule(this);}
            cardView.setValues("—", "—");
            cardView.setLoading(true);
            statusView.setText(R.string.loading);
            statusView.setVisibility(View.VISIBLE);
            updatedView.setVisibility(View.INVISIBLE);
            GramWidgetProvider.renderAll(getApplicationContext());
        }
        Toast.makeText(this, R.string.wallet_connected, Toast.LENGTH_SHORT).show();
        refresh(address);
    }

    private void refreshNow() {
        String address = currentAddress;
        if (address == null) {
            address = TonAddress.normalize(addressInput.getText().toString());
            if (address == null) {
                String raw = addressInput.getText().toString().trim();
                boolean empty = raw.replaceAll("\\s+", "").isEmpty();
                Toast.makeText(this,empty ? R.string.address_empty : R.string.address_invalid,Toast.LENGTH_SHORT).show();
                addressInput.requestFocus();
                return;
            }
            currentAddress = address;
            Store.setAddress(this, address);
            showCardAddress(address);
        }
        if (Store.load(this) == null) {
            statusView.setText(R.string.loading);
            statusView.setVisibility(View.VISIBLE);
        } else {statusView.setVisibility(View.GONE);}
        refresh(address);
    }

    private void refresh(final String address) {
        cardView.setLoading(true);
        final Context appContext = getApplicationContext();
        new Thread(() -> {
            WalletData data;
            try {
                data = Api.fetch(address);
                Store.save(appContext, address, data);
            } catch (Api.ApiException error) {
                Store.setError(appContext, address, error.kind);
                final String kind = error.kind;
                final String detail = error.getMessage() == null ? "" : error.getMessage();
                runOnUiThread(() -> {
                    if (isFinishing() || !address.equals(currentAddress)) {return;}
                    cardView.setLoading(false);
                    if (Store.load(appContext) == null) {updatedView.setVisibility(View.INVISIBLE);}
                    showFailure(kind, detail);
                });
                GramWidgetProvider.renderAll(appContext);
                return;
            } catch (Exception error) {
                Store.setError(appContext, address, Api.ERROR_NETWORK);
                final String detail = error.getMessage() == null ? "" : error.getMessage();
                runOnUiThread(() -> {
                    if (isFinishing() || !address.equals(currentAddress)) {return;}
                    cardView.setLoading(false);
                    showFailure(Api.ERROR_NETWORK, detail);
                });
                GramWidgetProvider.renderAll(appContext);
                return;
            }
            CardDecor decor = CardDecor.EMPTY;
            try {
                String cardRaw = Api.fetchCardRaw(address);
                if (cardRaw != null) {
                    Store.saveCard(appContext, address, cardRaw);
                    decor = Store.loadCard(appContext);
                    GiftImages.preload(appContext, decor);
                }
            } catch (Exception ignored) {}
            boolean donor = Api.isDonor(address);
            Store.setDonor(appContext, address, donor);
            final WalletData result = data;
            final CardDecor cardDecor = decor;
            final boolean donorFlag = donor;
            runOnUiThread(() -> {
                if (!isFinishing() && address.equals(currentAddress)) {
                    statusView.setVisibility(View.GONE);
                    statusView.setText("");
                    bind(result);
                    cardView.setDecor(cardDecor);
                    cardView.setLoading(false);
                    applyDonorBadge(donorFlag);
                }
            });
            GramWidgetProvider.renderAll(appContext);
        }, "gram-app-refresh").start();
    }

    private void applyDonorBadge(boolean donor) {if (headerDonor != null) {headerDonor.setVisibility(donor ? View.VISIBLE : View.GONE);}}

    private void showDonorClaim() {
        if (currentAddress == null) {
            Toast.makeText(this, R.string.donor_need_address, Toast.LENGTH_SHORT).show();
            return;
        }
        final float density = getResources().getDisplayMetrics().density;
        final PathInterpolator ease = new PathInterpolator(0.22f, 1f, 0.36f, 1f);
        final Dialog dialog = new Dialog(this);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0x99000000);
        root.setAlpha(0f);
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setBackgroundResource(R.drawable.update_sheet_bg);
        int pad = Math.round(22 * density);
        sheet.setPadding(pad, pad, pad, pad);
        sheet.setTranslationY(Math.round(40 * density));
        sheet.setAlpha(0f);
        TextView title = new TextView(this);
        title.setText(R.string.donor_claim_title);
        title.setTextColor(0xFF000000);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        sheet.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final EditText input = new EditText(this);
        input.setHint(R.string.donor_claim_hint);
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setBackgroundResource(R.drawable.input_white);
        input.setTextColor(0xFF0088FF);
        input.setHintTextColor(0x800C83FF);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        input.setGravity(Gravity.CENTER);
        input.setPadding(Math.round(14 * density), Math.round(14 * density), Math.round(14 * density), Math.round(14 * density));
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputLp.topMargin = Math.round(16 * density);
        sheet.addView(input, inputLp);
        Button unlock = new Button(this);
        unlock.setText(R.string.donor_claim_ok);
        unlock.setAllCaps(false);
        unlock.setTextColor(Color.WHITE);
        unlock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        unlock.setBackgroundResource(R.drawable.update_primary_btn);
        unlock.setStateListAnimator(null);
        LinearLayout.LayoutParams unlockLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(48 * density));
        unlockLp.topMargin = Math.round(16 * density);
        sheet.addView(unlock, unlockLp);
        TextView close = new TextView(this);
        close.setText(R.string.close);
        close.setTextColor(0xFF0088FF);
        close.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        close.setGravity(Gravity.CENTER);
        close.setPadding(0, Math.round(14 * density), 0, Math.round(4 * density));
        close.setClickable(true);
        close.setFocusable(true);
        sheet.addView(close, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        sheetLp.leftMargin = Math.round(28 * density);
        sheetLp.rightMargin = Math.round(28 * density);
        root.addView(sheet, sheetLp);
        Runnable dismiss = () -> {
            hideKeyboardFrom(input);
            root.animate().cancel();
            sheet.animate().cancel();
            root.animate().alpha(0f).setDuration(180).setInterpolator(ease).start();
            sheet.animate().alpha(0f).translationY(Math.round(24 * density)).setDuration(200).setInterpolator(ease).withEndAction(() -> {try {if (dialog.isShowing()) {dialog.dismiss();}} catch (Exception ignored) {}}).start();
        };
        root.setOnClickListener(v -> dismiss.run());
        sheet.setOnClickListener(v -> {});
        close.setOnClickListener(v -> dismiss.run());
        unlock.setOnClickListener(v -> {
            String code = input.getText().toString();
            dismiss.run();
            claimDonorCode(code);
        });
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String code = input.getText().toString();
                dismiss.run();
                claimDonorCode(code);
                return true;
            }
            return false;
        });
        dialog.setContentView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dialog.setCancelable(true);
        dialog.setOnCancelListener(d -> hideKeyboardFrom(input));
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setWindowAnimations(0);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        try {
            dialog.show();
            root.animate().alpha(1f).setDuration(220).setInterpolator(ease).start();
            sheet.animate().alpha(1f).translationY(0f).setDuration(280).setInterpolator(ease).start();
            input.requestFocus();
        } catch (Exception ignored) {}
    }

    private void hideKeyboardFrom(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {imm.hideSoftInputFromWindow(view.getWindowToken(), 0);}
    }

    private void claimDonorCode(String code) {
        final String address = currentAddress;
        if (address == null || code == null || code.trim().isEmpty()) {return;}
        final Context appContext = getApplicationContext();
        new Thread(() -> {
            boolean ok = Api.claimDonor(address, code.trim());
            if (ok) {Store.setDonor(appContext, address, true);}
            runOnUiThread(() -> {
                if (isFinishing()) {return;}
                if (ok) {
                    applyDonorBadge(true);
                    Toast.makeText(this, R.string.donor_claim_success, Toast.LENGTH_SHORT).show();
                } else {Toast.makeText(this, R.string.donor_claim_fail, Toast.LENGTH_SHORT).show();}
            });
        }, "gram-donor-claim").start();
    }

    private void showFailure(String kind, String detail) {
        String text = messageFor(kind, detail);
        if ((Api.ERROR_NETWORK.equals(kind) || Api.ERROR_TLS.equals(kind)) && !detail.isEmpty()) {text = text + "\n" + getString(R.string.error_detail, detail);}
        if (Store.load(this) != null) {text = text + "\n" + getString(R.string.cached_note);}
        statusView.setText(text);
        statusView.setVisibility(View.VISIBLE);
    }

    private String messageFor(String kind, String detail) {
        if (Api.ERROR_INVALID_ADDRESS.equals(kind)) {return getString(R.string.error_invalid_address);}
        if (Api.ERROR_NETWORK.equals(kind)) {return getString(R.string.error_network);}
        if (Api.ERROR_TLS.equals(kind)) {return getString(R.string.error_tls);}
        if (Api.ERROR_SERVER.equals(kind)) {return getString(R.string.error_server, detail);}
        if (Api.ERROR_BAD_RESPONSE.equals(kind)) {return getString(R.string.error_bad_response);}
        return getString(R.string.error_loading);
    }

    private void bind(WalletData data) {
        cardView.setValues(Format.shortGram(data.gram),data.hasUsd ? Format.usd(data.totalUsd) : "—");

        if (data.updatedAt > 0) {
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(data.updatedAt));
            updatedView.setText(getString(R.string.updated, time));
            updatedView.setVisibility(View.VISIBLE);
        }
    }

    private void showCardAddress(String address) {cardView.setAddress(address);}

    private void showReceiveQr() {
        if (currentAddress == null) {
            Toast.makeText(this, R.string.save_address_first, Toast.LENGTH_SHORT).show();
            return;
        }
        final String address = currentAddress;
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_receive);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(Math.round(getResources().getDisplayMetrics().widthPixels * 0.92f), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        if (!ReceiveActivity.bindReceiveViews(this, dialog.findViewById(R.id.receive_panel), address)) {return;}
        dialog.findViewById(R.id.receive_close).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void pinWidget() {
        if (currentAddress == null) {
            Toast.makeText(this, R.string.save_address_first, Toast.LENGTH_LONG).show();
            addressInput.requestFocus();
            return;
        }

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        if (!manager.isRequestPinAppWidgetSupported()) {
            Toast.makeText(this, R.string.pin_unsupported, Toast.LENGTH_LONG).show();
            return;
        }
        ComponentName provider = new ComponentName(this, GramWidgetProvider.class);
        boolean requested = manager.requestPinAppWidget(provider, null, null);
        Toast.makeText(this,requested ? R.string.pin_requested : R.string.pin_unsupported,Toast.LENGTH_LONG).show();
    }

    private void hideKeyboard() {
        InputMethodManager manager =(InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (manager != null) {manager.hideSoftInputFromWindow(addressInput.getWindowToken(), 0);}
        addressInput.clearFocus();
    }

    private void setupEdgeToEdge() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {window.setDecorFitsSystemWindows(false);} else {window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);}
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
            int left;
            int top;
            int right;
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;
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

    private void applyUpdateBannerInsets(View banner) {
        if (banner == null) {return;}
        final int baseLeft = banner.getPaddingLeft();
        final int baseTop = banner.getPaddingTop();
        final int baseRight = banner.getPaddingRight();
        final int baseBottom = banner.getPaddingBottom();
        banner.setOnApplyWindowInsetsListener((view, insets) -> {
            int bottom = Build.VERSION.SDK_INT >= 30 ? insets.getInsets(WindowInsets.Type.systemBars()).bottom : insets.getSystemWindowInsetBottom();
            view.setPadding(baseLeft, baseTop, baseRight, baseBottom + bottom);
            return insets;
        });
        banner.requestApplyInsets();
    }
}
