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
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;

public final class CoverVideoView extends FrameLayout implements TextureView.SurfaceTextureListener {
    private final TextureView texture;
    private MediaPlayer player;
    private Uri pendingUri;
    private Surface surface;
    private int videoWidth;
    private int videoHeight;
    private boolean prepared;
    private boolean startWhenReady;
    private MediaPlayer.OnPreparedListener preparedListener;
    private MediaPlayer.OnErrorListener errorListener;

    public CoverVideoView(Context context) {this(context, null);}
    public CoverVideoView(Context context, AttributeSet attrs) {this(context, attrs, 0);}
    public CoverVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        texture = new TextureView(context);
        texture.setOpaque(false);
        texture.setSurfaceTextureListener(this);
        addView(texture, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    void setVideoURI(Uri uri) {
        pendingUri = uri;
        prepared = false;
        releasePlayer();
        if (surface != null && uri != null) {openPlayer();}
    }

    void setOnPreparedListener(MediaPlayer.OnPreparedListener listener) {preparedListener = listener;}
    void setOnErrorListener(MediaPlayer.OnErrorListener listener) {errorListener = listener;}

    void start() {
        startWhenReady = true;
        if (player != null && prepared) {try {player.start();} catch (Exception ignored) {}}
    }

    void pause() {
        startWhenReady = false;
        if (player != null) {try {if (player.isPlaying()) {player.pause();}} catch (Exception ignored) {}}
    }

    void stopPlayback() {
        startWhenReady = false;
        prepared = false;
        pendingUri = null;
        releasePlayer();
    }

    void suspend() {pause();}

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        surface = new Surface(surfaceTexture);
        if (pendingUri != null) {openPlayer();}
        applyCoverTransform();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {applyCoverTransform();}

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        releasePlayer();
        if (surface != null) {
            surface.release();
            surface = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        applyCoverTransform();
    }

    private void openPlayer() {
        if (surface == null || pendingUri == null) {return;}
        releasePlayer();
        try {
            player = new MediaPlayer();
            player.setSurface(surface);
            player.setDataSource(getContext(), pendingUri);
            player.setOnPreparedListener(mp -> {
                prepared = true;
                videoWidth = mp.getVideoWidth();
                videoHeight = mp.getVideoHeight();
                applyCoverTransform();
                if (preparedListener != null) {preparedListener.onPrepared(mp);}
                if (startWhenReady) {try {mp.start();} catch (Exception ignored) {}}
            });
            player.setOnErrorListener((mp, what, extra) -> errorListener != null && errorListener.onError(mp, what, extra));
            player.prepareAsync();
        } catch (Exception error) {
            releasePlayer();
            if (errorListener != null) {errorListener.onError(null, 1, 0);}
        }
    }

    private void applyCoverTransform() {
        int viewW = texture.getWidth();
        int viewH = texture.getHeight();
        if (viewW <= 0 || viewH <= 0 || videoWidth <= 0 || videoHeight <= 0) {return;}
        float scale = Math.max(viewW / (float) videoWidth, viewH / (float) videoHeight);
        float scaledW = videoWidth * scale;
        float scaledH = videoHeight * scale;
        Matrix matrix = new Matrix();
        matrix.setScale(scaledW / viewW, scaledH / viewH);
        matrix.postTranslate((viewW - scaledW) * 0.5f, (viewH - scaledH) * 0.5f);
        texture.setTransform(matrix);
    }

    private void releasePlayer() {
        if (player == null) {return;}
        try {player.setOnPreparedListener(null); player.setOnErrorListener(null);} catch (Exception ignored) {}
        try {player.stop();} catch (Exception ignored) {}
        try {player.reset();} catch (Exception ignored) {}
        try {player.release();} catch (Exception ignored) {}
        player = null;
        prepared = false;
    }
}
