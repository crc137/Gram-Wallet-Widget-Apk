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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ReceiveActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String address = Store.getAddress(this);
        if (address == null || address.isEmpty()) {
            startActivity(new android.content.Intent(this, MainActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_receive);
        findViewById(R.id.receive_root).setOnClickListener(v -> finish());
        findViewById(R.id.receive_panel).setOnClickListener(v -> {});
        findViewById(R.id.receive_close).setOnClickListener(v -> finish());
        if (!bindReceiveViews(this, findViewById(R.id.receive_panel), address)) {finish();}
    }

    static boolean bindReceiveViews(Context context, View root, String address) {
        final AddressRingView ring = root.findViewById(R.id.receive_ring);
        final View card = root.findViewById(R.id.receive_card);
        TextView addressView = root.findViewById(R.id.receive_address);
        addressView.setTextColor(0xFF111111);
        addressView.setText(Format.chunkAddressStyled(address), TextView.BufferType.SPANNABLE);
        View donorBadge = root.findViewById(R.id.receive_donor);
        if (donorBadge != null) {donorBadge.setVisibility(Store.isDonor(context) ? View.VISIBLE : View.GONE);}
        View.OnClickListener copy = v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {clipboard.setPrimaryClip(ClipData.newPlainText("address", address));}
            Toast.makeText(context, R.string.address_copied, Toast.LENGTH_SHORT).show();
        };
        addressView.setOnClickListener(copy);
        View copyHint = root.findViewById(R.id.receive_copy);
        if (copyHint != null) {copyHint.setOnClickListener(copy);}
        if (card != null) {card.setOnClickListener(copy);}
        if (ring != null && card != null) {
            Runnable fitRing = () -> {
                int m = Math.round(20f * context.getResources().getDisplayMetrics().density);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(card.getWidth() + m * 2, card.getHeight() + m * 2, Gravity.CENTER);
                ring.setLayoutParams(lp);
                ring.setAddress(address);
            };
            if (card.getWidth() > 0 && card.getHeight() > 0) {fitRing.run();} else {card.post(fitRing);}
        } else if (ring != null) {ring.setAddress(address);}
        try {
            int size = Math.round(200 * context.getResources().getDisplayMetrics().density);
            Bitmap qr = QrEncoder.bitmap(context.getResources(), address, size);
            ((ImageView) root.findViewById(R.id.receive_qr)).setImageBitmap(qr);
            return true;
        } catch (Exception error) {
            Toast.makeText(context, R.string.error_loading, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}
