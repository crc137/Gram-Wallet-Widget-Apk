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
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HistoryActivity extends Activity {
    private ListView listView;
    private TextView statusView;
    private ProgressBar progressView;
    private Button moreButton;
    private TxAdapter adapter;
    private String address;
    private String nextCursor;
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupEdgeToEdge();
        setContentView(R.layout.activity_history);
        applySystemBarInsets(findViewById(R.id.history_root));
        listView = findViewById(R.id.history_list);
        statusView = findViewById(R.id.history_status);
        progressView = findViewById(R.id.history_progress);
        findViewById(R.id.history_back).setOnClickListener(view -> finish());
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
        address = Store.getAddress(this);
        if (address == null) {
            Toast.makeText(this, R.string.save_address_first, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        load(null, false);
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
        }
        moreButton.setVisibility(View.GONE);
        final Context appContext = getApplicationContext();
        new Thread(() -> {
            try {
                Api.ActionsPage page = Api.fetchActions(address, cursor);
                runOnUiThread(() -> {
                    if (isFinishing()) {return;}
                    loading = false;
                    progressView.setVisibility(View.GONE);
                    adapter.addAll(page.items);
                    nextCursor = page.next;
                    moreButton.setVisibility(nextCursor == null ? View.GONE : View.VISIBLE);
                    if (adapter.getCount() == 0) {
                        statusView.setText(R.string.history_empty);
                        statusView.setVisibility(View.VISIBLE);
                    } else {statusView.setVisibility(View.GONE);}
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

        void addAll(List<Tx> more) {items.addAll(more); notifyDataSetChanged();}
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
            } else {
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
