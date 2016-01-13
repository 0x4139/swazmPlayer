package com.swazm.swazmplayer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.view.SurfaceHolder.Callback;

import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;

public class Player extends Activity implements IVideoPlayer {

    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;
    private int _currentSize = SURFACE_BEST_FIT;

    private static final String TAG = Player.class.getSimpleName();
    private int _videoHeight;
    private int _videoWidth;
    private int _videoVisibleHeight;
    private int _videoVisibleWidth;
    private int _sarNum;
    private int _sarDen;
    private SurfaceView _surfaceView;
    private SurfaceHolder _surfaceHolder;
    private FrameLayout _surfaceFrame;
    private String _mediaUrl;
    private LibVLC _player;
    private Surface _surface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_player);
        _surfaceView = (SurfaceView) findViewById(R.id.player_surface);
        _surfaceHolder = _surfaceView.getHolder();
        _surfaceFrame = (FrameLayout) findViewById(R.id.player_surface_frame);
        _mediaUrl = getIntent().getExtras().getString("videoUrl");
        _surface = _surfaceHolder.getSurface();
        try {
            _player = new LibVLC();
            _player.setAout(_player.AOUT_OPENSLES);
            _player.setVout(_player.VOUT_ANDROID_SURFACE);
            _player.setHardwareAcceleration(LibVLC.HW_ACCELERATION_FULL);
            _player.attachSurface(_surface, Player.this);
            _player.setTimeStretching(true);
            _player.setVerboseMode(false);
            _player.init(getApplicationContext());
            _surfaceHolder.addCallback(_surfaceCallback);

        } catch (LibVlcException e) {
            Log.e(TAG, e.toString());
        }
        _player.playMRL(_mediaUrl);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // MediaCodec opaque direct rendering should not be used anymore since there is no surface to attach.
        _player.stop();
    }

    @Override
    public void setSurfaceLayout(int width, int height, int visible_width, int visible_height, int sar_num, int sar_den) {
        Log.d(TAG, "setSurfaceSize -- START");
        if (width * height == 0)
            return;

        // store video size
        _videoHeight = height;
        _videoWidth = width;
        _videoVisibleHeight = visible_height;
        _videoVisibleWidth = visible_width;
        _sarNum = sar_num;
        _sarDen = sar_den;
        changeSurfaceSize();

        Log.d(TAG, "setSurfaceSize -- mMediaUrl: " + _mediaUrl + " mVideoHeight: " + _videoHeight + " mVideoWidth: " + _videoWidth + " mVideoVisibleHeight: " + _videoVisibleHeight + " mVideoVisibleWidth: " + _videoVisibleWidth + " mSarNum: " + _sarNum + " mSarDen: " + _sarDen);

    }

    @Override
    public int configureSurface(Surface surface, int width, int height, int hal) {
        return -1;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void changeSurfaceSize() {
        int screenWidth = this.getWindow().getDecorView().getWidth();
        int screenHeight = this.getWindow().getDecorView().getHeight();


        double displayWidth = screenWidth, displayHeight = screenHeight;

        if (screenWidth < screenHeight) {
            displayWidth = screenHeight;
            displayHeight = screenWidth;
        }

        // sanity check
        if (displayWidth * displayHeight == 0 || _videoWidth * _videoHeight == 0) {
            Log.e("Video resize", "invalid values");
            return;
        }

        // compute the aspect ratio
        double aspectRatio, visibleWidth;
        if (_sarDen == _sarNum) {
            /* No indication about the density, assuming 1:1 */
            visibleWidth = _videoVisibleWidth;
            aspectRatio = (double) _videoVisibleWidth / (double) _videoVisibleHeight;
        } else {
            /* Use the specified aspect ratio */
            visibleWidth = _videoVisibleWidth * (double) _sarNum / _sarDen;
            aspectRatio = visibleWidth / _videoVisibleHeight;
        }

        // compute the display aspect ratio
        double displayAspectRatio = displayWidth / displayHeight;

        switch (_currentSize) {
            case SURFACE_BEST_FIT:
                if (displayAspectRatio < aspectRatio)
                    displayHeight = displayWidth / aspectRatio;
                else
                    displayWidth = displayHeight * aspectRatio;
                break;
            case SURFACE_FIT_HORIZONTAL:
                displayHeight = displayWidth / aspectRatio;
                break;
            case SURFACE_FIT_VERTICAL:
                displayWidth = displayHeight * aspectRatio;
                break;
            case SURFACE_FILL:
                break;
            case SURFACE_16_9:
                aspectRatio = 16.0 / 9.0;
                if (displayAspectRatio < aspectRatio)
                    displayHeight = displayWidth / aspectRatio;
                else
                    displayWidth = displayHeight * aspectRatio;
                break;
            case SURFACE_4_3:
                aspectRatio = 4.0 / 3.0;
                if (displayAspectRatio < aspectRatio)
                    displayHeight = displayWidth / aspectRatio;
                else
                    displayWidth = displayHeight * aspectRatio;
                break;
            case SURFACE_ORIGINAL:
                displayHeight = _videoVisibleHeight;
                displayWidth = visibleWidth;
                break;
        }
        final double finalDisplayWidth = displayWidth;
        final double finalDisplayHeight = displayHeight;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewGroup.LayoutParams lp = _surfaceView.getLayoutParams();
                lp.width = (int) Math.ceil(finalDisplayWidth * _videoWidth / _videoVisibleWidth);
                lp.height = (int) Math.ceil(finalDisplayHeight * _videoHeight / _videoVisibleHeight);
                Log.e("resize", "Chaging size to: " + lp.width + " x " + lp.height);
                _surfaceView.setLayoutParams(lp);
                _surfaceView.invalidate();
                Log.e("DONE!", "Chaging size to: " + lp.width + " x " + lp.height);
            }
        });


    }

    /**
     * attach and disattach surface to the lib
     */
    private final SurfaceHolder.Callback _surfaceCallback = new Callback() {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (format == PixelFormat.RGBX_8888)
                Log.d(TAG, "Pixel format is RGBX_8888");
            else if (format == PixelFormat.RGB_565)
                Log.d(TAG, "Pixel format is RGB_565");
            else if (format == ImageFormat.YV12)
                Log.d(TAG, "Pixel format is YV12");
            else
                Log.d(TAG, "Pixel format is other/unknown");
            if (_player != null)
                _player.attachSurface(holder.getSurface(), Player.this);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (_player != null)
                _player.detachSurface();
        }
    };

    @Override
    public void eventHardwareAccelerationError() {
        Log.e(TAG, "eventHardwareAccelerationError()!");
        return;
    }
}
