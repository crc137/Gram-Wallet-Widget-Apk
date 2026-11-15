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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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

final class TabPages {
    private final Activity activity;
    private final View historyRoot;
    private final View tokensRoot;
    private final View nftsRoot;
    private boolean historyReady;
    private boolean tokensReady;
    private boolean nftsReady;
    private String boundAddress;
    private ListView historyList;
    private TextView historyStatus;
    private ProgressBar historyProgress;
    private Button historyMore;
    private HistoryAdapter historyAdapter;
    private final List<Tx> historyAll = new ArrayList<>();
    private String historyCursor;
    private boolean historyLoading;
    private String dirFilter = "all";
    private String typeFilter = "all";
    private String periodFilter = "all";
    private boolean hideSpam = true;
    private ListView tokensList;
    private TextView tokensStatus;
    private ProgressBar tokensProgress;
    private Button tokensMore;
    private TokenAdapter tokensAdapter;
    private String tokensCursor;
    private boolean tokensLoading;
    private ListView nftsList;
    private TextView nftsStatus;
    private ProgressBar nftsProgress;
    private Button nftsMore;
    private NftAdapter nftsAdapter;
    private String nftsCursor;
    private boolean nftsLoading;

    TabPages(Activity activity, View historyRoot, View tokensRoot, View nftsRoot) {
        this.activity = activity;
        this.historyRoot = historyRoot;
        this.tokensRoot = tokensRoot;
        this.nftsRoot = nftsRoot;
        stripEmbeddedChrome(historyRoot, R.id.history_back, R.id.history_filter);
        stripEmbeddedChrome(tokensRoot, R.id.tokens_back, 0);
        stripEmbeddedChrome(nftsRoot, R.id.nfts_back, 0);
    }

    private static void stripEmbeddedChrome(View root, int backId, int filterId) {
        View back = root.findViewById(backId);
        if (back != null) {
            View parent = back.getParent() instanceof View ? (View) back.getParent() : null;
            if (parent != null) {parent.setVisibility(View.GONE);}
            else {back.setVisibility(View.GONE);}
        }
        if (filterId != 0) {
            View filter = root.findViewById(filterId);
            if (filter != null) {
                Object p = filter.getParent();
                if (p instanceof View) {((View) p).setVisibility(View.GONE);}
                else {filter.setVisibility(View.GONE);}
            }
        }
        int pad = Math.round(8 * root.getResources().getDisplayMetrics().density);
        root.setPadding(root.getPaddingLeft(), pad, root.getPaddingRight(), root.getPaddingBottom());
    }

    void openHistoryFilter() {
        if (!historyReady) {
            String address = Store.getAddress(activity);
            if (address == null) {return;}
            ensureHistory(address);
        }
        showFilterSheet();
    }

    void ensure(int page) {
        String address = Store.getAddress(activity);
        if (address == null) {
            Toast.makeText(activity, R.string.save_address_first, Toast.LENGTH_SHORT).show();
            return;
        }
        if (boundAddress != null && !boundAddress.equals(address)) {
            historyReady = tokensReady = nftsReady = false;
            historyAll.clear();
        }
        boundAddress = address;
        if (page == 1) {ensureHistory(address);}
        else if (page == 2) {ensureTokens(address);}
        else if (page == 3) {ensureNfts(address);}
    }

    void reloadIfAddressChanged() {
        String address = Store.getAddress(activity);
        if (address == null) {return;}
        if (boundAddress != null && boundAddress.equals(address)) {return;}
        historyReady = tokensReady = nftsReady = false;
        historyAll.clear();
        boundAddress = address;
    }

    private void ensureHistory(String address) {
        if (!historyReady) {
            historyList = historyRoot.findViewById(R.id.history_list);
            historyStatus = historyRoot.findViewById(R.id.history_status);
            historyProgress = historyRoot.findViewById(R.id.history_progress);
            TextView filter = historyRoot.findViewById(R.id.history_filter);
            filter.setOnClickListener(v -> showFilterSheet());
            if (historyMore == null) {
                historyMore = makeMore(R.string.history_load_more, v -> {if (historyCursor != null) {loadHistory(historyCursor, true);}});
                historyList.addFooterView(historyMore, null, false);
                historyAdapter = new HistoryAdapter(activity, new ArrayList<>());
                historyList.setAdapter(historyAdapter);
                historyList.setOnItemClickListener((p, v, pos, id) -> {
                    if (pos >= 0 && pos < historyAdapter.getCount()) {showTxDetail(historyAdapter.getItem(pos));}
                });
            }
            dirFilter = Store.getHistDir(activity);
            typeFilter = Store.getHistType(activity);
            periodFilter = Store.getHistPeriod(activity);
            hideSpam = Store.isHideSpam(activity);
            if ("day".equals(periodFilter)) {periodFilter = "all";}
            historyReady = true;
            loadHistory(null, false);
        } else if (historyAdapter.getCount() == 0 && !historyLoading) {loadHistory(null, false);}
    }

    private void ensureTokens(String address) {
        if (!tokensReady) {
            tokensList = tokensRoot.findViewById(R.id.tokens_list);
            tokensStatus = tokensRoot.findViewById(R.id.tokens_status);
            tokensProgress = tokensRoot.findViewById(R.id.tokens_progress);
            if (tokensMore == null) {
                tokensMore = makeMore(R.string.tokens_load_more, v -> {if (tokensCursor != null) {loadTokens(tokensCursor, true);}});
                tokensList.addFooterView(tokensMore, null, false);
                tokensAdapter = new TokenAdapter(activity, new ArrayList<>());
                tokensList.setAdapter(tokensAdapter);
            }
            tokensReady = true;
            loadTokens(null, false);
        } else if (tokensAdapter.getCount() == 0 && !tokensLoading) {loadTokens(null, false);}
    }

    private void ensureNfts(String address) {
        if (!nftsReady) {
            nftsList = nftsRoot.findViewById(R.id.nfts_list);
            nftsStatus = nftsRoot.findViewById(R.id.nfts_status);
            nftsProgress = nftsRoot.findViewById(R.id.nfts_progress);
            if (nftsMore == null) {
                nftsMore = makeMore(R.string.nfts_load_more, v -> {if (nftsCursor != null) {loadNfts(nftsCursor, true);}});
                nftsList.addFooterView(nftsMore, null, false);
                nftsAdapter = new NftAdapter(activity, new ArrayList<>());
                nftsList.setAdapter(nftsAdapter);
                nftsList.setOnItemClickListener((p, v, pos, id) -> {
                    if (pos < 0 || pos >= nftsAdapter.getCount()) {return;}
                    openNft(nftsAdapter.getItem(pos));
                });
            }
            nftsReady = true;
            loadNfts(null, false);
        } else if (nftsAdapter.getCount() == 0 && !nftsLoading) {loadNfts(null, false);}
    }

    private void openNft(Nft nft) {
        if (nft == null) {return;}
        String url = nft.openUrl();
        if (url == null) {
            Toast.makeText(activity, R.string.nfts_open_fail, Toast.LENGTH_SHORT).show();
            return;
        }
        try {activity.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)));} catch (Exception e) {Toast.makeText(activity, R.string.nfts_open_fail, Toast.LENGTH_SHORT).show();}
    }

    private Button makeMore(int label, View.OnClickListener click) {
        Button b = new Button(activity);
        b.setBackgroundResource(R.drawable.button_white);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(activity.getColor(R.color.gram_blue));
        b.setTextSize(14f);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setStateListAnimator(null);
        int height = Math.round(48 * activity.getResources().getDisplayMetrics().density);
        b.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        b.setVisibility(View.GONE);
        b.setOnClickListener(click);
        return b;
    }

    private static void setMoreVisible(Button more, String cursor, int count) {
        if (more == null) {return;}
        more.setVisibility(cursor != null && count > 0 ? View.VISIBLE : View.GONE);
    }

    private void loadHistory(String cursor, boolean append) {
        if (historyLoading || boundAddress == null) {return;}
        historyLoading = true;
        if (!append) {historyStatus.setVisibility(View.GONE); historyProgress.setVisibility(View.VISIBLE);}
        historyMore.setVisibility(View.GONE);
        final String address = boundAddress;
        new Thread(() -> {
            try {
                Api.ActionsPage page = Api.fetchActions(address, cursor);
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing()) {return;}
                    historyLoading = false;
                    historyProgress.setVisibility(View.GONE);
                    if (!append) {historyAll.clear();}
                    historyAll.addAll(page.items);
                    historyCursor = page.next;
                    applyHistoryFilters();
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing()) {return;}
                    historyLoading = false;
                    historyProgress.setVisibility(View.GONE);
                    if (historyAdapter.getCount() == 0) {
                        historyStatus.setText(R.string.history_error);
                        historyStatus.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(activity, R.string.history_error, Toast.LENGTH_SHORT).show();
                        setMoreVisible(historyMore, historyCursor, historyAdapter.getCount());
                    }
                });
            }
        }, "gram-tab-history").start();
    }

    private void applyHistoryFilters() {
        List<Tx> visible = new ArrayList<>();
        long minTime = periodMinTime(periodFilter);
        for (Tx tx : historyAll) {
            if (hideSpam && tx.isSpam()) {continue;}
            if ("in".equals(dirFilter) && !tx.incoming) {continue;}
            if ("out".equals(dirFilter) && tx.incoming) {continue;}
            if (!tx.matchesTypeFilter(typeFilter)) {continue;}
            if (minTime > 0 && tx.time < minTime) {continue;}
            visible.add(tx);
        }
        historyAdapter.replaceAll(visible);
        setMoreVisible(historyMore, historyCursor, historyAdapter.getCount());
        if (historyAdapter.getCount() == 0) {
            historyStatus.setText(R.string.history_empty);
            historyStatus.setVisibility(View.VISIBLE);
        } else {historyStatus.setVisibility(View.GONE);}
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
        if ("year".equals(period)) {cal.add(Calendar.YEAR, -1); return cal.getTimeInMillis() / 1000L;}
        return cal.getTimeInMillis() / 1000L;
    }

    private void showFilterSheet() {
        Dialog dialog = new Dialog(activity);
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
        Switch spamSwitch = dialog.findViewById(R.id.filter_hide_spam);
        spamSwitch.setChecked(hideSpam);
        spamSwitch.setOnCheckedChangeListener((b, v) -> draftSpam[0] = v);
        dialog.findViewById(R.id.filter_reset).setOnClickListener(v -> {
            draftDir[0] = "all"; draftType[0] = "all"; draftPeriod[0] = "all"; draftSpam[0] = true;
            spamSwitch.setChecked(true);
        });
        dialog.findViewById(R.id.filter_done).setOnClickListener(v -> {
            dirFilter = draftDir[0]; typeFilter = draftType[0]; periodFilter = draftPeriod[0]; hideSpam = draftSpam[0];
            Store.setHistDir(activity, dirFilter);
            Store.setHistType(activity, typeFilter);
            Store.setHistPeriod(activity, periodFilter);
            Store.setHideSpam(activity, hideSpam);
            applyHistoryFilters();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showTxDetail(Tx tx) {TxDetail.show(activity, tx);}

    private void loadTokens(String cursor, boolean append) {
        if (tokensLoading || boundAddress == null) {return;}
        tokensLoading = true;
        if (!append) {tokensStatus.setVisibility(View.GONE); tokensProgress.setVisibility(View.VISIBLE);}
        tokensMore.setVisibility(View.GONE);
        final String address = boundAddress;
        new Thread(() -> {
            try {
                Api.TokensPage page = Api.fetchTokens(address, cursor);
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing()) {return;}
                    tokensLoading = false;
                    tokensProgress.setVisibility(View.GONE);
                    if (!append) {tokensAdapter.clear();}
                    tokensAdapter.addAll(page.items);
                    tokensCursor = page.next;
                    setMoreVisible(tokensMore, tokensCursor, tokensAdapter.getCount());
                    if (tokensAdapter.getCount() == 0) {
                        tokensStatus.setText(R.string.tokens_empty);
                        tokensStatus.setVisibility(View.VISIBLE);
                    } else {tokensStatus.setVisibility(View.GONE);}
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing()) {return;}
                    tokensLoading = false;
                    tokensProgress.setVisibility(View.GONE);
                    if (tokensAdapter.getCount() == 0) {
                        tokensStatus.setText(R.string.tokens_error);
                        tokensStatus.setVisibility(View.VISIBLE);
                        setMoreVisible(tokensMore, null, 0);
                    } else {
                        Toast.makeText(activity, R.string.tokens_error, Toast.LENGTH_SHORT).show();
                        setMoreVisible(tokensMore, tokensCursor, tokensAdapter.getCount());
                    }
                });
            }
        }, "gram-tab-tokens").start();
    }

    private void loadNfts(String cursor, boolean append) {
        if (nftsLoading || boundAddress == null) {return;}
        nftsLoading = true;
        if (!append) {nftsStatus.setVisibility(View.GONE); nftsProgress.setVisibility(View.VISIBLE);}
        nftsMore.setVisibility(View.GONE);
        final String address = boundAddress;
        new Thread(() -> {
            try {
                Api.NftsPage page = Api.fetchNfts(address, cursor);
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing()) {return;}
                    nftsLoading = false;
                    nftsProgress.setVisibility(View.GONE);
                    if (!append) {nftsAdapter.clear();}
                    nftsAdapter.addAll(page.items);
                    nftsCursor = page.next;
                    setMoreVisible(nftsMore, nftsCursor, nftsAdapter.getCount());
                    if (nftsAdapter.getCount() == 0) {
                        nftsStatus.setText(R.string.nfts_empty);
                        nftsStatus.setVisibility(View.VISIBLE);
                    } else {nftsStatus.setVisibility(View.GONE);}
                    if (nftsCursor != null && nftsAdapter.getCount() > 0) {loadNfts(nftsCursor, true);}
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing()) {return;}
                    nftsLoading = false;
                    nftsProgress.setVisibility(View.GONE);
                    if (nftsAdapter.getCount() == 0) {
                        nftsStatus.setText(R.string.nfts_error);
                        nftsStatus.setVisibility(View.VISIBLE);
                        setMoreVisible(nftsMore, null, 0);
                    } else {
                        Toast.makeText(activity, R.string.nfts_error, Toast.LENGTH_SHORT).show();
                        setMoreVisible(nftsMore, nftsCursor, nftsAdapter.getCount());
                    }
                });
            }
        }, "gram-tab-nfts").start();
    }

    private static final class HistoryAdapter extends BaseAdapter {
        private final Activity activity;
        private final List<Tx> items;
        private final Set<String> revealed = new HashSet<>();
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM, HH:mm", Locale.getDefault());
        HistoryAdapter(Activity activity, List<Tx> items) {this.activity = activity; this.items = items;}
        void replaceAll(List<Tx> next) {items.clear(); items.addAll(next); notifyDataSetChanged();}
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
            comment.setVisibility(View.GONE);
            if (tx.isNft()) {
                arrow.setVisibility(View.GONE);
                if (tx.image.isEmpty()) {badge.setText("NFT"); badge.setVisibility(View.VISIBLE); image.setVisibility(View.GONE);}
                else {badge.setVisibility(View.GONE); image.setVisibility(View.VISIBLE); ImageLoader.load(activity, image, tx.image);}
                title.setText(tx.nftName.isEmpty() ? activity.getString(R.string.tx_nft) : tx.nftName);
                subtitle.setText(tx.collection.isEmpty() ? activity.getString(tx.incoming ? R.string.tx_nft_received : R.string.tx_nft_sent) : tx.collection);
                amount.setText(tx.incoming ? "+ NFT" : "− NFT");
            } else if (Tx.TYPE_TON.equals(tx.type)) {
                badge.setVisibility(View.GONE); image.setVisibility(View.GONE); arrow.setVisibility(View.VISIBLE);
                arrow.setImageResource(tx.incoming ? R.drawable.ic_tx_in : R.drawable.ic_tx_out);
                title.setText(activity.getString(tx.incoming ? R.string.tx_received : R.string.tx_sent));
                subtitle.setText(shortAddress(tx.counterparty));
                String sign = tx.incoming ? "+ " : "− ";
                amount.setText(sign + Format.shortGram(tx.gram) + " GRAM");
            } else {
                badge.setText("···"); badge.setVisibility(View.VISIBLE); image.setVisibility(View.GONE); arrow.setVisibility(View.GONE);
                title.setText(activity.getString(R.string.tx_contract));
                subtitle.setText(shortAddress(tx.counterparty));
                amount.setText(tx.gram > 0 ? ((tx.incoming ? "+ " : "− ") + Format.shortGram(tx.gram) + " GRAM") : "—");
            }
            amount.setTextColor(0xFFFFFFFF);
            time.setText(dateFormat.format(new Date(tx.time * 1000L)));
            if (!tx.success) {title.setText(activity.getString(R.string.tx_failed));}
            return view;
        }
        private String shortAddress(String value) {
            if (value == null || value.isEmpty()) {return activity.getString(R.string.tx_unknown);}
            if (value.indexOf('.') >= 0 || value.length() <= 16) {return value;}
            return value.substring(0, 6) + "…" + value.substring(value.length() - 4);
        }
    }

    private static final class TokenAdapter extends BaseAdapter {
        private final Activity activity;
        private final List<Token> items;
        TokenAdapter(Activity activity, List<Token> items) {this.activity = activity; this.items = items;}
        void clear() {items.clear(); notifyDataSetChanged();}
        void addAll(List<Token> more) {items.addAll(more); notifyDataSetChanged();}
        @Override public int getCount() {return items.size();}
        @Override public Token getItem(int position) {return items.get(position);}
        @Override public long getItemId(int position) {return position;}
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView == null ? activity.getLayoutInflater().inflate(R.layout.row_token, parent, false) : convertView;
            Token token = getItem(position);
            TextView badge = view.findViewById(R.id.token_badge);
            ImageView image = view.findViewById(R.id.token_image);
            TextView name = view.findViewById(R.id.token_name);
            TextView symbol = view.findViewById(R.id.token_symbol);
            TextView amount = view.findViewById(R.id.token_amount);
            TextView usd = view.findViewById(R.id.token_usd);
            String title = token.name.isEmpty() ? token.symbol : token.name;
            name.setText(token.scam ? title + " · scam" : title);
            symbol.setText(token.symbol.isEmpty() ? "—" : token.symbol);
            amount.setText(Format.tokenAmount(token.balanceRaw, token.decimals));
            String usdText = Format.tokenUsd(token.balanceRaw, token.decimals, token.price);
            usd.setText(usdText);
            usd.setVisibility(usdText.isEmpty() ? View.GONE : View.VISIBLE);
            if (token.image.isEmpty()) {
                badge.setText(title.isEmpty() ? "?" : title.substring(0, 1).toUpperCase(Locale.US));
                badge.setVisibility(View.VISIBLE); image.setVisibility(View.GONE); ImageLoader.load(activity, image, "");
            } else {badge.setVisibility(View.GONE); image.setVisibility(View.VISIBLE); ImageLoader.load(activity, image, token.image);}
            return view;
        }
    }

    private static final class NftAdapter extends BaseAdapter {
        private final Activity activity;
        private final List<Nft> items;
        NftAdapter(Activity activity, List<Nft> items) {this.activity = activity; this.items = items;}
        void clear() {items.clear(); notifyDataSetChanged();}
        void addAll(List<Nft> more) {items.addAll(more); notifyDataSetChanged();}
        @Override public int getCount() {return items.size();}
        @Override public Nft getItem(int position) {return items.get(position);}
        @Override public long getItemId(int position) {return position;}
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView == null ? activity.getLayoutInflater().inflate(R.layout.row_nft, parent, false) : convertView;
            Nft nft = getItem(position);
            TextView badge = view.findViewById(R.id.nft_badge);
            ImageView image = view.findViewById(R.id.nft_image);
            TextView name = view.findViewById(R.id.nft_name);
            TextView collection = view.findViewById(R.id.nft_collection);
            String title = nft.name.isEmpty() ? activity.getString(R.string.tx_nft) : nft.name;
            name.setText(nft.scam ? title + " · scam" : title);
            collection.setText(nft.collection.isEmpty() ? "—" : nft.collection);
            if (nft.image.isEmpty()) {
                badge.setText(title.substring(0, 1).toUpperCase(Locale.US));
                badge.setVisibility(View.VISIBLE); image.setVisibility(View.GONE); ImageLoader.load(activity, image, "");
            } else {badge.setVisibility(View.GONE); image.setVisibility(View.VISIBLE); ImageLoader.load(activity, image, nft.image);}
            return view;
        }
    }
}
