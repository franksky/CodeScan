package com.frank.codescan;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import com.frank.codescan.camera.CameraManager;
import com.frank.codescan.common.BitmapUtils;
import com.frank.codescan.decode.BitmapDecoder;
import com.frank.codescan.decode.CaptureActivityHandler;
import com.frank.codescan.decode.DecodeFormatManager;
import com.frank.codescan.view.ViewfinderView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.client.result.ResultParser;

/**
 * This activity opens the camera and does the actual scanning on a background
 * thread. It draws a viewfinder to help the user place the barcode correctly,
 * shows feedback as the image processing is happening, and then overlays the
 * results when a scan is successful.
 * 
 * 姝ctivity鎵�鍋氱殑浜嬶細 1.寮�鍚痗amera锛屽湪鍚庡彴鐙珛绾跨▼涓畬鎴愭壂鎻忎换鍔★紱
 * 2.缁樺埗浜嗕竴涓壂鎻忓尯锛坴iewfinder锛夋潵甯姪鐢ㄦ埛灏嗘潯鐮佺疆浜庡叾涓互鍑嗙‘鎵弿锛� 3.鎵弿鎴愬姛鍚庝細灏嗘壂鎻忕粨鏋滃睍绀哄湪鐣岄潰涓娿��
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CaptureActivity extends Activity implements
        SurfaceHolder.Callback, View.OnClickListener {

    private static final String TAG = CaptureActivity.class.getSimpleName();

    private static final int REQUEST_CODE = 100;

    private static final int PARSE_BARCODE_FAIL = 300;
    private static final int PARSE_BARCODE_SUC = 200;

    /**
     * 鏄惁鏈夐瑙�
     */
    private boolean hasSurface;

    /**
     * 娲诲姩鐩戞帶鍣ㄣ�傚鏋滄墜鏈烘病鏈夎繛鎺ョ數婧愮嚎锛岄偅涔堝綋鐩告満寮�鍚悗濡傛灉涓�鐩村浜庝笉琚娇鐢ㄧ姸鎬佸垯璇ユ湇鍔′細灏嗗綋鍓峚ctivity鍏抽棴銆�
     * 娲诲姩鐩戞帶鍣ㄥ叏绋嬬洃鎺ф壂鎻忔椿璺冪姸鎬侊紝涓嶤aptureActivity鐢熷懡鍛ㄦ湡鐩稿悓.姣忎竴娆℃壂鎻忚繃鍚庨兘浼氶噸缃鐩戞帶锛屽嵆閲嶆柊鍊掕鏃躲��
     */
    private InactivityTimer inactivityTimer;

    /**
     * 澹伴煶闇囧姩绠＄悊鍣ㄣ�傚鏋滄壂鎻忔垚鍔熷悗鍙互鎾斁涓�娈甸煶棰戯紝涔熷彲浠ラ渿鍔ㄦ彁閱掞紝鍙互閫氳繃閰嶇疆鏉ュ喅瀹氭壂鎻忔垚鍔熷悗鐨勮涓恒��
     */
    private BeepManager beepManager;

    /**
     * 闂厜鐏皟鑺傚櫒銆傝嚜鍔ㄦ娴嬬幆澧冨厜绾垮己寮卞苟鍐冲畾鏄惁寮�鍚棯鍏夌伅
     */
    private AmbientLightManager ambientLightManager;

    private CameraManager cameraManager;
    /**
     * 鎵弿鍖哄煙
     */
    private ViewfinderView viewfinderView;

    private CaptureActivityHandler handler;

    private Result lastResult;

    private boolean isFlashlightOpen;

    /**
     * 銆愯緟鍔╄В鐮佺殑鍙傛暟(鐢ㄤ綔MultiFormatReader鐨勫弬鏁�)銆� 缂栫爜绫诲瀷锛岃鍙傛暟鍛婅瘔鎵弿鍣ㄩ噰鐢ㄤ綍绉嶇紪鐮佹柟寮忚В鐮侊紝鍗矱AN-13锛孮R
     * Code绛夌瓑 瀵瑰簲浜嶥ecodeHintType.POSSIBLE_FORMATS绫诲瀷
     * 鍙傝�僁ecodeThread鏋勯�犲嚱鏁颁腑濡備笅浠ｇ爜锛歨ints.put(DecodeHintType.POSSIBLE_FORMATS,
     * decodeFormats);
     */
    private Collection<BarcodeFormat> decodeFormats;

    /**
     * 銆愯緟鍔╄В鐮佺殑鍙傛暟(鐢ㄤ綔MultiFormatReader鐨勫弬鏁�)銆� 璇ュ弬鏁版渶缁堜細浼犲叆MultiFormatReader锛�
     * 涓婇潰鐨刣ecodeFormats鍜宑haracterSet鏈�缁堜細鍏堝姞鍏ュ埌decodeHints涓� 鏈�缁堣璁剧疆鍒癕ultiFormatReader涓�
     * 鍙傝�僁ecodeHandler鏋勯�犲櫒涓涓嬩唬鐮侊細multiFormatReader.setHints(hints);
     */
    private Map<DecodeHintType, ?> decodeHints;

    /**
     * 銆愯緟鍔╄В鐮佺殑鍙傛暟(鐢ㄤ綔MultiFormatReader鐨勫弬鏁�)銆� 瀛楃闆嗭紝鍛婅瘔鎵弿鍣ㄨ浠ヤ綍绉嶅瓧绗﹂泦杩涜瑙ｇ爜
     * 瀵瑰簲浜嶥ecodeHintType.CHARACTER_SET绫诲瀷
     * 鍙傝�僁ecodeThread鏋勯�犲櫒濡備笅浠ｇ爜锛歨ints.put(DecodeHintType.CHARACTER_SET,
     * characterSet);
     */
    private String characterSet;

    private Result savedResultToShow;

    private IntentSource source;

    /**
     * 鍥剧墖鐨勮矾寰�
     */
    private String photoPath;

    private Handler mHandler = new MyHandler(this);

    static class MyHandler extends Handler {

        private WeakReference<Activity> activityReference;

        public MyHandler(Activity activity) {
            activityReference = new WeakReference<Activity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case PARSE_BARCODE_SUC: // 瑙ｆ瀽鍥剧墖鎴愬姛
                    Toast.makeText(activityReference.get(),
                            "瑙ｆ瀽鎴愬姛锛岀粨鏋滀负锛�" + msg.obj, Toast.LENGTH_SHORT).show();
                    break;

                case PARSE_BARCODE_FAIL:// 瑙ｆ瀽鍥剧墖澶辫触

                    Toast.makeText(activityReference.get(), "瑙ｆ瀽鍥剧墖澶辫触",
                            Toast.LENGTH_SHORT).show();
                    break;

                default:
                    break;
            }

            super.handleMessage(msg);
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_capture);
        // 璁剧疆鎵弿浜岀淮鐮佹ā寮�
        decodeFormats = new Vector<BarcodeFormat>(2);
        decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
        // 璁剧疆璇嗗埆鐨勫瓧绗︾紪鐮佷负UTF8
        characterSet = "UTF8";

        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);
        ambientLightManager = new AmbientLightManager(this);

        // 鐩戝惉鍥剧墖璇嗗埆鎸夐挳
        findViewById(R.id.button_back).setOnClickListener(this);

        findViewById(R.id.capture_flashlight).setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        // CameraManager must be initialized here, not in onCreate(). This is
        // necessary because we don't
        // want to open the camera driver and measure the screen size if we're
        // going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the
        // wrong size and partially
        // off screen.

        // 鐩告満鍒濆鍖栫殑鍔ㄤ綔闇�瑕佸紑鍚浉鏈哄苟娴嬮噺灞忓箷澶у皬锛岃繖浜涙搷浣�
        // 涓嶅缓璁斁鍒皁nCreate涓紝鍥犱负濡傛灉鍦╫nCreate涓姞涓婇娆″惎鍔ㄥ睍绀哄府鍔╀俊鎭殑浠ｇ爜鐨� 璇濓紝
        // 浼氬鑷存壂鎻忕獥鍙ｇ殑灏哄璁＄畻鏈夎鐨刡ug
        cameraManager = new CameraManager(getApplication());

        viewfinderView = (ViewfinderView) findViewById(R.id.capture_viewfinder_view);
        viewfinderView.setCameraManager(cameraManager);

        handler = null;
        lastResult = null;

        // 鎽勫儚澶撮瑙堝姛鑳藉繀椤诲�熷姪SurfaceView锛屽洜姝や篃闇�瑕佸湪涓�寮�濮嬪鍏惰繘琛屽垵濮嬪寲
        // 濡傛灉闇�瑕佷簡瑙urfaceView鐨勫師鐞�
        // 鍙傝��:http://blog.csdn.net/luoshengyang/article/details/8661317
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.capture_preview_view); // 棰勮
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still
            // exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);

        } else {
            // 闃叉sdk8鐨勮澶囧垵濮嬪寲棰勮寮傚父
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

            // Install the callback and wait for surfaceCreated() to init the
            // camera.
            surfaceHolder.addCallback(this);
        }

        // 鍔犺浇澹伴煶閰嶇疆锛屽叾瀹炲湪BeemManager鐨勬瀯閫犲櫒涓篃浼氳皟鐢ㄨ鏂规硶锛屽嵆鍦╫nCreate鐨勬椂鍊欎細璋冪敤涓�娆�
        beepManager.updatePrefs();

        // 鍚姩闂厜鐏皟鑺傚櫒
        ambientLightManager.start(cameraManager);

        // 鎭㈠娲诲姩鐩戞帶鍣�
        inactivityTimer.onResume();

        source = IntentSource.NONE;
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        beepManager.close();

        // 鍏抽棴鎽勫儚澶�
        cameraManager.closeDriver();
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.capture_preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if ((source == IntentSource.NONE) && lastResult != null) { // 閲嶆柊杩涜鎵弿
                    restartPreviewAfterDelay(0L);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;

            case KeyEvent.KEYCODE_VOLUME_UP:
                cameraManager.zoomIn();
                return true;

            case KeyEvent.KEYCODE_VOLUME_DOWN:
                cameraManager.zoomOut();
                return true;

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (resultCode == RESULT_OK) {
            final ProgressDialog progressDialog;
            switch (requestCode) {
                case REQUEST_CODE:

                    // 鑾峰彇閫変腑鍥剧墖鐨勮矾寰�
                    Cursor cursor = getContentResolver().query(
                            intent.getData(), null, null, null, null);
                    if (cursor.moveToFirst()) {
                        photoPath = cursor.getString(cursor
                                .getColumnIndex(MediaStore.Images.Media.DATA));
                    }
                    cursor.close();

                    progressDialog = new ProgressDialog(this);
                    progressDialog.setMessage("姝ｅ湪鎵弿...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    new Thread(new Runnable() {

                        @Override
                        public void run() {

                            Bitmap img = BitmapUtils
                                    .getCompressedBitmap(photoPath);

                            BitmapDecoder decoder = new BitmapDecoder(
                                    CaptureActivity.this);
                            Result result = decoder.getRawResult(img);

                            if (result != null) {
                                Message m = mHandler.obtainMessage();
                                m.what = PARSE_BARCODE_SUC;
                                m.obj = ResultParser.parseResult(result)
                                        .toString();
                                mHandler.sendMessage(m);
                            } else {
                                Message m = mHandler.obtainMessage();
                                m.what = PARSE_BARCODE_FAIL;
                                mHandler.sendMessage(m);
                            }

                            progressDialog.dismiss();

                        }
                    }).start();

                    break;

            }
        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG,
                    "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        /* hasSurface = false; */
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    /**
     * A valid barcode has been found, so give an indication of success and show
     * the results.
     * 
     * @param rawResult
     *            The contents of the barcode.
     * @param scaleFactor
     *            amount by which thumbnail was scaled
     * @param barcode
     *            A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {

        // 閲嶆柊璁℃椂
        inactivityTimer.onActivity();

        lastResult = rawResult;

        beepManager.playBeepSoundAndVibrate();

        String resultString = rawResult.getText();
        if (resultString.equals("")) {
            Toast.makeText(CaptureActivity.this, "Scan failed!",
                    Toast.LENGTH_SHORT).show();
        } else {
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString("result", resultString);
            resultIntent.putExtras(bundle);
            this.setResult(RESULT_OK, resultIntent);
        }
        CaptureActivity.this.finish();
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
        resetStatusView();
    }

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    private void resetStatusView() {
        viewfinderView.setVisibility(View.VISIBLE);
        lastResult = null;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }

        if (cameraManager.isOpen()) {
            Log.w(TAG,
                    "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a
            // RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, decodeFormats,
                        decodeHints, characterSet, cameraManager);
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    /**
     * 鍚慍aptureActivityHandler涓彂閫佹秷鎭紝骞跺睍绀烘壂鎻忓埌鐨勫浘鍍�
     * 
     * @param bitmap
     * @param result
     */
    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler,
                        R.id.decode_succeeded, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
        builder.setOnCancelListener(new FinishListener(this));
        builder.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_back: // 鍥剧墖璇嗗埆
                CaptureActivity.this.finish();
                break;

            case R.id.capture_flashlight:
                if (isFlashlightOpen) {
                    cameraManager.setTorch(false); // 鍏抽棴闂厜鐏�
                    isFlashlightOpen = false;
                } else {
                    cameraManager.setTorch(true); // 鎵撳紑闂厜鐏�
                    isFlashlightOpen = true;
                }
                break;
            default:
                break;
        }

    }

}
