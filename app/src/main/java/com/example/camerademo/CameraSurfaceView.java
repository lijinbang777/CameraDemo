package com.example.camerademo;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;

import java.util.List;

public  class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private static final int ORIENTATION = 90;
    private int mScreenWidth;
    private int mScreenHeight;
    private boolean isOpen;

    public CameraSurfaceView(Context context, AttributeSet attributeSet){
        super(context,attributeSet);
        getScreenMatrix(context);
        mHolder = getHolder();
        //实现相对应的接口类
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private  void getScreenMatrix(Context context){
        WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        mScreenHeight = displayMetrics.heightPixels;
        mScreenWidth = displayMetrics.widthPixels;
    }

    public void takePicture(Camera.ShutterCallback mShutterCallback,Camera.PictureCallback rawPictureCallback,Camera.PictureCallback jpegPictureCallback){
        if (mCamera != null){
            mCamera.takePicture(mShutterCallback,rawPictureCallback,jpegPictureCallback);
        }
    }

    public  void startPreview(){
        mCamera.startPreview();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        if (!checkCameraHardware(getContext())) return;
        if (mCamera == null){
           isOpen = safeCameraOpen(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
        if (!isOpen){
            return;
        }
        mCamera.setDisplayOrientation(ORIENTATION);
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        }catch (Exception e){
           e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (mCamera != null){
           setCameraParams(mScreenWidth,mScreenHeight);
           mCamera.startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        releaseCameraAndPreview();
    }

    /**
     * 设置摄像头的分辨率
     * @param width
     * @param height
     */
    private void setCameraParams(int width,int height){
        Camera.Parameters parameters = mCamera.getParameters();

        List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
        Camera.Size pictureSize = getProperSize(pictureSizeList,((float) height/width));
        if (pictureSize == null){
            pictureSize = parameters.getPictureSize();
        }

        //根据选出的picturesize重新设置surfaceview大小
        float w = pictureSize.width;
        float h = pictureSize.height;
        parameters.setPictureSize(pictureSize.width,pictureSize.height);
        this.setLayoutParams(new LinearLayoutCompat.LayoutParams((int) (height *(h/w)),height));
        //this.setLayoutParams(new RelativeLayout.LayoutParams((int) (height *(h/w)),height));

        //获取摄像头支持的Previewsize列表
        List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();
        Camera.Size preSize = getProperSize(previewSizeList,((float) height)/width);
        if (preSize != null){
            parameters.setPreviewSize(preSize.width,preSize.height);
        }
        parameters.setJpegQuality(100);
        if (parameters.getSupportedFocusModes().contains(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 连续对焦模式
        }
        mCamera.setDisplayOrientation(90);
        mCamera.setParameters(parameters);
    }

    /**
     * 选择合适的分辨率
     * @param pictureSizeList
     * @param screenRatio
     * @return
     */
    private Camera.Size getProperSize(List<Camera.Size> pictureSizeList,float screenRatio){
        Camera.Size result = null;

        for (Camera.Size size: pictureSizeList) {
            float currentRatio = ((float) size.width) / size.height;
            if (currentRatio - screenRatio == 0) {
                result = size;
                break;
            }
        }
        if (null == result) {
            for (Camera.Size size : pictureSizeList) {
                float curRatio = ((float) size.width) / size.height;
                if (curRatio == 4f / 3) {// 默认w:h = 4:3
                    result = size;
                    break;
                }
            }
        }
        return  result;
    }
    /**
     * 安全打开摄像头
     * @param id
     * @return
     */
    private boolean safeCameraOpen(int id){
        boolean qOpened = false;
        try {
            releaseCameraAndPreview();
            mCamera = Camera.open(id);
            qOpened = (mCamera != null);
        }catch (Exception e){
            e.printStackTrace();
        }
        return qOpened;
    }

    /**
     * 关闭摄像头资源与预览
     */
    private void releaseCameraAndPreview(){
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
    /**
     * 检查设备硬件是否支持相机
     * @param context
     * @return
     */
    private boolean checkCameraHardware(Context context){
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            return true;
        }else {
            return false;
        }
    }
}
