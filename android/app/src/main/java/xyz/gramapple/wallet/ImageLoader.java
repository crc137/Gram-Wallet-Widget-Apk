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
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ImageLoader {
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final int TARGET_PX = 160;
    private static final int REVEAL_MS = 680;
    private static final float BLUR_START = 22f;
    private static final float SCALE_START = 1.06f;
    private static final PathInterpolator EASE = new PathInterpolator(0.22f, 1f, 0.36f, 1f);
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(8 * 1024 * 1024) {@Override protected int sizeOf(String key, Bitmap value) {return value.getByteCount();}};
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private ImageLoader() {}

    static void load(Context context, ImageView view, String url) {
        cancelReveal(view);
        String target = url == null || "null".equalsIgnoreCase(url) ? "" : url.trim();
        view.setTag(target);
        if (target.isEmpty()) {
            clearEffects(view);
            view.setTag(R.id.image_bound_url, null);
            view.setImageDrawable(null);
            return;
        }
        Object bound = view.getTag(R.id.image_bound_url);
        if (target.equals(bound) && view.getDrawable() != null) {
            clearEffects(view);
            return;
        }
        Bitmap cached = CACHE.get(target);
        if (cached != null) {
            reveal(view, cached, target, true);
            return;
        }
        showPlaceholder(view);
        final Context appContext = context.getApplicationContext();
        POOL.execute(() -> {
            byte[] data = readDisk(appContext, target);
            Bitmap bitmap = data == null ? null : decode(data);
            if (bitmap == null) {
                data = fetch(target);
                bitmap = data == null ? null : decode(data);
                if (bitmap != null) {writeDisk(appContext, target, data);}
            }
            if (bitmap == null) {return;}
            CACHE.put(target, bitmap);
            final Bitmap result = bitmap;
            MAIN.post(() -> {
                if (!target.equals(view.getTag())) {return;}
                reveal(view, result, target, false);
            });
        });
    }

    private static void showPlaceholder(ImageView view) {
        clearEffects(view);
        view.setTag(R.id.image_bound_url, null);
        int pad = Math.round(8f * view.getResources().getDisplayMetrics().density);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setPadding(pad, pad, pad, pad);
        view.setImageResource(R.drawable.ic_coondev);
        view.setAlpha(0.5f);
        view.setScaleX(1f);
        view.setScaleY(1f);
    }

    private static void reveal(ImageView view, Bitmap bitmap, String url, boolean fromMemory) {
        cancelReveal(view);
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        view.setPadding(0, 0, 0, 0);
        view.setImageBitmap(bitmap);
        view.setTag(R.id.image_bound_url, url);
        view.setPivotX(view.getWidth() > 0 ? view.getWidth() / 2f : view.getLayoutParams().width / 2f);
        view.setPivotY(view.getHeight() > 0 ? view.getHeight() / 2f : view.getLayoutParams().height / 2f);
        view.setAlpha(0f);
        view.setScaleX(SCALE_START);
        view.setScaleY(SCALE_START);
        applyBlur(view, BLUR_START);
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(fromMemory ? 360 : REVEAL_MS);
        animator.setInterpolator(EASE);
        animator.addUpdateListener(animation -> {
            float t = (Float) animation.getAnimatedValue();
            float soft = t * t * (3f - 2f * t);
            view.setAlpha(soft);
            float scale = SCALE_START + (1f - SCALE_START) * soft;
            view.setScaleX(scale);
            view.setScaleY(scale);
            applyBlur(view, BLUR_START * (1f - soft));
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {finishReveal(view, animator);}
            @Override public void onAnimationCancel(Animator animation) {if (view.getTag(R.id.image_reveal_anim) == animator) {view.setTag(R.id.image_reveal_anim, null);}}
        });
        view.setTag(R.id.image_reveal_anim, animator);
        animator.start();
    }

    private static void finishReveal(ImageView view, ValueAnimator animator) {
        if (view.getTag(R.id.image_reveal_anim) == animator) {view.setTag(R.id.image_reveal_anim, null);}
        clearEffects(view);
    }

    private static void cancelReveal(ImageView view) {
        Object anim = view.getTag(R.id.image_reveal_anim);
        if (anim instanceof ValueAnimator) {
            view.setTag(R.id.image_reveal_anim, null);
            ((ValueAnimator) anim).cancel();
        }
    }

    private static void clearEffects(ImageView view) {
        clearBlur(view);
        view.setAlpha(1f);
        view.setScaleX(1f);
        view.setScaleY(1f);
    }

    private static void applyBlur(ImageView view, float radius) {
        if (Build.VERSION.SDK_INT < 31) {return;}
        if (radius <= 0.4f) {clearBlur(view); return;}
        view.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
    }

    private static void clearBlur(ImageView view) {if (Build.VERSION.SDK_INT >= 31) {view.setRenderEffect(null);}}

    private static byte[] fetch(String url) {
        byte[] data = fetchBytes(url);
        if (data == null && url.contains("gram.org/api/img")) {
            int marker = url.indexOf("u=");
            if (marker >= 0) {
                String raw = Uri.decode(url.substring(marker + 2));
                if (raw != null && !raw.isEmpty() && !"null".equalsIgnoreCase(raw)) {data = fetchBytes(raw);}
            }
        }
        if (data == null && url.contains("/pr:small/")) {data = fetchBytes(url.replace("/pr:small/", "/pr:big/"));}
        return data;
    }

    private static byte[] fetchBytes(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(20000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "GramWalletWidget_app (Android)");
            connection.setRequestProperty("Accept", "image/*,*/*;q=0.8");
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {return null;}
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream stream = connection.getInputStream()) {
                byte[] chunk = new byte[8192];
                int read;
                int total = 0;
                while ((read = stream.read(chunk)) != -1) {
                    total += read;
                    if (total > MAX_BYTES) {return null;}
                    buffer.write(chunk, 0, read);
                }
            }
            return buffer.toByteArray();
        } catch (Exception error) {return null;} finally {if (connection != null) {connection.disconnect();}}
    }

    private static Bitmap decode(byte[] data) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
            int sample = 1;
            while (bounds.outWidth / (sample * 2) >= TARGET_PX && bounds.outHeight / (sample * 2) >= TARGET_PX) {sample *= 2;}
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } catch (Exception error) {return null;}
    }

    private static File cacheFile(Context context, String url) {
        File dir = new File(context.getCacheDir(), "nft_images");
        if (!dir.exists()) {dir.mkdirs();}
        return new File(dir, Integer.toHexString(url.hashCode()) + ".img");
    }

    private static byte[] readDisk(Context context, String url) {
        File file = cacheFile(context, url);
        if (!file.isFile()) {return null;}
        try (FileInputStream stream = new FileInputStream(file)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = stream.read(chunk)) != -1) {buffer.write(chunk, 0, read);}
            return buffer.toByteArray();
        } catch (Exception error) {
            file.delete();
            return null;
        }
    }

    private static void writeDisk(Context context, String url, byte[] data) {try (FileOutputStream stream = new FileOutputStream(cacheFile(context, url))) {stream.write(data);} catch (Exception ignored) {}}
}
