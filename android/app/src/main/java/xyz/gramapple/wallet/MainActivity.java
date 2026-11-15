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
import android.text.SpannableStringBuilder;
import android.text.Spanned;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private WalletCardView cardView;
    private TextView updatedView;
    private TextView statusView;
    private View updateBanner;
    private TextView updateBannerTitle;
    private TextView updateBannerSize;
    private ImageView headerDonor;
    private LinearLayout todayList;
    private TextView todayEmpty;
    private BalanceChartView balanceChart;
    private CardStackView cardStack;
    private TextView chart7;
    private TextView chart30;
    private TextView chart90;
    private SlidePager slidePager;
    private MorphTabBar tabBar;
    private TabPages tabPages;
    private View activatePage;
    private View tasksPage;
    private int chartDays = 7;
    private String currentAddress;
    private int todaySeq;
    private int chartSeq;
    private final java.util.HashMap<String, java.util.List<Tx>> todayMem = new java.util.HashMap<>();
    private final java.util.HashMap<String, double[]> chartMem = new java.util.HashMap<>();
    private boolean skipNextResumeSync = true;
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
        applySystemBarInsets(findViewById(R.id.main_content));
        applyUpdateBannerInsets(findViewById(R.id.update_banner));
        cardStack = findViewById(R.id.card_stack);
        cardView = findViewById(R.id.card);
        if (cardStack != null) {
            cardStack.setBinder(this::bindWalletCard);
            cardStack.setListener(this::applyWalletSwitch);
            wireFrontCard();
        }
        updatedView = findViewById(R.id.updated);
        statusView = findViewById(R.id.status);
        todayList = findViewById(R.id.today_list);
        todayEmpty = findViewById(R.id.today_empty);
        balanceChart = findViewById(R.id.balance_chart);
        chart7 = findViewById(R.id.chart_7);
        chart30 = findViewById(R.id.chart_30);
        chart90 = findViewById(R.id.chart_90);
        chartDays = Store.getChartDays(this);
        if (chart7 != null) {
            chart7.setOnClickListener(v -> setChartDays(7));
            chart30.setOnClickListener(v -> setChartDays(30));
            chart90.setOnClickListener(v -> setChartDays(90));
            refreshChartChips();
        }
        headerDonor = findViewById(R.id.header_donor);
        View headerRow = findViewById(R.id.header_row);
        if (headerRow != null) {headerRow.setOnLongClickListener(v -> {showDonorClaim(); return true;});}
        findViewById(R.id.header_title).setOnLongClickListener(v -> {showDonorClaim(); return true;});
        findViewById(R.id.header_add).setOnClickListener(view -> pinWidget());
        findViewById(R.id.header_settings).setOnClickListener(view -> startActivity(new Intent(this, SettingsActivity.class)));
        View walletTip = findViewById(R.id.wallet_tip);
        if (walletTip != null) {walletTip.setOnClickListener(view -> onWalletTipClick());}
        findViewById(R.id.header_filter).setOnClickListener(view -> {if (tabPages != null) {tabPages.openHistoryFilter();}});
        findViewById(R.id.today_all).setOnClickListener(view -> openHistory());
        slidePager = findViewById(R.id.slide_pager);
        tabBar = findViewById(R.id.tab_bar);
        tabPages = new TabPages(this, findViewById(R.id.history_root), findViewById(R.id.tokens_root), findViewById(R.id.nfts_root));
        activatePage = findViewById(R.id.activate_root);
        tasksPage = findViewById(R.id.tasks_root);
        View activateCta = findViewById(R.id.activate_cta);
        if (activateCta != null) {activateCta.setOnClickListener(v -> requestWalletActivation());}
        bindTasks();
        syncExtraPages(false);
        tabBar.setListener(new MorphTabBar.Listener() {
            @Override public void onTabSelected(int index) {
                if (needsWallet(index) && Store.getAddress(MainActivity.this) == null) {
                    tabBar.setSelectedIndex(0, true);
                    applyHeader(0);
                    pulseWalletTip();
                    return;
                }
                slidePager.setCurrentPage(index, true);
                applyHeader(index);
                if (needsWallet(index)) {tabPages.ensure(index);}
            }
            @Override public void onTabLocked(int index) {pulseWalletTip();}
        });
        slidePager.setListener(new SlidePager.Listener() {
            @Override public void onScrollProgress(float progress) {
                tabBar.setScrollProgress(progress);
                applyHeader(Math.round(progress));
            }
            @Override public void onPageSelected(int index) {
                if (needsWallet(index) && Store.getAddress(MainActivity.this) == null) {
                    slidePager.setCurrentPage(0, false);
                    tabBar.setSelectedIndex(0, false);
                    applyHeader(0);
                    pulseWalletTip();
                    return;
                }
                tabBar.setSelectedIndex(index, false);
                applyHeader(index);
                if (needsWallet(index)) {tabPages.ensure(index);}
            }
        });
        applyHeader(0);
        applyDonorBadge(Store.isDonor(this));
        if (Store.wantsAnyNotify(this)) {Notifications.ensureChannel(this); Notifications.schedule(this);}
        cardView.setOnQrClickListener(this::showReceiveQr);
        cardView.setOnMassClickListener(this::onMassTap);
        cardView.setOnCardTapListener(() -> {if (cardStack != null) {cardStack.onFrontTap();}});
        showVersion();
        syncFromStore(false);
        GramWidgetProvider.renderAll(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (skipNextResumeSync) {skipNextResumeSync = false; return;}
        syncFromStore(false);
        if (tabPages != null) {tabPages.reloadIfAddressChanged();}
        maybeCelebrateTasks();
        if (Store.wantsAnyNotify(this)) {Notifications.schedule(this); Notifications.pollAsync(this);}
    }

    private void syncFromStore(boolean forceRefresh) {
        String address = Store.getAddress(this);
        boolean changed = address == null ? currentAddress != null : !address.equals(currentAddress);
        currentAddress = address;
        showCardAddress(currentAddress);
        applyDonorBadge(Store.isDonor(this));
        WalletData cached = Store.load(this);
        if (cached != null) {
            bind(cached);
            statusView.setVisibility(View.GONE);
            cardView.setLoading(false);
            String error = Store.getError(this);
            if (error != null) {showFailure(error, "");}
        } else if (currentAddress == null) {
            statusView.setText(R.string.enter_address);
            statusView.setVisibility(View.VISIBLE);
            cardView.setValues("—", "—");
            cardView.setDecor(CardDecor.EMPTY);
            cardView.setLoading(false);
            updatedView.setVisibility(View.INVISIBLE);
        } else {
            statusView.setText(R.string.loading);
            statusView.setVisibility(View.VISIBLE);
        }
        CardDecor cachedCard = Store.loadCard(this);
        if (cachedCard != null && !cachedCard.isEmpty()) {
            cardView.setDecor(cachedCard);
            maybePreloadGifts(currentAddress, cachedCard);
        }
        boolean fresh = currentAddress != null && Store.isFresh(this, currentAddress);
        if (currentAddress != null && cached == null) {refresh(currentAddress);}
        else if (currentAddress != null && !fresh) {refreshQuiet(currentAddress);}
        if (cardStack != null) {cardStack.setWallets(Store.getWallets(this), currentAddress);}
        updateWalletGate();
        syncExtraPages(false);
        warmWalletCaches();
        loadToday(!changed && fresh);
        loadChart(!changed && fresh);
    }

    private Runnable walletPulseLoop;

    private boolean needsActivate() {
        String addr = Store.getAddress(this);
        if (addr == null) {return false;}
        CardDecor decor;
        if (Store.hasCardCache(this, addr)) {decor = Store.loadCardCache(this, addr);}
        else if (Store.hasCard(this)) {decor = Store.loadCard(this);}
        else {return false;}
        return decor != null && !decor.telegramWallet;
    }

    private int activateTabIndex() {
        if (!needsActivate() || tabBar == null) {return -1;}
        return 4;
    }

    private void onWalletTipClick() {
        if (Store.getAddress(this) == null) {
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        if (needsActivate()) {openActivateTab(true); return;}
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void openActivateTab(boolean animate) {
        int idx = activateTabIndex();
        if (idx < 0 || tabBar == null || slidePager == null) {return;}
        tabBar.setSelectedIndex(idx, animate);
        slidePager.setCurrentPage(idx, animate);
        applyHeader(idx);
    }

    private void requestWalletActivation() {
        String address = Store.getAddress(this);
        if (address == null || address.isEmpty()) {
            Toast.makeText(this, R.string.save_address_first, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.activate_pending, Toast.LENGTH_SHORT).show();
    }

    private void syncExtraPages(boolean animateHide) {
        if (slidePager == null || tabBar == null) {return;}
        boolean needAct = needsActivate();
        boolean needTasks = !Store.isTasksTabHidden(this);
        if (activatePage == null) {activatePage = findViewById(R.id.activate_root);}
        if (tasksPage == null) {tasksPage = findViewById(R.id.tasks_root);}
        View act = activatePage;
        View tasks = tasksPage;
        int cur = slidePager.getCurrentPage();
        View curChild = cur >= 0 && cur < slidePager.getChildCount() ? slidePager.getChildAt(cur) : null;
        while (slidePager.getChildCount() > 4) {slidePager.removePageAt(slidePager.getChildCount() - 1);}
        if (needAct && act != null) {
            if (act.getParent() instanceof ViewGroup) {((ViewGroup) act.getParent()).removeView(act);}
            slidePager.addView(act);
        }
        if (needTasks && tasks != null) {
            if (tasks.getParent() instanceof ViewGroup) {((ViewGroup) tasks.getParent()).removeView(tasks);}
            slidePager.addView(tasks);
        }
        java.util.ArrayList<Integer> icons = new java.util.ArrayList<>();
        icons.add(R.drawable.ic_tab_wallet);
        icons.add(R.drawable.ic_tab_history);
        icons.add(R.drawable.ic_tab_tokens);
        icons.add(R.drawable.ic_tab_nfts);
        if (needAct) {icons.add(R.drawable.ic_tab_activate);}
        if (needTasks) {icons.add(R.drawable.ic_tab_tasks);}
        int[] ids = new int[icons.size()];
        for (int i = 0; i < icons.size(); i++) {ids[i] = icons.get(i);}
        tabBar.setTabIcons(ids);
        int next = 0;
        if (curChild != null) {for (int i = 0; i < slidePager.getChildCount(); i++) {if (slidePager.getChildAt(i) == curChild) {next = i; break;}}}
        if (next >= slidePager.getChildCount()) {next = Math.max(0, slidePager.getChildCount() - 1);}
        slidePager.setCurrentPage(next, false);
        tabBar.setSelectedIndex(next, false);
        applyHeader(next);
        updateWalletTip();
    }

    private void updateWalletTip() {
        boolean locked = Store.getAddress(this) == null;
        boolean activate = !locked && needsActivate();
        boolean showTip = locked || activate;
        View tip = findViewById(R.id.wallet_tip);
        View pulse = findViewById(R.id.wallet_tip_pulse);
        if (tip instanceof TextView) {
            ((TextView) tip).setText(activate ? R.string.activate_tip : R.string.add_wallet_tip);
            tip.setVisibility(showTip && (slidePager == null || slidePager.getCurrentPage() <= 0) ? View.VISIBLE : View.GONE);
        }
        if (pulse != null) {
            pulse.animate().cancel();
            pulse.removeCallbacks(walletPulseLoop);
            if (locked) {
                pulse.setVisibility(View.VISIBLE);
                walletPulseLoop = () -> {
                    if (isFinishing() || Store.getAddress(this) != null) {pulse.setVisibility(View.GONE); return;}
                    pulse.setScaleX(1f); pulse.setScaleY(1f); pulse.setAlpha(.9f);
                    pulse.animate().scaleX(1.4f).scaleY(1.4f).alpha(0f).setDuration(1000L).withEndAction(() -> {if (walletPulseLoop != null) {pulse.postDelayed(walletPulseLoop, 500L);}}).start();
                };
                pulse.post(walletPulseLoop);
            } else {
                walletPulseLoop = null;
                pulse.setVisibility(View.GONE);
            }
        }
    }

    private void updateWalletGate() {
        boolean locked = Store.getAddress(this) == null;
        if (tabBar != null) {tabBar.setExtrasLocked(locked);}
        if (slidePager != null) {
            slidePager.setSwipeLocked(locked);
            if (locked && slidePager.getCurrentPage() > 0) {
                slidePager.setCurrentPage(0, false);
                if (tabBar != null) {tabBar.setSelectedIndex(0, false);}
                applyHeader(0);
            }
        }
        updateWalletTip();
    }

    private void pulseWalletTip() {
        View tip = findViewById(R.id.wallet_tip);
        View pulse = findViewById(R.id.wallet_tip_pulse);
        View settings = findViewById(R.id.header_settings);
        if (tip != null) {
            tip.setVisibility(View.VISIBLE);
            tip.animate().cancel();
            tip.setScaleX(1f); tip.setScaleY(1f);
            tip.animate().scaleX(1.08f).scaleY(1.08f).setDuration(160L).withEndAction(() -> tip.animate().scaleX(1f).scaleY(1f).setDuration(160L).start()).start();
        }
        if (pulse != null) {
            pulse.setVisibility(View.VISIBLE);
            pulse.animate().cancel();
            pulse.setScaleX(1f); pulse.setScaleY(1f); pulse.setAlpha(1f);
            pulse.animate().scaleX(1.55f).scaleY(1.55f).alpha(0f).setDuration(520L).start();
        }
        if (settings != null) {
            settings.animate().cancel();
            settings.animate().scaleX(1.12f).scaleY(1.12f).setDuration(140L).withEndAction(() -> settings.animate().scaleX(1f).scaleY(1f).setDuration(140L).start()).start();
        }
    }

    private void wireFrontCard() {
        if (cardStack != null) {cardView = cardStack.getFrontCard();}
        if (cardView == null) {return;}
        cardView.setOnQrClickListener(this::showReceiveQr);
        cardView.setOnMassClickListener(this::onMassTap);
        cardView.setOnCardTapListener(() -> {if (cardStack != null) {cardStack.onFrontTap();}});
    }

    private void applyWalletSwitch(String address) {
        Store.selectWallet(this, address);
        currentAddress = address;
        wireFrontCard();
        applyDonorBadge(Store.isDonor(this));
        WalletData cached = Store.load(this);
        if (cached == null) {cached = Store.loadCache(this, address);}
        if (cached != null) {
            if (cached.updatedAt > 0) {
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(cached.updatedAt));
                updatedView.setText(getString(R.string.updated, time));
                updatedView.setVisibility(View.VISIBLE);
            }
            statusView.setVisibility(View.GONE);
            bind(cached);
        }
        syncExtraPages(false);
        todayFp = "";
        final String addr = address;
        View root = findViewById(android.R.id.content);
        Runnable defer = () -> {
            if (isFinishing() || !addr.equals(currentAddress)) {return;}
            loadToday(true);
            loadChart(true);
            if (!Store.isFresh(this, addr)) {refreshQuiet(addr);}
        };
        if (root != null) {root.post(defer);} else {runOnUiThread(defer);}
    }

    private void bindWalletCard(WalletCardView view, String address) {
        if (view == null || address == null) {return;}
        WalletData data = Store.loadCache(this, address);
        CardDecor decor = Store.loadCardCache(this, address);
        if (decor == null || decor.isEmpty()) {decor = CardDecor.EMPTY;}
        String bal = data != null ? Format.shortGram(data.gram) : "—";
        String usd = data != null && data.hasUsd ? Format.usd(data.totalUsd) : "—";
        view.bindWallet(address, bal, usd, decor);
        view.setLoading(false);
        if (!decor.isEmpty()) {maybePreloadGifts(address, decor);}
    }

    private void maybePreloadGifts(String address, CardDecor decor) {
        if (decor == null || decor.isEmpty()) {return;}
        if (GiftImages.isReady(this, decor)) {return;}
        final String addr = address;
        GiftImages.preloadAsync(getApplicationContext(), decor, () -> {
            if (isFinishing()) {return;}
            if (addr != null && addr.equals(currentAddress) && cardView != null) {cardView.refreshArt();}
            if (cardStack != null && addr != null) {cardStack.updateAddress(addr);}
            GramWidgetProvider.renderAll(getApplicationContext());
        });
    }

    private void warmWalletCaches() {
        final Context app = getApplicationContext();
        final java.util.List<Store.WalletEntry> list = Store.getWallets(this);
        if (list.isEmpty()) {return;}
        for (Store.WalletEntry e : list) {
            if (Store.isFresh(app, e.address)) {continue;}
            final String addr = e.address;
            new Thread(() -> {
                try {
                    WalletData data = Api.fetch(addr);
                    Store.save(app, addr, data);
                } catch (Exception ignored) {}
                CardDecor decor = CardDecor.EMPTY;
                try {
                    String raw = Api.fetchCardRaw(addr);
                    if (raw != null) {
                        Store.saveCard(app, addr, raw);
                        decor = Store.loadCardCache(app, addr);
                    }
                } catch (Exception ignored) {}
                final CardDecor cardDecor = decor;
                runOnUiThread(() -> {
                    if (isFinishing() || cardStack == null) {return;}
                    cardStack.updateAddress(addr);
                    if (addr.equals(currentAddress)) {
                        WalletData active = Store.load(this);
                        if (active != null) {bind(active);}
                        if (cardDecor != null && !cardDecor.isEmpty() && cardView != null) {cardView.setDecor(cardDecor);}
                        syncExtraPages(false);
                    }
                });
            }, "gram-warm").start();
        }
    }

    private void refreshQuiet(final String address) {
        final Context appContext = getApplicationContext();
        new Thread(() -> {
            try {
                WalletData data = Api.fetch(address);
                Store.save(appContext, address, data);
                String cardRaw = Api.fetchCardRaw(address);
                if (cardRaw != null) {Store.saveCard(appContext, address, cardRaw);}
                boolean donor = Api.isDonor(address);
                Store.setDonor(appContext, address, donor);
                final WalletData result = data;
                final CardDecor decor = Store.loadCardCache(appContext, address);
                final boolean donorFlag = donor;
                runOnUiThread(() -> {
                    if (isFinishing()) {return;}
                    if (cardStack != null) {cardStack.updateAddress(address);}
                    if (!address.equals(currentAddress)) {return;}
                    bind(result);
                    if (decor != null && !decor.isEmpty() && cardView != null) {cardView.setDecor(decor);}
                    if (cardView != null) {cardView.setLoading(false);}
                    applyDonorBadge(donorFlag);
                    maybePreloadGifts(address, decor);
                    syncExtraPages(false);
                });
                GramWidgetProvider.renderAll(appContext);
            } catch (Exception ignored) {}
        }, "gram-quiet").start();
    }


    private static boolean needsWallet(int page) {return page > 0;}
    private boolean tasksCelebrating;

    private void bindTasks() {
        LinearLayout list = findViewById(R.id.tasks_list);
        View progress = findViewById(R.id.tasks_progress);
        if (list == null) {return;}
        String cached = Store.loadTasksCache(this);
        if (cached != null) {
            try {
                java.util.List<Api.TaskItem> items = Api.parseTasks(new org.json.JSONObject(cached));
                if (!items.isEmpty()) {renderTasks(list, items); applyTasksVisibility();}
            } catch (Exception ignored) {}
        }
        if (progress != null && list.getChildCount() == 0) {progress.setVisibility(View.VISIBLE);}
        new Thread(() -> {
            java.util.List<Api.TaskItem> items = Api.fetchTasks();
            runOnUiThread(() -> {
                if (progress != null) {progress.setVisibility(View.GONE);}
                if (items.isEmpty()) {
                    if (list.getChildCount() == 0) {applyTasksVisibility();}
                    return;
                }
                Store.saveTasksCache(this, Api.tasksToCache(items));
                renderTasks(list, items);
                applyTasksVisibility();
                maybeCelebrateTasks();
            });
        }, "gram-tasks").start();
    }

    private void applyTasksVisibility() {
        if (!Store.areAllTasksDone(this)) {Store.setTasksTabHidden(this, false);}
        syncExtraPages(false);
    }

    private void renderTasks(LinearLayout list, java.util.List<Api.TaskItem> items) {
        list.removeAllViews();
        float d = getResources().getDisplayMetrics().density;
        Locale locale = getResources().getConfiguration().locale;
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Api.TaskItem task = items.get(i);
            ids.add(task.id);
            if (i > 0) {
                View line = new View(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.round(1 * d));
                lp.setMarginStart(Math.round(54 * d));
                line.setLayoutParams(lp);
                line.setBackgroundColor(0x22FFFFFF);
                list.addView(line);
            }
            list.addView(makeTaskRow(task, locale, d));
        }
        Store.setTaskIds(this, ids);
    }

    private View makeTaskRow(Api.TaskItem task, Locale locale, float d) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(Math.round(64 * d));
        row.setPadding(Math.round(14 * d), Math.round(12 * d), Math.round(12 * d), Math.round(12 * d));
        ImageView icon = new ImageView(this);
        int box = Math.round(28 * d);
        icon.setLayoutParams(new LinearLayout.LayoutParams(box, box));
        icon.setImageResource(taskIcon(task.icon));
        icon.setContentDescription(task.titleFor(locale));
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textLp.setMarginStart(Math.round(12 * d));
        text.setLayoutParams(textLp);
        TextView title = new TextView(this);
        title.setText(task.titleFor(locale));
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(16f);
        TextView hint = new TextView(this);
        hint.setText(task.hintFor(locale));
        hint.setTextColor(0x99FFFFFF);
        hint.setTextSize(12f);
        hint.setPadding(0, Math.round(2 * d), 0, 0);
        text.addView(title);
        text.addView(hint);
        Button action = new Button(this);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Math.round(36 * d));
        btnLp.setMarginStart(Math.round(10 * d));
        action.setLayoutParams(btnLp);
        action.setMinHeight(0);
        action.setMinWidth(0);
        action.setPadding(Math.round(14 * d), 0, Math.round(14 * d), 0);
        action.setAllCaps(false);
        action.setTextSize(13f);
        action.setStateListAnimator(null);
        Runnable paint = () -> {
            boolean done = Store.isTaskDone(this, task.id);
            action.setText(done ? R.string.task_done : R.string.task_do);
            action.setBackgroundResource(done ? R.drawable.button_mode_off : R.drawable.button_white);
            action.setTextColor(done ? 0xFFFFFFFF : 0xFF0088FF);
        };
        paint.run();
        View.OnClickListener open = v -> {
            try {startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(task.url)));}
            catch (Exception e) {Toast.makeText(this, R.string.tonconnect_wallet_missing, Toast.LENGTH_SHORT).show(); return;}
            Store.setTaskDone(this, task.id, true);
            paint.run();
            applyDonorBadge(Store.isDonor(this));
            maybeCelebrateTasks();
        };
        row.setOnClickListener(open);
        action.setOnClickListener(open);
        row.addView(icon);
        row.addView(text);
        row.addView(action);
        return row;
    }

    private static int taskIcon(String icon) {
        if ("group".equals(icon)) {return R.drawable.ic_task_group;}
        if ("nfts".equals(icon)) {return R.drawable.ic_tab_nfts;}
        if ("coondev".equals(icon)) {return R.drawable.ic_coondev;}
        if ("tasks".equals(icon)) {return R.drawable.ic_tab_tasks;}
        return R.drawable.ic_task_channel;
    }

    private void maybeCelebrateTasks() {
        if (tasksCelebrating || Store.isTasksTabHidden(this)) {return;}
        if (!Store.areAllTasksDone(this)) {return;}
        View tasks = findViewById(R.id.tasks_root);
        if (tabBar == null || tasks == null || tasks.getParent() == null) {Store.setTasksTabHidden(this, true); syncExtraPages(false); return;}
        tasksCelebrating = true;
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        TaskSaluteView salute = new TaskSaluteView(this);
        root.addView(salute, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        salute.play(() -> {
            Store.setTasksTabHidden(this, true);
            if (tasks.getParent() != null) {
                tasks.animate().alpha(0f).scaleX(.92f).scaleY(.92f).setDuration(420L).withEndAction(() -> {
                    tasks.setAlpha(1f); tasks.setScaleX(1f); tasks.setScaleY(1f);
                    syncExtraPages(false);
                    root.removeView(salute);
                    tasksCelebrating = false;
                    applyDonorBadge(Store.isDonor(this));
                }).start();
                if (tabBar != null) {tabBar.animate().alpha(.55f).setDuration(200L).withEndAction(() -> tabBar.animate().alpha(1f).setDuration(280L).start()).start();}
            } else {
                syncExtraPages(false);
                root.removeView(salute);
                tasksCelebrating = false;
                applyDonorBadge(Store.isDonor(this));
            }
        });
    }

    private void applyHeader(int page) {
        TextView title = findViewById(R.id.header_title);
        View add = findViewById(R.id.header_add);
        View settings = findViewById(R.id.header_settings);
        View filter = findViewById(R.id.header_filter);
        View tip = findViewById(R.id.wallet_tip);
        View pulse = findViewById(R.id.wallet_tip_pulse);
        if (title == null) {return;}
        View pageView = slidePager != null && page >= 0 && page < slidePager.getChildCount() ? slidePager.getChildAt(page) : null;
        int pageId = pageView != null ? pageView.getId() : R.id.root_scroll;
        Runnable hideTip = () -> {
            if (tip != null) {tip.setVisibility(View.GONE);}
            if (pulse != null) {pulse.setVisibility(View.GONE);}
            if (headerDonor != null) {headerDonor.setVisibility(View.GONE);}
        };
        if (pageId == R.id.history_root) {
            title.setText(R.string.history_title);
            add.setVisibility(View.INVISIBLE); settings.setVisibility(View.GONE); filter.setVisibility(View.VISIBLE);
            hideTip.run();
            return;
        }
        if (pageId == R.id.tokens_root) {
            title.setText(R.string.tokens_title);
            add.setVisibility(View.INVISIBLE); settings.setVisibility(View.GONE); filter.setVisibility(View.GONE);
            hideTip.run();
            return;
        }
        if (pageId == R.id.nfts_root) {
            title.setText(R.string.nfts_title);
            add.setVisibility(View.INVISIBLE); settings.setVisibility(View.GONE); filter.setVisibility(View.GONE);
            hideTip.run();
            return;
        }
        if (pageId == R.id.activate_root) {
            title.setText(R.string.activate_title);
            add.setVisibility(View.INVISIBLE); settings.setVisibility(View.GONE); filter.setVisibility(View.GONE);
            hideTip.run();
            return;
        }
        if (pageId == R.id.tasks_root) {
            title.setText(R.string.tasks_title);
            add.setVisibility(View.INVISIBLE); settings.setVisibility(View.GONE); filter.setVisibility(View.GONE);
            hideTip.run();
            return;
        }
        title.setText(R.string.header_logo);
        add.setVisibility(View.VISIBLE);
        settings.setVisibility(View.VISIBLE);
        filter.setVisibility(View.GONE);
        updateWalletTip();
        applyDonorBadge(Store.isDonor(this));
    }

    private void openHistory() {
        if (currentAddress == null) {
            Toast.makeText(this, R.string.save_address_first, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        tabBar.setSelectedIndex(1, true);
        slidePager.setCurrentPage(1, true);
        applyHeader(1);
        tabPages.ensure(1);
    }

    private void openTokens() {
        if (currentAddress == null) {
            Toast.makeText(this, R.string.save_address_first, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        tabBar.setSelectedIndex(2, true);
        slidePager.setCurrentPage(2, true);
        applyHeader(2);
        tabPages.ensure(2);
    }

    private void openNfts() {
        if (currentAddress == null) {
            Toast.makeText(this, R.string.save_address_first, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        tabBar.setSelectedIndex(3, true);
        slidePager.setCurrentPage(3, true);
        applyHeader(3);
        tabPages.ensure(3);
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
        String versionName = "";
        long code = 0L;
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = info.versionName == null ? "" : info.versionName;
            code = android.os.Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
            versionView.setText(getString(R.string.version_label, versionName, code));
        } catch (Exception ignored) {versionView.setText("");}
        checkForUpdate(versionName);
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
        final PathInterpolator ease = new PathInterpolator(.22f,1f,.36f,1f);
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
        final PathInterpolator ease = new PathInterpolator(.22f,1f,.36f,1f);
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
        int side = Math.round(Math.min(getResources().getDisplayMetrics().widthPixels * .72f, getResources().getDisplayMetrics().heightPixels * .38f));
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
        view.setShadowLayer(.01f,.05f * em, .05f * em, 0xFF000000);
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

    private void checkForUpdate(final String currentVersion) {
        final long mine = Api.versionRank(currentVersion);
        new Thread(() -> {
            try {
                Api.AppRelease release = Api.fetchLatestRelease();
                if (release == null || release.apkUrl.isEmpty()) {return;}
                if (Api.releaseRank(release) <= mine) {return;}
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
            updateBanner.animate().translationY(0f).alpha(1f).setDuration(420).setInterpolator(new PathInterpolator(.22f,1f,.36f,1f)).start();
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
        final PathInterpolator ease = new PathInterpolator(.22f,1f,.36f,1f);
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
            sheet.animate().alpha(0f).translationY(Math.round(24 * density)).setDuration(240).setInterpolator(ease).withEndAction(() -> {try {if (dialog.isShowing()) {dialog.dismiss();}} catch (Exception ignored) {}}).start();
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

    private void refresh(final String address) {
        boolean showSpinner = Store.loadCache(this, address) == null;
        if (showSpinner) {cardView.setLoading(true);}
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
                    syncExtraPages(false);
                }
            });
            GramWidgetProvider.renderAll(appContext);
            runOnUiThread(() -> maybePreloadGifts(address, cardDecor));
        }, "gram-app-refresh").start();
    }

    private void applyDonorBadge(boolean donor) {if (headerDonor != null) {headerDonor.setVisibility(donor ? View.VISIBLE : View.GONE);}}

    private void showDonorClaim() {
        if (currentAddress == null) {
            Toast.makeText(this, R.string.donor_need_address, Toast.LENGTH_SHORT).show();
            return;
        }
        final float density = getResources().getDisplayMetrics().density;
        final PathInterpolator ease = new PathInterpolator(.22f,1f,.36f,1f);
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
            window.setLayout(Math.round(getResources().getDisplayMetrics().widthPixels * .92f), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        if (!ReceiveActivity.bindReceiveViews(this, dialog.findViewById(R.id.receive_panel), address)) {return;}
        dialog.findViewById(R.id.receive_close).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void pinWidget() {
        if (currentAddress == null) {
            Toast.makeText(this, R.string.save_address_first, Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SettingsActivity.class));
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



    private void setChartDays(int days) {
        chartDays = days;
        Store.setChartDays(this, days);
        refreshChartChips();
        loadChart(false);
    }

    private void refreshChartChips() {
        if (chart7 == null) {return;}
        chart7.setTextColor(chartDays == 7 ? 0xFFFFFFFF : 0x99FFFFFF);
        chart30.setTextColor(chartDays == 30 ? 0xFFFFFFFF : 0x99FFFFFF);
        chart90.setTextColor(chartDays == 90 ? 0xFFFFFFFF : 0x99FFFFFF);
        chart7.setTypeface(null, chartDays == 7 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        chart30.setTypeface(null, chartDays == 30 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        chart90.setTypeface(null, chartDays == 90 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void loadChart(boolean memOnly) {
        if (balanceChart == null) {return;}
        if (currentAddress == null) {balanceChart.setValues(new double[0]); return;}
        final String address = currentAddress;
        final int days = chartDays;
        String key = address + ":" + days;
        double[] mem = chartMem.get(key);
        if (mem != null) {balanceChart.setValues(mem); if (memOnly || Store.isFresh(this, address)) {return;}}
        final int seq = ++chartSeq;
        new Thread(() -> {
            double[] values = new double[0];
            try {values = Api.fetchBalanceHistory(address, days);} catch (Exception ignored) {}
            final double[] out = values;
            runOnUiThread(() -> {
                if (isFinishing() || balanceChart == null || seq != chartSeq) {return;}
                if (!address.equals(currentAddress)) {return;}
                chartMem.put(key, out);
                balanceChart.setValues(out);
            });
        }, "gram-chart").start();
    }

    private void loadToday(boolean memOnly) {
        if (currentAddress == null) {
            todayList.removeAllViews();
            todayEmpty.setVisibility(View.VISIBLE);
            todayEmpty.setText(R.string.enter_address);
            return;
        }
        final String address = currentAddress;
        java.util.List<Tx> mem = todayMem.get(address);
        if (mem != null) {
            bindTodayRows(mem);
            if (memOnly || Store.isFresh(this, address)) {return;}
        }
        final int seq = ++todaySeq;
        final boolean hideSpam = Store.isHideSpam(this);
        new Thread(() -> {
            java.util.List<Tx> today = new java.util.ArrayList<>();
            try {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long minTime = cal.getTimeInMillis() / 1000L;
                String cursor = null;
                for (int page = 0; page < 4 && today.size() < 4; page++) {
                    Api.ActionsPage result = Api.fetchActions(address, cursor);
                    if (result == null || result.items == null || result.items.isEmpty()) {break;}
                    boolean older = false;
                    for (Tx tx : result.items) {
                        if (tx.time < minTime) {older = true; break;}
                        if (hideSpam && tx.isSpam()) {continue;}
                        today.add(tx);
                        if (today.size() >= 4) {break;}
                    }
                    if (older || result.next == null || result.next.isEmpty()) {break;}
                    cursor = result.next;
                }
            } catch (Exception ignored) {}
            final java.util.List<Tx> rows = today;
            runOnUiThread(() -> {
                if (isFinishing() || seq != todaySeq || !address.equals(currentAddress)) {return;}
                todayMem.put(address, rows);
                bindTodayRows(rows);
            });
        }, "gram-today").start();
    }

    private String todayFp = "";

    private void bindTodayRows(java.util.List<Tx> rows) {
        String fp = todayFingerprint(rows);
        if (fp.equals(todayFp) && todayList.getChildCount() == (rows == null ? 0 : rows.size())) {return;}
        todayFp = fp;
        if (rows == null || rows.isEmpty()) {
            todayList.removeAllViews();
            todayEmpty.setVisibility(View.VISIBLE);
            todayEmpty.setText(R.string.today_empty);
            return;
        }
        todayEmpty.setVisibility(View.GONE);
        float density = getResources().getDisplayMetrics().density;
        int gap = Math.round(8 * density);
        android.widget.LinearLayout next = new android.widget.LinearLayout(this);
        next.setOrientation(android.widget.LinearLayout.VERTICAL);
        next.setLayoutParams(new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        for (int i = 0; i < rows.size(); i++) {
            Tx tx = rows.get(i);
            View row = getLayoutInflater().inflate(R.layout.row_tx, next, false);
            bindTodayRow(row, tx);
            if (i > 0) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = gap;
                row.setLayoutParams(lp);
            }
            row.setOnClickListener(v -> TxDetail.show(this, tx));
            next.addView(row);
        }
        todayList.removeAllViews();
        for (int i = 0; i < next.getChildCount(); ) {
            View child = next.getChildAt(0);
            next.removeViewAt(0);
            todayList.addView(child);
        }
    }

    private static String todayFingerprint(java.util.List<Tx> rows) {
        if (rows == null || rows.isEmpty()) {return "";}
        StringBuilder sb = new StringBuilder(rows.size() * 24);
        for (Tx tx : rows) {sb.append(tx.id).append('|').append(tx.time).append(';');}
        return sb.toString();
    }

    private void bindTodayRow(View view, Tx tx) {
        TextView badge = view.findViewById(R.id.tx_badge);
        ImageView arrow = view.findViewById(R.id.tx_arrow);
        ImageView image = view.findViewById(R.id.tx_image);
        TextView title = view.findViewById(R.id.tx_title);
        TextView subtitle = view.findViewById(R.id.tx_subtitle);
        TextView comment = view.findViewById(R.id.tx_comment);
        TextView time = view.findViewById(R.id.tx_time);
        TextView amount = view.findViewById(R.id.tx_amount);
        comment.setVisibility(View.GONE);
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        if (tx.isNft()) {
            arrow.setVisibility(View.GONE);
            if (tx.image.isEmpty()) {
                badge.setText("NFT");
                badge.setVisibility(View.VISIBLE);
                image.setVisibility(View.GONE);
            } else {
                badge.setVisibility(View.GONE);
                image.setVisibility(View.VISIBLE);
                ImageLoader.load(this, image, tx.image);
            }
            title.setText(tx.nftName.isEmpty() ? getString(R.string.tx_nft) : tx.nftName);
            subtitle.setText(tx.collection.isEmpty() ? getString(tx.incoming ? R.string.tx_nft_received : R.string.tx_nft_sent) : tx.collection);
            amount.setText(tx.incoming ? "+ NFT" : "− NFT");
        } else if (Tx.TYPE_TON.equals(tx.type)) {
            badge.setVisibility(View.GONE);
            image.setVisibility(View.GONE);
            arrow.setVisibility(View.VISIBLE);
            arrow.setImageResource(tx.incoming ? R.drawable.ic_tx_in : R.drawable.ic_tx_out);
            title.setText(getString(tx.incoming ? R.string.tx_received : R.string.tx_sent));
            subtitle.setText(shortTodayAddress(tx.counterparty));
            String sign = tx.incoming ? "+ " : "− ";
            amount.setText(sign + Format.shortGram(tx.gram) + " GRAM");
        } else {
            badge.setText("···");
            badge.setVisibility(View.VISIBLE);
            image.setVisibility(View.GONE);
            arrow.setVisibility(View.GONE);
            title.setText(getString(R.string.tx_contract));
            subtitle.setText(shortTodayAddress(tx.counterparty));
            if (tx.gram > 0) {
                String sign = tx.incoming ? "+ " : "− ";
                amount.setText(sign + Format.shortGram(tx.gram) + " GRAM");
            } else {amount.setText("—");}
        }
        amount.setTextColor(0xFFFFFFFF);
        time.setText(dateFormat.format(new Date(tx.time * 1000L)));
        if (!tx.success) {title.setText(getString(R.string.tx_failed));}
    }

    private String shortTodayAddress(String value) {
        if (value == null || value.isEmpty()) {return getString(R.string.tx_unknown);}
        if (value.indexOf('.') >= 0 || value.length() <= 16) {return value;}
        return value.substring(0, 6) + "…" + value.substring(value.length() - 4);
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
        final View tab = findViewById(R.id.tab_bar);
        final ViewGroup.MarginLayoutParams tabLp = tab == null ? null : (ViewGroup.MarginLayoutParams) tab.getLayoutParams();
        final int tabBaseBottom = tabLp == null ? 0 : tabLp.bottomMargin;
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
            view.setPadding(baseLeft + left, baseTop + top, baseRight + right, 0);
            if (tabLp != null) {
                tabLp.bottomMargin = tabBaseBottom + bottom;
                tab.setLayoutParams(tabLp);
            }
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
