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
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class TxDetail {
    private TxDetail() {}

    static void show(Activity activity, Tx tx) {
        if (activity == null || tx == null || activity.isFinishing()) {return;}
        float density = activity.getResources().getDisplayMetrics().density;
        FrameLayout root = new FrameLayout(activity);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0xA6000000, 0x00000000}));
        root.setAlpha(0f);
        View sheet = LayoutInflater.from(activity).inflate(R.layout.dialog_tx_detail, root, false);
        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(Math.round(activity.getResources().getDisplayMetrics().widthPixels * .92f), ViewGroup.LayoutParams.WRAP_CONTENT);
        sheetLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        sheetLp.bottomMargin = Math.round(24 * density);
        root.addView(sheet, sheetLp);
        Dialog dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.BOTTOM);
        }

        bind(activity, sheet, tx);
        PathInterpolator ease = new PathInterpolator(.2f,.9f,.2f,1f);
        Runnable dismiss = () -> {
            sheet.animate().translationY(sheet.getHeight() + 80 * density).setDuration(260).setInterpolator(ease).start();
            root.animate().alpha(0f).setDuration(260).setInterpolator(ease).setListener(new AnimatorListenerAdapter() {@Override public void onAnimationEnd(Animator animation) {try {if (dialog.isShowing()) {dialog.dismiss();}} catch (Exception ignored) {}}}).start();
        };
        root.setOnClickListener(v -> dismiss.run());
        sheet.setOnClickListener(v -> {});
        sheet.findViewById(R.id.tx_detail_close).setOnClickListener(v -> dismiss.run());
        sheet.findViewById(R.id.tx_detail_ok).setOnClickListener(v -> dismiss.run());
        sheet.setTranslationY(720 * density);
        dialog.setOnShowListener(d -> sheet.post(() -> {
            float start = sheet.getHeight() <= 0 ? 720 * density : sheet.getHeight() + 64 * density;
            sheet.setTranslationY(start);
            ObjectAnimator slide = ObjectAnimator.ofFloat(sheet, View.TRANSLATION_Y, start, 0f);
            slide.setDuration(420);
            slide.setInterpolator(ease);
            slide.start();
            root.animate().alpha(1f).setDuration(320).setInterpolator(ease).start();
        }));
        dialog.show();
    }

    private static void bind(Activity activity, View sheet, Tx tx) {
        ImageView gem = sheet.findViewById(R.id.tx_detail_gem);
        TextView amount = sheet.findViewById(R.id.tx_detail_amount);
        TextView usd = sheet.findViewById(R.id.tx_detail_usd);
        TextView partyLabel = sheet.findViewById(R.id.tx_detail_party_label);
        TextView party = sheet.findViewById(R.id.tx_detail_party);
        TextView addressView = sheet.findViewById(R.id.tx_detail_address);
        TextView comment = sheet.findViewById(R.id.tx_detail_comment);
        TextView date = sheet.findViewById(R.id.tx_detail_date);
        float density = activity.getResources().getDisplayMetrics().density;
        partyLabel.setText(tx.incoming ? R.string.tx_detail_sender : R.string.tx_detail_recipient);
        String who = tx.counterparty == null || tx.counterparty.isEmpty() ? "" : tx.counterparty.trim();
        String whoLabel = who.isEmpty() ? activity.getString(R.string.tx_detail_dash) : (who.indexOf('.') >= 0 || who.length() <= 18 ? who : who.substring(0, 6) + "…" + who.substring(who.length() - 4));
        String whoAddress = who.isEmpty() ? activity.getString(R.string.tx_detail_dash) : Format.displayAddress(who);
        if (tx.isNft()) {
            int size = Math.round(128 * density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            lp.topMargin = Math.round(4 * density);
            gem.setLayoutParams(lp);
            gem.setVisibility(View.VISIBLE);
            gem.setBackgroundResource(R.drawable.tx_detail_nft_bg);
            gem.setClipToOutline(true);
            gem.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            gem.setScaleType(ImageView.ScaleType.CENTER_CROP);
            gem.setPadding(0, 0, 0, 0);
            gem.setAlpha(1f);
            String image = detailNftImage(tx.image);
            if (image.isEmpty()) {
                ImageLoader.load(activity, gem, "");
                gem.setImageResource(R.drawable.ic_image_placeholder);
                gem.setScaleType(ImageView.ScaleType.FIT_CENTER);
                gem.setAlpha(.55f);
            } else {ImageLoader.load(activity, gem, image);}
            amount.setText(R.string.tx_detail_nft_transfer);
            amount.setTextColor(0xFF8E8E93);
            amount.setTextSize(14f);
            amount.setTypeface(amount.getTypeface(), android.graphics.Typeface.NORMAL);
            usd.setText(tx.nftName.isEmpty() ? activity.getString(R.string.tx_nft) : tx.nftName);
            usd.setTextColor(0xFF111111);
            usd.setTextSize(22f);
            usd.setTypeface(usd.getTypeface(), android.graphics.Typeface.BOLD);
            party.setText(whoLabel);
            addressView.setText(whoAddress);
            comment.setText(activity.getString(R.string.tx_detail_dash));
            comment.setMaxLines(2);
        } else {
            int size = Math.round(72 * density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            gem.setLayoutParams(lp);
            gem.setVisibility(View.VISIBLE);
            gem.setBackground(null);
            gem.setClipToOutline(false);
            ImageLoader.load(activity, gem, "");
            gem.setImageResource(R.drawable.blue_diamond);
            gem.setScaleType(ImageView.ScaleType.FIT_CENTER);
            gem.setAlpha(1f);
            gem.setPadding(0, 0, 0, 0);
            String sign = tx.incoming ? "+" : "−";
            String grams = Format.shortGram(tx.gram);
            amount.setTextSize(28f);
            amount.setTypeface(amount.getTypeface(), android.graphics.Typeface.BOLD);
            amount.setText(gramAmountSpan(sign + grams, !tx.success), TextView.BufferType.SPANNABLE);
            usd.setTextSize(14f);
            usd.setTypeface(usd.getTypeface(), android.graphics.Typeface.NORMAL);
            usd.setTextColor(0xFF8E8E93);
            WalletData data = Store.load(activity);
            if (data != null && data.hasUsd && data.gram > 0) {
                double approx = tx.gram * (data.totalUsd / data.gram);
                usd.setText(activity.getString(R.string.tx_detail_usd, Format.shortGram(approx)));
            } else {usd.setText(R.string.tx_detail_dash);}
            party.setText(who.isEmpty() ? activity.getString(R.string.tx_unknown) : whoLabel);
            addressView.setText(who.isEmpty() ? activity.getString(R.string.tx_unknown) : whoAddress);
            String note = tx.comment == null ? "" : tx.comment.trim().replace('\n', ' ');
            if (note.length() > 120) {note = note.substring(0, 117) + "…";}
            comment.setText(note.isEmpty() ? activity.getString(R.string.tx_detail_dash) : note);
            comment.setMaxLines(3);
        }
        date.setText(new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(new Date(tx.time * 1000L)));
    }

    private static String detailNftImage(String url) {
        if (url == null || url.isEmpty()) {return "";}
        if (url.contains("/pr:small/")) {return url.replace("/pr:small/", "/pr:big/");}
        return url;
    }

    private static CharSequence gramAmountSpan(String value, boolean failed) {
        String label = " Grams";
        SpannableString text = new SpannableString(value + label);
        int numColor = failed ? 0xFFFF3B30 : 0xFF34C759;
        text.setSpan(new ForegroundColorSpan(numColor), 0, value.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new ForegroundColorSpan(0xFF0088FF), value.length(), text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }
}
