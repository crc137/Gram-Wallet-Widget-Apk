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
import java.util.Locale;

public class NftsActivity extends Activity {
    private ListView listView;
    private TextView statusView;
    private ProgressBar progressView;
    private Button moreButton;
    private NftAdapter adapter;
    private String address;
    private String nextCursor;
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupEdgeToEdge();
        setContentView(R.layout.activity_nfts);
        applySystemBarInsets(findViewById(R.id.nfts_root));
        listView = findViewById(R.id.nfts_list);
        statusView = findViewById(R.id.nfts_status);
        progressView = findViewById(R.id.nfts_progress);
        findViewById(R.id.nfts_back).setOnClickListener(view -> finish());
        moreButton = new Button(this);
        moreButton.setBackgroundResource(R.drawable.button_white);
        moreButton.setText(R.string.nfts_load_more);
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
        adapter = new NftAdapter(this, new ArrayList<>());
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
                Api.NftsPage page = Api.fetchNfts(address, cursor);
                runOnUiThread(() -> {
                    if (isFinishing()) {return;}
                    loading = false;
                    progressView.setVisibility(View.GONE);
                    if (!append) {adapter.clear();}
                    adapter.addAll(page.items);
                    nextCursor = page.next;
                    moreButton.setVisibility(nextCursor == null ? View.GONE : View.VISIBLE);
                    if (adapter.getCount() == 0) {
                        statusView.setText(R.string.nfts_empty);
                        statusView.setVisibility(View.VISIBLE);
                    } else {statusView.setVisibility(View.GONE);}
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing()) {return;}
                    loading = false;
                    progressView.setVisibility(View.GONE);
                    if (adapter.getCount() == 0) {
                        statusView.setText(R.string.nfts_error);
                        statusView.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(appContext, R.string.nfts_error, Toast.LENGTH_SHORT).show();
                        moreButton.setVisibility(nextCursor == null ? View.GONE : View.VISIBLE);
                    }
                });
            }
        }, "gram-nfts").start();
    }

    private static final class NftAdapter extends BaseAdapter {
        private final Activity activity;
        private final List<Nft> items;

        NftAdapter(Activity activity, List<Nft> items) {
            this.activity = activity;
            this.items = items;
        }

        void clear() {items.clear(); notifyDataSetChanged();}
        void addAll(List<Nft> more) {items.addAll(more); notifyDataSetChanged();}
        @Override public int getCount() {return items.size();}
        @Override public Nft getItem(int position) {return items.get(position);}
        @Override public long getItemId(int position) {return position;}
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {view = activity.getLayoutInflater().inflate(R.layout.row_nft, parent, false);}
            Nft nft = getItem(position);
            TextView badge = view.findViewById(R.id.nft_badge);
            ImageView image = view.findViewById(R.id.nft_image);
            TextView name = view.findViewById(R.id.nft_name);
            TextView collection = view.findViewById(R.id.nft_collection);
            String title = nft.name.isEmpty() ? activity.getString(R.string.tx_nft) : nft.name;
            name.setText(nft.scam ? title + " · scam" : title);
            collection.setText(nft.collection.isEmpty() ? "—" : nft.collection);
            if (nft.image.isEmpty()) {
                badge.setText(initial(title));
                badge.setVisibility(View.VISIBLE);
                image.setVisibility(View.GONE);
                ImageLoader.load(activity, image, "");
            } else {
                badge.setVisibility(View.GONE);
                image.setVisibility(View.VISIBLE);
                ImageLoader.load(activity, image, nft.image);
            }
            return view;
        }

        private String initial(String value) {
            if (value == null || value.isEmpty()) {return "?";}
            return value.substring(0, 1).toUpperCase(Locale.US);
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
