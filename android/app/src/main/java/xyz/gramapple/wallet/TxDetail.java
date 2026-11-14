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
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
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
        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(Math.round(activity.getResources().getDisplayMetrics().widthPixels * 0.92f), ViewGroup.LayoutParams.WRAP_CONTENT);
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
        PathInterpolator ease = new PathInterpolator(0.2f, 0.9f, 0.2f, 1f);
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
        View gem = sheet.findViewById(R.id.tx_detail_gem);
        TextView amount = sheet.findViewById(R.id.tx_detail_amount);
        TextView usd = sheet.findViewById(R.id.tx_detail_usd);
        TextView partyLabel = sheet.findViewById(R.id.tx_detail_party_label);
        TextView party = sheet.findViewById(R.id.tx_detail_party);
        TextView addressView = sheet.findViewById(R.id.tx_detail_address);
        TextView comment = sheet.findViewById(R.id.tx_detail_comment);
        TextView date = sheet.findViewById(R.id.tx_detail_date);
        partyLabel.setText(tx.incoming ? R.string.tx_detail_sender : R.string.tx_detail_recipient);
        if (tx.isNft()) {
            gem.setVisibility(View.GONE);
            amount.setText(tx.incoming ? "+ NFT" : "− NFT");
            amount.setTextColor(tx.incoming ? 0xFF34C759 : 0xFF111111);
            usd.setText(tx.nftName.isEmpty() ? activity.getString(R.string.tx_nft) : tx.nftName);
            party.setText(tx.collection.isEmpty() ? activity.getString(R.string.tx_detail_dash) : tx.collection);
            addressView.setText(activity.getString(R.string.tx_detail_dash));
            comment.setText(activity.getString(R.string.tx_detail_dash));
        } else {
            gem.setVisibility(View.VISIBLE);
            String sign = tx.incoming ? "+" : "−";
            String grams = Format.shortGram(tx.gram);
            amount.setText(gramAmountSpan(sign + grams, !tx.success), TextView.BufferType.SPANNABLE);
            WalletData data = Store.load(activity);
            if (data != null && data.hasUsd && data.gram > 0) {
                double approx = tx.gram * (data.totalUsd / data.gram);
                usd.setText(activity.getString(R.string.tx_detail_usd, Format.shortGram(approx)));
            } else {usd.setText(R.string.tx_detail_dash);}
            String who = tx.counterparty == null || tx.counterparty.isEmpty() ? activity.getString(R.string.tx_unknown) : tx.counterparty;
            party.setText(who.indexOf('.') >= 0 || who.length() <= 18 ? who : who.substring(0, 6) + "…" + who.substring(who.length() - 4));
            addressView.setText(Format.chunkAddress(who).replace('\n', ' '));
            String note = tx.comment == null ? "" : tx.comment.trim().replace('\n', ' ');
            if (note.length() > 120) {note = note.substring(0, 117) + "…";}
            comment.setText(note.isEmpty() ? activity.getString(R.string.tx_detail_dash) : note);
            comment.setMaxLines(3);
        }
        date.setText(new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(new Date(tx.time * 1000L)));
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
