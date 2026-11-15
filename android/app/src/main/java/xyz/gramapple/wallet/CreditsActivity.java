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
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CreditsActivity extends Activity {
    private static final String[][] PEOPLE = {{"coonlink", "author_role_creator"},{"GiftChanges", "author_role_gifts"}};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupEdgeToEdge();
        setContentView(R.layout.activity_credits);
        applySystemBarInsets(findViewById(R.id.credits_root));
        findViewById(R.id.credits_back).setOnClickListener(v -> finish());
        bindList();
    }

    private void bindList() {
        LinearLayout list = findViewById(R.id.credits_list);
        list.removeAllViews();
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < PEOPLE.length; i++) {
            String user = PEOPLE[i][0];
            int roleId = getResources().getIdentifier(PEOPLE[i][1], "string", getPackageName());
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int padH = Math.round(14 * d);
            int padV = Math.round(12 * d);
            row.setPadding(padH, padV, padH, padV);
            int sel = resolveSelectable();
            if (sel != 0) {row.setBackgroundResource(sel);}
            ImageView avatar = new ImageView(this);
            int size = Math.round(36 * d);
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(size, size);
            avatar.setLayoutParams(ap);
            avatar.setBackgroundResource(R.drawable.avatar_circle_bg);
            avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            avatar.setClipToOutline(true);
            avatar.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View view, Outline outline) {
                    int s = Math.min(view.getWidth(), view.getHeight());
                    if (s <= 0) {outline.setEmpty(); return;}
                    outline.setOval(0, 0, s, s);
                }
            });
            ImageLoader.load(this, avatar, "https://t.me/i/userpic/320/" + user + ".jpg");
            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tp.leftMargin = Math.round(12 * d);
            textCol.setLayoutParams(tp);
            TextView name = new TextView(this);
            name.setText("@" + user);
            name.setTextColor(0xFFFFFFFF);
            name.setTextSize(16f);
            TextView role = new TextView(this);
            role.setText(roleId != 0 ? getString(roleId) : "");
            role.setTextColor(0x99FFFFFF);
            role.setTextSize(12f);
            role.setPadding(0, Math.round(2 * d), 0, 0);
            textCol.addView(name);
            textCol.addView(role);
            row.addView(avatar);
            row.addView(textCol);
            row.setOnClickListener(v -> {
                try {startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/" + user)));}
                catch (Exception e) {Toast.makeText(this, R.string.tonconnect_wallet_missing, Toast.LENGTH_SHORT).show();}
            });
            list.addView(row);
            if (i < PEOPLE.length - 1) {
                View line = new View(this);
                line.setBackgroundColor(0x33FFFFFF);
                list.addView(line, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, Math.round(0.5f * d))));
            }
        }
    }

    private int resolveSelectable() {
        android.util.TypedValue out = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, out, true);
        return out.resourceId;
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
