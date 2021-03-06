package com.kk.imgod.zxing_lib.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.kk.imgod.zxing_lib.R;
import com.kk.imgod.zxing_lib.camera.CameraManager;
import com.kk.imgod.zxing_lib.decoding.CaptureActivityHandler;
import com.kk.imgod.zxing_lib.decoding.DecodeImage;
import com.kk.imgod.zxing_lib.decoding.InactivityTimer;
import com.kk.imgod.zxing_lib.utils.ImageUtils;
import com.kk.imgod.zxing_lib.utils.StatusBarUtil;
import com.kk.imgod.zxing_lib.view.ViewfinderView;

import java.io.IOException;
import java.util.Vector;

/**
 * 解析二维码的界面
 * 如果扫描到了二维码 会返回给上个界面 key是此类的常量 RESULT_KEY
 * 如果没有扫描到二维码 会返回请求失败的code给上个界面 code为 RESULT_CODE_NOT_FIND_QR
 * 可以依赖下来之后自己修改界面什么的 使之更符合你自己app的主题
 * 注意点:
 * 因为这就是加一个二维码扫描lib而已 所以并没有对android 6.0+上的动态权限做适配
 * 需要用户在自己的项目中 去适配动态权限申请
 */
public class CaptureActivity extends AppCompatActivity implements Callback {
    public static final int REQUEST_CODE_TAKE_PHOTO_FROM_ALBUM = 0x00;//获取相册图片的请求码
    public static final int RESULT_CODE_NOT_FIND_QR = -0x9999;//没有发现二维码的结果标志位
    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private ImageView img_flash;
    private boolean hasSurface;
    private Vector<BarcodeFormat> decodeFormats;
    private String characterSet;
    private InactivityTimer inactivityTimer;
    private MediaPlayer mediaPlayer;
    private boolean playBeep;
    private static final float BEEP_VOLUME = 0.10f;
    private boolean vibrate;
    private boolean isFlashOn = false;

    private Camera camera;

    /**
     * 快速跳转方法
     *
     * @param activity    源Activity
     * @param requestCode 请求码
     */
    public static void actionStartForResult(Activity activity, int requestCode) {
        actionStartForResult(activity, requestCode, COME_TYPE_LIGHT);
    }

    /**
     * 快速跳转方法
     *
     * @param fragment    源fragment
     * @param requestCode 请求码
     */
    public static void actionStartForResult(Fragment fragment, int requestCode) {
        actionStartForResult(fragment, requestCode, COME_TYPE_LIGHT);
    }


    public static final int COME_TYPE_DARK = 0x00;
    public static final int COME_TYPE_LIGHT = 0x01;
    private int comeType;

    /**
     * 快速跳转方法
     *
     * @param activity    源Activity
     * @param requestCode 请求码
     */
    public static void actionStartForResult(Activity activity, int requestCode, int comeType) {
        Intent intent = new Intent(activity, CaptureActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("comeType", comeType);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 快速跳转方法
     *
     * @param fragment    源fragment
     * @param requestCode 请求码
     */
    public static void actionStartForResult(Fragment fragment, int requestCode, int comeType) {
        Intent intent = new Intent(fragment.getContext(), CaptureActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("comeType", comeType);
        fragment.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        comeType = getIntent().getIntExtra("comeType", COME_TYPE_LIGHT);
        if (comeType == COME_TYPE_LIGHT) {
            StatusBarUtil.setLightMode(this);
        } else {
            StatusBarUtil.setDarkMode(this);
        }

        setContentView(R.layout.layout_qr_capture);
        PackageManager packageManager = getPackageManager();
        if (packageManager.checkPermission(Manifest.permission.CAMERA, getApplicationContext().getPackageName()) == PackageManager.PERMISSION_GRANTED) {
            initView();
        } else {
            Toast.makeText(this, "没有相机权限", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    public void initView() {
        //ViewUtil.addTopView(getApplicationContext(), this, R.string.scan_card);
        CameraManager.init(getApplication());
        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        img_flash = (ImageView) findViewById(R.id.img_flash);
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        initTitle();
        img_flash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFlashOn = !isFlashOn;
                Camera.Parameters parameter = camera.getParameters();
                // TODO 开灯
                if (isFlashOn) {
                    parameter.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(parameter);
                } else {  // 关灯
                    parameter.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(parameter);
                }
            }
        });
    }

    private Toolbar toolbar;

    /**
     * 初始化标题栏
     */
    private void initTitle() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("扫描二维码");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            onBackPressed();
        } else if (i == R.id.menu_photo) {
            ImageUtils.takePhotoFromAbnum(CaptureActivity.this, REQUEST_CODE_TAKE_PHOTO_FROM_ALBUM);
        } else {

        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.zxing_lib_menu, menu);
        return true;
    }


    @Override
    protected void onResume() {
        super.onResume();
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        decodeFormats = null;
        characterSet = null;

        playBeep = true;
        AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false;
        }
        initBeepSound();
        vibrate = true;

    }

    //从相册选择一个图片并解析的结果处理
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_TAKE_PHOTO_FROM_ALBUM) {
            if (resultCode == RESULT_OK) {
                String path = ImageUtils.getImageAbsolutePath(CaptureActivity.this, data.getData());
                Log.e("onActivityResult", "path-->" + path);
                handleDecode(DecodeImage.readImage(path), null);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        CameraManager.get().closeDriver();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    public static final String RESULT_KEY = "QrResult";//intent 传递结果的key

    /**
     * 处理二维码结果
     *
     * @param result  结果字符串
     * @param barcode 结果bitmap
     */
    public void handleDecode(Result result, Bitmap barcode) {
        inactivityTimer.onActivity();
        playBeepSoundAndVibrate();
        Intent intent = new Intent();
        if (result != null) {
            String resultString = result.getText();
            intent.putExtra(RESULT_KEY, resultString);
            setResult(Activity.RESULT_OK, intent);
        } else {
            setResult(RESULT_CODE_NOT_FIND_QR);
        }
        finish();
    }


    /**
     * 初始化camera
     */
    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);
        } catch (Exception e) {
            return;
        }
        if (handler == null) {
            handler = new CaptureActivityHandler(this, decodeFormats,
                    characterSet);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
            camera = CameraManager.getCamera();
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
        if (camera != null) {
            CameraManager.get().stopPreview();
        }
    }


    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();

    }

    private void initBeepSound() {
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(beepListener);

            AssetFileDescriptor file = getResources().openRawResourceFd(
                    R.raw.beep);
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
                file.close();
                mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                mediaPlayer.prepare();
            } catch (IOException e) {
                mediaPlayer = null;
            }
        }
    }

    private static final long VIBRATE_DURATION = 200L;

    private void playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }
        if (vibrate) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private final OnCompletionListener beepListener = new OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };

}