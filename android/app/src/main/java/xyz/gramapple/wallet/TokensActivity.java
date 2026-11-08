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
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
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
import java.util.ArrayList;
import java.util.List;

public class TokensActivity extends Activity {
    private ListView listView;
    private TextView statusView;
    private ProgressBar progressView;
    private Button moreButton;
    private TokenAdapter adapter;
    private String address;
    private String nextCursor;
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupEdgeToEdge();
        setContentView(R.layout.activity_tokens);
        applySystemBarInsets(findViewById(R.id.tokens_root));
        listView = findViewById(R.id.tokens_list);
        statusView = findViewById(R.id.tokens_status);
        progressView = findViewById(R.id.tokens_progress);
        findViewById(R.id.tokens_back).setOnClickListener(view -> finish());
        moreButton = new Button(this);
        moreButton.setBackgroundResource(R.drawable.button_white);
        moreButton.setText(R.string.tokens_load_more);
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
        adapter = new TokenAdapter(this, new ArrayList<>());
        listView.setAdapter(adapter);
        address = Store.getAddress(this);
        if (address == null) {
            Toast.makeText(this, R.string.save_address_first, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        load(null, false);
    }

    private void loadMore() {if (nextCursor != null) {load(nextCursor, true);}}

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
                Api.TokensPage page = Api.fetchTokens(address, cursor);
                runOnUiThread(() -> {
                    if (isFinishing()) {return;}
                    loading = false;
                    progressView.setVisibility(View.GONE);
                    if (!append) {adapter.clear();}
                    adapter.addAll(page.items);
                    nextCursor = page.next;
                    moreButton.setVisibility(nextCursor == null ? View.GONE : View.VISIBLE);
                    if (adapter.getCount() == 0) {
                        statusView.setText(R.string.tokens_empty);
                        statusView.setVisibility(View.VISIBLE);
                    } else {statusView.setVisibility(View.GONE);}
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing()) {return;}
                    loading = false;
                    progressView.setVisibility(View.GONE);
                    if (adapter.getCount() == 0) {
                        statusView.setText(R.string.tokens_error);
                        statusView.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(appContext, R.string.tokens_error, Toast.LENGTH_SHORT).show();
                        moreButton.setVisibility(nextCursor == null ? View.GONE : View.VISIBLE);
                    }
                });
            }
        }, "gram-tokens").start();
    }

    private static final class TokenAdapter extends BaseAdapter {
        private final Activity activity;
        private final List<Token> items;

        TokenAdapter(Activity activity, List<Token> items) {
            this.activity = activity;
            this.items = items;
        }

        void clear() {items.clear(); notifyDataSetChanged();}
        void addAll(List<Token> more) {items.addAll(more); notifyDataSetChanged();}
        @Override public int getCount() {return items.size();}
        @Override public Token getItem(int position) {return items.get(position);}
        @Override public long getItemId(int position) {return position;}
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {view = activity.getLayoutInflater().inflate(R.layout.row_token, parent, false);}
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
                badge.setText(initial(token.symbol.isEmpty() ? title : token.symbol));
                badge.setVisibility(View.VISIBLE);
                image.setVisibility(View.GONE);
                ImageLoader.load(activity, image, "");
            } else {
                badge.setVisibility(View.GONE);
                image.setVisibility(View.VISIBLE);
                ImageLoader.load(activity, image, token.image);
            }
            return view;
        }

        private String initial(String value) {
            if (value == null || value.isEmpty()) {return "?";}
            return value.substring(0, 1).toUpperCase(java.util.Locale.US);
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
