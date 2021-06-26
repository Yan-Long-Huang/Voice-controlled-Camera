package com.example.camerax;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.CameraController;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static android.content.ContentValues.TAG;
import static androidx.camera.core.ImageCapture.FLASH_MODE_AUTO;
import static androidx.camera.core.ImageCapture.FLASH_MODE_OFF;
import static androidx.camera.core.ImageCapture.FLASH_MODE_ON;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {
    private static PreviewView previewView;
    private static ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageButton ib_listening, ib_flash, ib_flip_camera, ib_shutter, ib_exit, ib_gallery;
    private final static int SPEECH_REQUEST_CODE = 0;
    private static ImageCapture imageCapture;
    private static CameraSelector cameraSelector;
    private static ExecutorService cameraExecutor;
    private SurfaceHolder holder;
    private Camera camera;//宣告相機
    private int cameraPosition = 1;//0代表前置攝像頭，1代表後置攝像頭

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide(); //隱藏 AppBar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); //設定全屏
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//拍照時讓螢幕處於恆亮狀態

        initView(); //連結 UI
        setListener(); //設定監聽
        startCamera(); //啟用相機
    }

    private void initView() { //連結 UI
        ib_listening = findViewById(R.id.ib_listening);
        ib_flash = findViewById(R.id.ib_flash);
        ib_flip_camera = findViewById(R.id.ib_flip_camera);
        ib_shutter = findViewById(R.id.ib_shutter);
        ib_exit = findViewById(R.id.ib_exit);
        ib_gallery = findViewById(R.id.ib_gallery);
    }

    private void setListener() { //設定監聽
        ib_listening.setOnClickListener(this);
        ib_flash.setOnClickListener(this);
        ib_flip_camera.setOnClickListener(this);
        ib_shutter.setOnClickListener(this);
        ib_exit.setOnClickListener(this);
        ib_gallery.setOnClickListener(this);
    }

    public void startCamera() { //啟用相機
        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindImageAnalysis(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build(); //預覽配置:預覽用於相機預覽的時候顯示預覽畫面
        preview.setSurfaceProvider(previewView.getSurfaceProvider()); //SurfaceProvider提供拍攝影像的預覽
        imageCapture = new ImageCapture.Builder().build(); //照相配置:ImageCapture用於拍照,並將圖片保存
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);
    }

    public void takePhoto() {

        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "DCIM"
                + File.separator + "Camera" + File.separator + "IMG_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg";
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(new File(filePath)).build();
        imageCapture.takePicture(outputFileOptions, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        // insert your code here.
                        Toast.makeText(CameraActivity.this, "照片儲存成功" + filePath, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(ImageCaptureException error) {
                        // insert your code here.
                    }
                }
        );
        Toast.makeText(CameraActivity.this, "照片儲存成功" + filePath, Toast.LENGTH_SHORT).show();
    }

    //将前面保存的文件添加到媒体中
    private void onFileSaved(Uri savedUri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            sendBroadcast(new Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri));
        }
        String mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap
                .getFileExtensionFromUrl(savedUri.getPath()));
        MediaScannerConnection.scanFile(getApplicationContext(),
                new String[]{new File(savedUri.getPath()).getAbsolutePath()},
                new String[]{mimeTypeFromExtension}, new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        Log.d(TAG, "Image capture scanned into media store: $uri" + uri);
                    }
                });
//            PreviewActivity.start(this, outputFilePath, !takingPicture);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ib_listening:
                displaySpeechRecognizer();
                break;
            case R.id.ib_shutter:
                takePhoto();
                break;
            case R.id.ib_flash:
                flash_swap();
                break;
            case R.id.ib_exit:
                finishAffinity();
                break;
            case R.id.ib_flip_camera:
                switchCamera();
                break;
        }
    }

    private void flash_swap() {
        switch (imageCapture.getFlashMode()) {
            case FLASH_MODE_AUTO:
                ib_flash.setImageResource(R.drawable.ic_flash_off);
                imageCapture.setFlashMode(FLASH_MODE_OFF);
                break;
            case FLASH_MODE_ON:
                ib_flash.setImageResource(R.drawable.ic_flash_auto);
                imageCapture.setFlashMode(FLASH_MODE_AUTO);
                break;
            case FLASH_MODE_OFF:
                ib_flash.setImageResource(R.drawable.ic_flash_on);
                imageCapture.setFlashMode(FLASH_MODE_ON);
                break;
        }
    }

    private void switchCamera() {
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
                Toast.makeText(CameraActivity.this, "請稍等", Toast.LENGTH_SHORT).show();
                //預設自動對焦拍照
                takePhoto();
            } else {
                Toast.makeText(CameraActivity.this, "請說拍照", Toast.LENGTH_SHORT).show();
                displaySpeechRecognizer();
            }
        }
    }
}