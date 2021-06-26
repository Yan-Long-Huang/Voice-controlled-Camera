package com.example.voicecamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_EXTERNAL_STORAGE = 10086;
    private static int PREVIEW_WIDTH = 1920;
    private static int PREVIEW_HEIGHT = 1080;
    private ImageView iv_voice, iv_switch_camera, iv_album, iv_flash, iv_cancel;//返回和切換前後置攝像頭
    private SurfaceView surface;
    private ImageView iv_shutter;//快門
    private SurfaceHolder holder;
    private Camera camera;//宣告相機
    private String filepath = "";//照片儲存路徑
    private int cameraPosition = 1;//0代表前置攝像頭，1代表後置攝像頭
    private final static int SPEECH_REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(getSupportActionBar()).hide();//沒有標題

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//設定全屏
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//拍照過程螢幕一直處於高亮
        //設定手機螢幕朝向，一共有7種
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        //SCREEN_ORIENTATION_BEHIND： 繼承Activity堆疊中當前Activity下面的那個Activity的方向
        //SCREEN_ORIENTATION_LANDSCAPE： 橫屏(風景照) ，顯示時寬度大於高度
        //SCREEN_ORIENTATION_PORTRAIT： 豎屏 (肖像照) ， 顯示時高度大於寬度
        //SCREEN_ORIENTATION_SENSOR  由重力感應器來決定螢幕的朝向,它取決於使用者如何持有裝置,當裝置被旋轉時方向會隨之在橫屏與豎屏之間變化
        //SCREEN_ORIENTATION_NOSENSOR： 忽略物理感應器——即顯示方向與物理感應器無關，不管使用者如何旋轉裝置顯示方向都不會隨著改變("unspecified"設定除外)
        //SCREEN_ORIENTATION_UNSPECIFIED： 未指定，此為預設值，由Android系統自己選擇適當的方向，選擇策略視具體裝置的配置情況而定，因此不同的裝置會有不同的方向選擇
        //SCREEN_ORIENTATION_USER： 使用者當前的首選方向

        setContentView(R.layout.activity_main);
        requestPermission();
        initView();
        setListener();

    }

    private void initView() {
        iv_voice = findViewById(R.id.iv_voice);
        iv_switch_camera = findViewById(R.id.iv_switch_camera);
        surface = findViewById(R.id.cp_surface);
        iv_album = findViewById(R.id.iv_album);
        iv_cancel = findViewById(R.id.iv_cancel);
        iv_flash = findViewById(R.id.iv_flash);
        iv_shutter = findViewById(R.id.iv_shutter);
        holder = surface.getHolder();//獲得控制代碼
        holder.addCallback(this);//添加回調
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);//surfaceview不維護自己的緩衝區，等待螢幕渲染引擎將內容推送到使用者面前
    }

    private void setListener() {
        //設定監聽
        iv_voice.setOnClickListener(listener);
        iv_switch_camera.setOnClickListener(listener);
        iv_shutter.setOnClickListener(listener);
        iv_cancel.setOnClickListener(listener);
        iv_album.setOnClickListener(listener);
        iv_flash.setOnClickListener(listener);
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED
                ||ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED
                ||ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.CAMERA}, REQUEST_EXTERNAL_STORAGE);
        }
    }

    //響應點選事件
    View.OnClickListener listener = new View.OnClickListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            Camera.Parameters parameter = camera.getParameters();
            switch (v.getId()) {
                case R.id.iv_voice:
                    //返回
                    //MainActivity.this.finish();
                    displaySpeechRecognizer();
//                    camera.autoFocus(new Camera.AutoFocusCallback() {//自動對焦
//                        @Override
//                        public void onAutoFocus(boolean success, Camera camera) {
//                            // TODO Auto-generated method stub
//                            if(success) {
//                                camera.takePicture(null, null, picture_callback);//將拍攝到的照片給自定義的物件
//                            }
//                        }
//                    });
                    break;
                case R.id.iv_switch_camera:
                    //切換前後攝像頭
                    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                    int cameraCount = Camera.getNumberOfCameras();//得到攝像頭的個數
                    for(int i = 0; i < cameraCount; i++) {
                        Camera.getCameraInfo(i, cameraInfo);//得到每一個攝像頭的資訊
                        if(cameraPosition == 1) {
                            //現在是後置，變更為前置
                            if(cameraInfo.facing  == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表攝像頭的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK後置
                                camera.stopPreview();//停掉原來攝像頭的預覽
                                camera.release();//釋放資源
                                camera = null;//取消原來攝像頭
                                camera = Camera.open(i);//開啟當前選中的攝像頭
                                try {
                                    camera.setPreviewDisplay(holder);//通過surfaceview顯示取景畫面
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                camera.startPreview();//開始預覽
                                cameraPosition = 0;
                                break;
                            }
                        } else {
                            //現在是前置， 變更為後置
                            if(cameraInfo.facing  == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表攝像頭的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK後置
                                camera.stopPreview();//停掉原來攝像頭的預覽
                                camera.release();//釋放資源
                                camera = null;//取消原來攝像頭
                                camera = Camera.open(i);//開啟當前選中的攝像頭
                                try {
                                    camera.setPreviewDisplay(holder);//通過surfaceview顯示取景畫面
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                camera.startPreview();//開始預覽
                                cameraPosition = 1;
                                break;
                            }
                        }
                    }
                    break;
                case R.id.iv_shutter: //快門
                    //自動對焦
                    camera.autoFocus((success, camera) -> {
                        // TODO Auto-generated method stub
                        if(success) {
                            camera.takePicture(null, null, picture_callback);//將拍攝到的照片給自定義的物件
                        }
                    });
                    break;
                case R.id.iv_cancel:
                    //閃光燈關閉變開啟
                    //camera.setParameters();
                    //camera.getParameters().setFlashMode(Camera.Parameters.FLASH_MODE_ON);

                    parameter.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                    camera.setParameters(parameter);
                    camera.startPreview();
                    //camera.setParameters(camera.getParameters());
                    Toast.makeText(MainActivity.this, "閃光燈已打開", Toast.LENGTH_SHORT).show();

                    iv_cancel.setVisibility(View.INVISIBLE);
                    break;
                case R.id.iv_flash:
                    //閃光燈開啟變關閉
//                    camera.getParameters().setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
//                    camera.setParameters(camera.getParameters());
                    Toast.makeText(MainActivity.this, "閃光燈已關閉", Toast.LENGTH_SHORT).show();

                    parameter.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(parameter);
                    camera.startPreview();

                    iv_cancel.setVisibility(View.VISIBLE);
                    break;
                case R.id.iv_album: // 跳轉到相簿.
                    startActivity(new Intent(Intent.ACTION_VIEW, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + v.getId());
            }
        }
    };
    /*surfaceHolder他是系統提供的一個用來設定surfaceView的一個物件，而它通過surfaceView.getHolder()這個方法來獲得。
         Camera提供一個setPreviewDisplay(SurfaceHolder)的方法來連線*/
    //SurfaceHolder.Callback,這是個holder用來顯示surfaceView 資料的介面,他必須實現以下3個方法
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub
        if (holder.getSurface() == null) {
            return;
        }
        try {
            camera.stopPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
//當surfaceview建立時開啟相機
        if(camera == null) {
            camera = Camera.open();
            try {
                //設定引數，開始預覽
                Camera.Parameters params = camera.getParameters();
                params.setPictureFormat(PixelFormat.JPEG);//圖片格式
//                params.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);//預覽
//                params.setPictureSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);//圖片大小
                params.setJpegQuality(100);
                camera.setParameters(params);//將引數設定到我的camera
                camera.setPreviewDisplay(holder);//通過surfaceview顯示取景畫面
                camera.startPreview();//開始預覽
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
//當surfaceview關閉時，關閉預覽並釋放資源
        camera.stopPreview();
        camera.release();
        camera = null;
        surface = null;
    }

    //建立jpeg圖片回撥資料物件
    @SuppressLint("SimpleDateFormat")
    Camera.PictureCallback picture_callback = (data, camera) -> {
        //將儲存圖片的放到子執行緒中去，別影響主執行緒
        new Thread(() -> {
            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//自定義檔案儲存路徑  以拍攝時間區分命名
                filepath = "/storage/emulated/0/DCIM/Camera/"+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+ ".jpg";
                final File file = new File(filepath); // file.exists();
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);//將圖片壓縮的流裡面
                bos.flush();// 重新整理此緩衝區的輸出流
                bos.close();// 關閉此輸出流並釋放與此流有關的所有系統資源
                bitmap.recycle();//回收bitmap空間
                runOnUiThread(() -> {
                    try {  //圖片插入到系統圖庫中
                        MediaStore.Images.Media.insertImage(getContentResolver(), filepath, file.getName(), null);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(MainActivity.this, "照片儲存成功" + filepath, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }).start();
        camera.stopPreview();//關閉預覽 處理資料
        camera.startPreview();//資料處理完後繼續開始預覽
    };
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && requestCode == REQUEST_EXTERNAL_STORAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this, "ok", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MainActivity.this, "not ok", Toast.LENGTH_LONG).show();
        }
    }

    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {

            assert data != null;
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            Toast.makeText(this, spokenText, Toast.LENGTH_SHORT).show();
            if (spokenText.contains("拍照")) {
                Toast.makeText(MainActivity.this, "請稍等", Toast.LENGTH_SHORT).show();
                //自動對焦
                camera.autoFocus((success, camera) -> {
                    // TODO Auto-generated method stub
                    if(success) {
                        camera.takePicture(null, null, picture_callback);//將拍攝到的照片給自定義的物件
                    }
                });
            }
            else{
                Toast.makeText(MainActivity.this, "請說拍照", Toast.LENGTH_SHORT).show();
                displaySpeechRecognizer();
            }
        }
    }
}