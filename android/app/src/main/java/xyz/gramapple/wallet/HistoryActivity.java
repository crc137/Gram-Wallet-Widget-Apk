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
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HistoryActivity extends Activity {
    static final String EXTRA_TX_ID = "tx_id";
    static final String EXTRA_SPAM_AMOUNT = "spam_amount";
    static final String EXTRA_SPAM = "spam";
    private ListView listView;
    private TextView statusView;
    private TextView spamBanner;
    private ProgressBar progressView;
    private Button moreButton;
    private TextView filterButton;
    private TxAdapter adapter;
    private final List<Tx> allItems = new ArrayList<>();
    private String address;
    private String nextCursor;
    private boolean loading;
    private String dirFilter = "all";
    private String typeFilter = "all";
    private String periodFilter = "all";
    private boolean hideSpam = true;
    private String focusTxId = "";
    private boolean pendingSpamBanner;
    private double spamAmount;
    private int autoLoadPages;
    private static final int AUTO_TARGET = 12;
    private static final int AUTO_MAX_PAGES = 40;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupEdgeToEdge();
        setContentView(R.layout.activity_history);
        applySystemBarInsets(findViewById(R.id.history_root));
        listView = findViewById(R.id.history_list);
        statusView = findViewById(R.id.history_status);
        spamBanner = findViewById(R.id.history_spam_banner);
        progressView = findViewById(R.id.history_progress);
        filterButton = findViewById(R.id.history_filter);
        findViewById(R.id.history_back).setOnClickListener(view -> finish());
        filterButton.setOnClickListener(v -> showFilterSheet());
        moreButton = new Button(this);
        moreButton.setBackgroundResource(R.drawable.button_white);
        moreButton.setText(R.string.history_load_more);
        moreButton.setAllCaps(false);
        moreButton.setTextColor(getColor(R.color.gram_blue));
        moreButton.setTextSize(14f);
        moreButton.setMinHeight(0);
        moreButton.setMinWidth(0);
        moreButton.setStateListAnimator(null);
        int height = Math.round(48 * getResources().getDisplayMetrics().density);
        moreButton.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        moreButton.setVisibility(View.GONE);
        moreButton.setOnClickListener(view -> loadMore());
        listView.addFooterView(moreButton, null, false);
        adapter = new TxAdapter(this, new ArrayList<>());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= adapter.getCount()) {return;}
            showTxDetail(adapter.getItem(position));
        });
        dirFilter = Store.getHistDir(this);
        typeFilter = Store.getHistType(this);
        periodFilter = Store.getHistPeriod(this);
        hideSpam = Store.isHideSpam(this);
        if ("day".equals(periodFilter)) {periodFilter = "all";}
        refreshFilterButton();
        readIntent(getIntent());
        address = Store.getAddress(this);
        if (address == null) {
            Toast.makeText(this, R.string.save_address_first, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        load(null, false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readIntent(intent);
        applyFilters(true);
    }

    private void readIntent(Intent intent) {
        if (intent == null) {return;}
        focusTxId = intent.getStringExtra(EXTRA_TX_ID);
        if (focusTxId == null) {focusTxId = "";}
        pendingSpamBanner = intent.getBooleanExtra(EXTRA_SPAM, false);
        spamAmount = intent.getDoubleExtra(EXTRA_SPAM_AMOUNT, 0d);
        if (pendingSpamBanner) {showSpamBanner(spamAmount);}
    }

    private void showSpamBanner(double amount) {
        spamBanner.setVisibility(View.VISIBLE);
        spamBanner.setText(getString(R.string.history_spam_banner, Format.shortGram(Math.max(0d, amount))));
    }

    private void refreshFilterButton() {
        boolean active = !"all".equals(dirFilter) || !"all".equals(typeFilter) || !"all".equals(periodFilter) || hideSpam;
        filterButton.setTextColor(getColor(active ? R.color.white : R.color.white));
        filterButton.setAlpha(active ? 1f : 0.85f);
    }

    private void showFilterSheet() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_history_filter);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(android.view.Gravity.BOTTOM);
        }
        final String[] draftDir = {dirFilter};
        final String[] draftType = {typeFilter};
        final String[] draftPeriod = {periodFilter};
        final boolean[] draftSpam = {hideSpam};
        final TextView dirAll = dialog.findViewById(R.id.filter_dir_all);
        final TextView dirIn = dialog.findViewById(R.id.filter_dir_in);
        final TextView dirOut = dialog.findViewById(R.id.filter_dir_out);
        final Switch spamSwitch = dialog.findViewById(R.id.filter_hide_spam);
        spamSwitch.setChecked(hideSpam);
        styleSpamSwitch(spamSwitch);
        spamSwitch.setOnCheckedChangeListener((b, checked) -> {
            draftSpam[0] = checked;
            styleSpamSwitch(spamSwitch);
        });
        final View[] typeRows = {
                dialog.findViewById(R.id.filter_type_all), dialog.findViewById(R.id.filter_type_ton),
                dialog.findViewById(R.id.filter_type_token), dialog.findViewById(R.id.filter_type_nft),
                dialog.findViewById(R.id.filter_type_swap), dialog.findViewById(R.id.filter_type_stake),
                dialog.findViewById(R.id.filter_type_domain), dialog.findViewById(R.id.filter_type_contract)
        };
        final String[] typeKeys = {"all", "ton", "token", "nft", "swap", "stake", "domain", "contract"};
        final int[] typeLabels = {
                R.string.filter_type_all, R.string.filter_type_ton, R.string.filter_type_token, R.string.filter_type_nft,
                R.string.filter_type_swap, R.string.filter_type_stake, R.string.filter_type_domain, R.string.filter_type_contract
        };
        final View[] periodRows = {
                dialog.findViewById(R.id.filter_period_all), dialog.findViewById(R.id.filter_period_week),
                dialog.findViewById(R.id.filter_period_month), dialog.findViewById(R.id.filter_period_quarter),
                dialog.findViewById(R.id.filter_period_year)
        };
        final String[] periodKeys = {"all", "week", "month", "quarter", "year"};
        final int[] periodLabels = {
                R.string.filter_period_all, R.string.filter_period_week, R.string.filter_period_month,
                R.string.filter_period_quarter, R.string.filter_period_year
        };
        Runnable paint = () -> {
            styleDirSeg(dirAll, "all".equals(draftDir[0]));
            styleDirSeg(dirIn, "in".equals(draftDir[0]));
            styleDirSeg(dirOut, "out".equals(draftDir[0]));
            for (int i = 0; i < typeRows.length; i++) {bindOptionRow(typeRows[i], getString(typeLabels[i]), typeKeys[i].equals(draftType[0]));}
            for (int i = 0; i < periodRows.length; i++) {bindOptionRow(periodRows[i], getString(periodLabels[i]), periodKeys[i].equals(draftPeriod[0]));}
        };
        dirAll.setOnClickListener(v -> {draftDir[0] = "all"; paint.run();});
        dirIn.setOnClickListener(v -> {draftDir[0] = "in"; paint.run();});
        dirOut.setOnClickListener(v -> {draftDir[0] = "out"; paint.run();});
        for (int i = 0; i < typeRows.length; i++) {
            final String key = typeKeys[i];
            typeRows[i].setOnClickListener(v -> {draftType[0] = key; paint.run();});
        }
        for (int i = 0; i < periodRows.length; i++) {
            final String key = periodKeys[i];
            periodRows[i].setOnClickListener(v -> {draftPeriod[0] = key; paint.run();});
        }
        dialog.findViewById(R.id.filter_reset).setOnClickListener(v -> {
            draftDir[0] = "all";
            draftType[0] = "all";
            draftPeriod[0] = "all";
            draftSpam[0] = true;
            spamSwitch.setChecked(true);
            styleSpamSwitch(spamSwitch);
            paint.run();
        });
        dialog.findViewById(R.id.filter_done).setOnClickListener(v -> {
            dirFilter = draftDir[0];
            typeFilter = draftType[0];
            periodFilter = draftPeriod[0];
            hideSpam = draftSpam[0];
            Store.setHistDir(this, dirFilter);
            Store.setHistType(this, typeFilter);
            Store.setHistPeriod(this, periodFilter);
            Store.setHideSpam(this, hideSpam);
            refreshFilterButton();
            autoLoadPages = 0;
            applyFilters(false);
            dialog.dismiss();
        });
        paint.run();
        dialog.show();
    }

    private void styleDirSeg(TextView chip, boolean on) {
        if (on) {chip.setBackgroundResource(R.drawable.filter_seg_on);}
        else {chip.setBackgroundColor(Color.TRANSPARENT);}
        chip.setTypeface(null, on ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void styleSpamSwitch(Switch spamSwitch) {
        int on = 0xFF0088FF;
        int off = 0xFFC7C7CC;
        if (Build.VERSION.SDK_INT >= 23) {
            spamSwitch.getThumbDrawable().setTint(spamSwitch.isChecked() ? on : 0xFFF4F4F5);
            spamSwitch.getTrackDrawable().setTint(spamSwitch.isChecked() ? 0x990088FF : off);
        }
    }

    private void bindOptionRow(View row, String label, boolean selected) {
        TextView text = row.findViewById(R.id.filter_option_label);
        ImageView check = row.findViewById(R.id.filter_option_check);
        text.setText(label);
        check.setVisibility(selected ? View.VISIBLE : View.GONE);
    }

    private void applyFilters(boolean openFocus) {
        List<Tx> visible = filteredItems();
        adapter.replaceAll(visible);
        if (shouldAutoLoadMore(visible.size())) {
            if (visible.isEmpty()) {
                statusView.setVisibility(View.GONE);
                progressView.setVisibility(View.VISIBLE);
            }
            autoLoadPages += 1;
            load(nextCursor, true);
            return;
        }
        progressView.setVisibility(View.GONE);
        moreButton.setVisibility(nextCursor == null ? View.GONE : View.VISIBLE);
        if (adapter.getCount() == 0 && !loading) {
            statusView.setText(R.string.history_empty);
            statusView.setVisibility(View.VISIBLE);
        } else if (adapter.getCount() > 0) {statusView.setVisibility(View.GONE);}
        if (openFocus || (focusTxId != null && !focusTxId.isEmpty())) {maybeOpenFocus();}
    }

    private List<Tx> filteredItems() {
        List<Tx> visible = new ArrayList<>();
        long minTime = periodMinTime(periodFilter);
        for (Tx tx : allItems) {
            if (hideSpam && tx.isSpam()) {continue;}
            if ("in".equals(dirFilter) && !tx.incoming) {continue;}
            if ("out".equals(dirFilter) && tx.incoming) {continue;}
            if (!tx.matchesTypeFilter(typeFilter)) {continue;}
            if (minTime > 0 && tx.time < minTime) {continue;}
            visible.add(tx);
        }
        return visible;
    }

    private boolean shouldAutoLoadMore(int visibleCount) {
        if (loading || nextCursor == null || autoLoadPages >= AUTO_MAX_PAGES) {return false;}
        if (visibleCount >= AUTO_TARGET) {return false;}
        return true;
    }

    private void maybeOpenFocus() {
        if (pendingSpamBanner) {
            showSpamBanner(spamAmount);
            focusTxId = "";
            return;
        }
        if (focusTxId == null || focusTxId.isEmpty()) {return;}
        for (int i = 0; i < adapter.getCount(); i++) {
            Tx tx = adapter.getItem(i);
            if (focusTxId.equals(tx.id)) {
                listView.setSelection(i);
                showTxDetail(tx);
                focusTxId = "";
                return;
            }
        }
        for (Tx tx : allItems) {
            if (!focusTxId.equals(tx.id)) {continue;}
            if (tx.isSpam()) {
                showSpamBanner(tx.gram);
                pendingSpamBanner = true;
                spamAmount = tx.gram;
            } else {showTxDetail(tx);}
            focusTxId = "";
            return;
        }
    }

    private static long periodMinTime(String period) {
        if (period == null || "all".equals(period)) {return 0L;}
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if ("week".equals(period)) {cal.add(Calendar.DAY_OF_YEAR, -7); return cal.getTimeInMillis() / 1000L;}
        if ("month".equals(period)) {cal.add(Calendar.DAY_OF_YEAR, -30); return cal.getTimeInMillis() / 1000L;}
        if ("quarter".equals(period)) {cal.add(Calendar.DAY_OF_YEAR, -90); return cal.getTimeInMillis() / 1000L;}
        if ("year".equals(period)) {cal.add(Calendar.DAY_OF_YEAR, -365); return cal.getTimeInMillis() / 1000L;}
        return 0L;
    }

    private void loadMore() {if (nextCursor != null) {load(nextCursor, true);}}

    private void showTxDetail(Tx tx) {
        if (tx == null) {return;}
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_tx_detail);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(Math.round(getResources().getDisplayMetrics().widthPixels * 0.9f), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        View gem = dialog.findViewById(R.id.tx_detail_gem);
        TextView amount = dialog.findViewById(R.id.tx_detail_amount);
        TextView usd = dialog.findViewById(R.id.tx_detail_usd);
        TextView partyLabel = dialog.findViewById(R.id.tx_detail_party_label);
        TextView party = dialog.findViewById(R.id.tx_detail_party);
        TextView addressView = dialog.findViewById(R.id.tx_detail_address);
        TextView comment = dialog.findViewById(R.id.tx_detail_comment);
        TextView date = dialog.findViewById(R.id.tx_detail_date);
        partyLabel.setText(tx.incoming ? R.string.tx_detail_sender : R.string.tx_detail_recipient);
        if (tx.isNft()) {
            gem.setVisibility(View.GONE);
            amount.setText(tx.incoming ? "+ NFT" : "− NFT");
            amount.setTextColor(tx.incoming ? 0xFF34C759 : 0xFF111111);
            usd.setText(tx.nftName.isEmpty() ? getString(R.string.tx_nft) : tx.nftName);
            party.setText(tx.collection.isEmpty() ? getString(R.string.tx_detail_dash) : tx.collection);
            addressView.setText(getString(R.string.tx_detail_dash));
            comment.setText(getString(R.string.tx_detail_dash));
        } else {
            gem.setVisibility(View.VISIBLE);
            String sign = tx.incoming ? "+" : "−";
            String grams = Format.shortGram(tx.gram);
            amount.setText(gramAmountSpan(sign + grams, !tx.success), TextView.BufferType.SPANNABLE);
            WalletData data = Store.load(this);
            if (data != null && data.hasUsd && data.gram > 0) {
                double approx = tx.gram * (data.totalUsd / data.gram);
                usd.setText(getString(R.string.tx_detail_usd, Format.shortGram(approx)));
            } else {usd.setText(R.string.tx_detail_dash);}
            String who = tx.counterparty == null || tx.counterparty.isEmpty() ? getString(R.string.tx_unknown) : tx.counterparty;
            party.setText(who.indexOf('.') >= 0 || who.length() <= 18 ? who : who.substring(0, 6) + "…" + who.substring(who.length() - 4));
            addressView.setText(Format.chunkAddress(who).replace('\n', ' '));
            String note = tx.comment == null ? "" : tx.comment.trim().replace('\n', ' ');
            if (note.length() > 120) {note = note.substring(0, 117) + "…";}
            comment.setText(note.isEmpty() ? getString(R.string.tx_detail_dash) : note);
            comment.setMaxLines(3);
        }
        date.setText(new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(new Date(tx.time * 1000L)));
        Runnable dismiss = () -> {try {if (dialog.isShowing()) {dialog.dismiss();}} catch (Exception ignored) {}};
        dialog.findViewById(R.id.tx_detail_close).setOnClickListener(v -> dismiss.run());
        dialog.findViewById(R.id.tx_detail_ok).setOnClickListener(v -> dismiss.run());
        dialog.show();
    }

    private CharSequence gramAmountSpan(String value, boolean failed) {
        String label = " Grams";
        SpannableString text = new SpannableString(value + label);
        int numColor = failed ? 0xFFFF3B30 : 0xFF34C759;
        text.setSpan(new ForegroundColorSpan(numColor), 0, value.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new ForegroundColorSpan(0xFF0088FF), value.length(), text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }

    private void load(final String cursor, final boolean append) {
        if (loading) {return;}
        loading = true;
        if (!append) {
            statusView.setVisibility(View.GONE);
            progressView.setVisibility(View.VISIBLE);
            allItems.clear();
            autoLoadPages = 0;
        }
        moreButton.setVisibility(View.GONE);
        final Context appContext = getApplicationContext();
        new Thread(() -> {
            try {
                Api.ActionsPage page = Api.fetchActions(address, cursor);
                runOnUiThread(() -> {
                    if (isFinishing()) {return;}
                    loading = false;
                    if (!append) {allItems.clear();}
                    allItems.addAll(page.items);
                    nextCursor = page.next;
                    applyFilters(true);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing()) {return;}
                    loading = false;
                    progressView.setVisibility(View.GONE);
                    if (adapter.getCount() == 0) {
                        statusView.setText(R.string.history_error);
                        statusView.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(appContext, R.string.history_error, Toast.LENGTH_SHORT).show();
                        moreButton.setVisibility(nextCursor == null ? View.GONE : View.VISIBLE);
                    }
                });
            }
        }, "gram-history").start();
    }

    private static final class TxAdapter extends BaseAdapter {
        private final Activity activity;
        private final List<Tx> items;
        private final Set<String> revealed = new HashSet<>();
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM, HH:mm", Locale.getDefault());

        TxAdapter(Activity activity, List<Tx> items) {
            this.activity = activity;
            this.items = items;
        }

        void replaceAll(List<Tx> next) {
            items.clear();
            items.addAll(next);
            notifyDataSetChanged();
        }

        @Override public int getCount() {return items.size();}
        @Override public Tx getItem(int position) {return items.get(position);}
        @Override public long getItemId(int position) {return position;}
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {view = activity.getLayoutInflater().inflate(R.layout.row_tx, parent, false);}
            Tx tx = getItem(position);
            TextView badge = view.findViewById(R.id.tx_badge);
            ImageView arrow = view.findViewById(R.id.tx_arrow);
            ImageView image = view.findViewById(R.id.tx_image);
            TextView title = view.findViewById(R.id.tx_title);
            TextView subtitle = view.findViewById(R.id.tx_subtitle);
            TextView comment = view.findViewById(R.id.tx_comment);
            TextView time = view.findViewById(R.id.tx_time);
            TextView amount = view.findViewById(R.id.tx_amount);

            if (tx.isNft()) {
                arrow.setVisibility(View.GONE);
                if (tx.image.isEmpty()) {
                    badge.setText("NFT");
                    badge.setVisibility(View.VISIBLE);
                    image.setVisibility(View.GONE);
                    ImageLoader.load(activity, image, "");
                } else {
                    badge.setVisibility(View.GONE);
                    image.setVisibility(View.VISIBLE);
                    ImageLoader.load(activity, image, tx.image);
                }
                title.setText(tx.nftName.isEmpty() ? activity.getString(R.string.tx_nft) : tx.nftName);
                subtitle.setText(tx.collection.isEmpty() ? activity.getString(tx.incoming ? R.string.tx_nft_received : R.string.tx_nft_sent) : tx.collection);
                amount.setText(tx.incoming ? "+ NFT" : "− NFT");
                amount.setTextColor(0xFFFFFFFF);
                clearCommentBlur(comment);
                comment.setOnClickListener(null);
                comment.setClickable(false);
                comment.setVisibility(View.GONE);
            } else if (Tx.TYPE_TON.equals(tx.type)) {
                badge.setVisibility(View.GONE);
                image.setVisibility(View.GONE);
                ImageLoader.load(activity, image, "");
                arrow.setVisibility(View.VISIBLE);
                arrow.setImageResource(tx.incoming ? R.drawable.ic_tx_in : R.drawable.ic_tx_out);
                arrow.setContentDescription(activity.getString(tx.incoming ? R.string.tx_received : R.string.tx_sent));
                title.setText(activity.getString(tx.incoming ? R.string.tx_received : R.string.tx_sent));
                subtitle.setText(shortAddress(tx.counterparty));
                String text = tx.comment.replace('\n', ' ').replace('\r', ' ').trim();
                if (text.length() > 160) {text = text.substring(0, 160) + "…";}
                bindComment(comment, tx, text);
                String sign = tx.incoming ? "+ " : "− ";
                amount.setText(sign + Format.shortGram(tx.gram) + " GRAM");
                amount.setTextColor(0xFFFFFFFF);
            } else {
                badge.setText("···");
                badge.setVisibility(View.VISIBLE);
                image.setVisibility(View.GONE);
                ImageLoader.load(activity, image, "");
                arrow.setVisibility(View.GONE);
                title.setText(activity.getString(R.string.tx_contract));
                subtitle.setText(shortAddress(tx.counterparty));
                clearCommentBlur(comment);
                comment.setOnClickListener(null);
                comment.setClickable(false);
                comment.setVisibility(View.GONE);
                if (tx.gram > 0) {
                    String sign = tx.incoming ? "+ " : "− ";
                    amount.setText(sign + Format.shortGram(tx.gram) + " GRAM");
                } else {amount.setText("—");}
                amount.setTextColor(0xFFFFFFFF);
            }
            time.setText(dateFormat.format(new Date(tx.time * 1000L)));
            if (!tx.success) {title.setText(activity.getString(R.string.tx_failed));}
            return view;
        }

        private void bindComment(TextView comment, Tx tx, String text) {
            if (text.isEmpty()) {
                clearCommentBlur(comment);
                comment.setOnClickListener(null);
                comment.setClickable(false);
                comment.setVisibility(View.GONE);
                return;
            }
            comment.setVisibility(View.VISIBLE);
            boolean hidden = tx.hasHiddenComment() && !revealed.contains(tx.id);
            if (hidden) {
                comment.setText(text);
                applyCommentBlur(comment);
                comment.setClickable(true);
                comment.setOnClickListener(v -> {
                    revealed.add(tx.id);
                    notifyDataSetChanged();
                });
            } else {
                clearCommentBlur(comment);
                comment.setText(text);
                comment.setOnClickListener(null);
                comment.setClickable(false);
            }
        }

        private void applyCommentBlur(TextView comment) {
            if (Build.VERSION.SDK_INT >= 31) {comment.setRenderEffect(RenderEffect.createBlurEffect(14f, 14f, Shader.TileMode.CLAMP));} else {
                comment.setText(R.string.tx_spam_hidden);
                comment.setAlpha(0.7f);
            }
        }

        private void clearCommentBlur(TextView comment) {
            if (Build.VERSION.SDK_INT >= 31) {comment.setRenderEffect(null);}
            comment.setAlpha(1f);
        }

        private String shortAddress(String value) {
            if (value == null || value.isEmpty()) {return activity.getString(R.string.tx_unknown);}
            if (value.indexOf('.') >= 0 || value.length() <= 16) {return value;}
            return value.substring(0, 6) + "…" + value.substring(value.length() - 4);
        }
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
}
