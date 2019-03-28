package com.zer0.possessor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ZCamera
{
	private ZRuntime _zRuntime;
    private UIWebView _appView;

    private interface CameraImplementation
    {
        public void attach(ZRuntime zRuntime, UIWebView appView) throws Exception;
        public void detach() throws Exception;
        public boolean isInited();
        boolean takePicture(File imageFile);
    }

    private CameraImplementation _camera = null;

    private static class CameraOld implements CameraImplementation
    {
        private Camera _camera;
        private CameraSurfaceView _surfaceView;
        private PictureCallback _picture;
        private CountDownLatch _latch;
        private File _imageFile;
        private UIWebView _appView;
        private boolean _attached;

        public CameraOld()
        {
            _surfaceView = null;
            _camera = null;
            _imageFile = null;
            _attached = false;
        }

        public boolean isInited()
        {
            return _attached;
        }

        public void attach(ZRuntime zRuntime, UIWebView appView) throws Exception
        {
            if (!_attached) {
                int numCam = findFrontFacingCamera();
                if (numCam == -1) {
                    numCam = findBackFacingCamera();
                }
                _camera = Camera.open(numCam);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    _camera.enableShutterSound(false);
                }
                _picture = getPictureCallback();

                _appView = appView;
                _surfaceView = new CameraSurfaceView(zRuntime.getContext());
                _appView.addView(_surfaceView);

                refreshCamera(_camera);

                _attached = true;
            }
        }

        public void detach() throws Exception
        {
            if (_attached) {
                _attached = false;
                try {
                    if (_surfaceView != null) {
                        _appView.removeView(_surfaceView);
                    }

                    if (_camera != null) {
                        _camera.release();
                        _camera = null;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private int findFrontFacingCamera()
        {
            int cameraId = -1;
            // Search for the front facing camera
            int numberOfCameras = Camera.getNumberOfCameras();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    cameraId = i;
                    break;
                }
            }
            return cameraId;
        }

        private int findBackFacingCamera()
        {
            int cameraId = -1;
            //Search for the back facing camera
            //get the number of cameras
            int numberOfCameras = Camera.getNumberOfCameras();
            //for every camera check
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraId = i;
                    //_cameraFront = false;
                    break;
                }
            }
            return cameraId;
        }


        public void refreshCamera(Camera camera) {
            try {
                if (_surfaceView._holder.getSurface() == null || camera == null) {
                    // preview surface does not exist
                    return;
                }
                // stop preview before making changes
                _camera.stopPreview();
            }
            catch (Exception e) {
                e.printStackTrace();
                // ignore: tried to stop a non-existent preview
            }
            // set preview size and make any resize, rotate or
            // reformatting changes here
            // start preview with new settings
            _camera = camera;
            try {
                _camera.setPreviewDisplay(_surfaceView._holder);
                _camera.startPreview();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        private class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback
        {
            private SurfaceHolder _holder;

            CameraSurfaceView(Context ctx)
            {
                super(ctx);
                _holder = getHolder();
                _holder.addCallback(this);
                ViewGroup.LayoutParams lp = getLayoutParams();
                if (lp == null) {
                    lp = new ViewGroup.LayoutParams(1, 1);
                    setLayoutParams(lp);
                }
                else {
                    lp.width = lp.height = 1;
                }
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (_camera != null) {
                        _camera.setPreviewDisplay(holder);
                        _camera.startPreview();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
            {
                // If your preview can change or rotate, take care of those events here.
                // Make sure to stop the preview before resizing or reformatting it.
                try {
                    if (_camera != null) {
                        refreshCamera(_camera);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder)
            {
                try {
                    if (_camera != null) {
                        _camera.release();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private PictureCallback getPictureCallback() {
            final CameraOld me = this;
            return new PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    try {
                        FileOutputStream fos = new FileOutputStream(_imageFile);
                        fos.write(data);
                        fos.close();

                        //refresh camera to continue preview
                        me.refreshCamera(me._camera);
                        me._latch.countDown();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        }

        public boolean takePicture(File imageFile)
        {
            boolean ret;
            try {
                _imageFile = imageFile;
                _latch = new CountDownLatch(1);
                LockActivity a = LockActivity.getInstance();
                if (a != null) {
                    final CameraOld me = this;
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (me._attached) {
                                    me._camera.takePicture(null, null, _picture);
                                }
                                else {
                                    _latch.countDown();
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

                ret = _latch.await(30, TimeUnit.SECONDS);
            }
            catch (Exception e) {
                e.printStackTrace();
                ret = false;
            }

            return ret;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static class CameraLollipop implements CameraImplementation
    {
        private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

        static {
            ORIENTATIONS.append(Surface.ROTATION_0, 90);
            ORIENTATIONS.append(Surface.ROTATION_90, 0);
            ORIENTATIONS.append(Surface.ROTATION_180, 270);
            ORIENTATIONS.append(Surface.ROTATION_270, 180);
        }

        // Camera state: Showing camera preview.
        private static final int STATE_PREVIEW = 0;
        // Camera state: Waiting for the focus to be locked.
        private static final int STATE_WAITING_LOCK = 1;
        // Camera state: Waiting for the exposure to be precapture state.
        private static final int STATE_WAITING_PRECAPTURE = 2;
        // Camera state: Waiting for the exposure state to be something other than precapture.
        private static final int STATE_WAITING_NON_PRECAPTURE = 3;
        // Camera state: Picture was taken.
        private static final int STATE_PICTURE_TAKEN = 4;

        private UIWebView _appView;
        private boolean _isInited;
        private boolean _attached = false;

        private CountDownLatch _latch;
        private File _imageFile;
        private String mCameraId;
        private Size mPreviewSize;
        private CameraDevice _cameraDevice;
        private CameraCaptureSession _captureSession;
        private AutoFitTextureView _textureView;
        private CaptureRequest.Builder _previewRequestBuilder;
        private ImageReader _imageReader;
        private CaptureRequest _previewRequest;
        private Semaphore _cameraOpenCloseLock = new Semaphore(1);
        private int mState = STATE_PREVIEW;
        private Handler _backgroundHandler;
        private HandlerThread _backgroundThread;
        private final CameraDevice.StateCallback _stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice cameraDevice) {
                // This method is called when the camera is opened.  We start camera preview here.
                _cameraOpenCloseLock.release();
                _cameraDevice = cameraDevice;
                createCameraPreviewSession();
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onDisconnected(CameraDevice cameraDevice) {
                _cameraOpenCloseLock.release();
                cameraDevice.close();
                _cameraDevice = null;
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onError(CameraDevice cameraDevice, int error) {
                _cameraOpenCloseLock.release();
                cameraDevice.close();
                _cameraDevice = null;

                LockActivity a = LockActivity.getInstance();
                if (a != null) {
                    a.destroyCamera();
                }
            }
        };
        private final ImageReader.OnImageAvailableListener _onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
            @TargetApi(Build.VERSION_CODES.KITKAT)
            @Override
            public void onImageAvailable(ImageReader reader) {
                _backgroundHandler.post(new ImageSaver(_latch, reader.acquireNextImage(), _imageFile));
            }

        };

        private final CameraCaptureSession.CaptureCallback _captureCallback = new CameraCaptureSession.CaptureCallback() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            private void process(CaptureResult result) {
                switch (mState) {
                    case STATE_PREVIEW: {
                        // We have nothing to do when the camera preview is working normally.
                        _isInited = true;
                        break;
                    }
                    case STATE_WAITING_LOCK: {
                        //int afState = result.get(CaptureResult.CONTROL_AF_STATE);
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_WAITING_NON_PRECAPTURE;
                            captureStillPicture();
                        }
                        else {
                            runPrecaptureSequence();
                        }
                        //}
                        break;
                    }
                    case STATE_WAITING_PRECAPTURE: {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                            mState = STATE_WAITING_NON_PRECAPTURE;
                        }
                        break;
                    }
                    case STATE_WAITING_NON_PRECAPTURE: {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        }
                        break;
                    }
                }
            }

            @Override
            public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult)
            {
                process(partialResult);
            }

            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result)
            {
                process(result);
            }
        };
        private final TextureView.SurfaceTextureListener _surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
                open(width, height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
                configureTransform(width, height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture texture) {
            }
        };

        public CameraLollipop()
        {
            _textureView = null;
            _imageFile = null;
            _isInited = false;
        }

        public boolean isInited()
        {
            return _isInited;
        }

        static class CompareSizesByArea implements Comparator<Size> {

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public int compare(Size lhs, Size rhs) {
                // We cast here to ensure the multiplications won't overflow
                return Long.signum((long)rhs.getWidth() * rhs.getHeight() - (long)lhs.getWidth() * lhs.getHeight());
            }

        }

        private static class ImageSaver implements Runnable {
            CountDownLatch _latch;
            private final Image mImage;
            private final File mFile;

            public ImageSaver(CountDownLatch latch, Image image, File file) {
                mImage = image;
                mFile = file;
                _latch = latch;
            }

            @TargetApi(Build.VERSION_CODES.KITKAT)
            @Override
            public void run() {
                ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                FileOutputStream output = null;
                try {
                    output = new FileOutputStream(mFile);
                    output.write(bytes);
                    _latch.countDown();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                finally {
                    mImage.close();
                    if (null != output) {
                        try {
                            output.close();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private void setUpCameraOutputs(int width, int height) {
            Activity activity = LockActivity.getInstance();
            CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            try {
                String[] camIds = manager.getCameraIdList();
                String cameraId = camIds[0];
                for (String id : camIds) {
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                    cameraId = id;
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                        break;
                    }
                }

                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                // For still image captures, we use the largest available size.
                List<Size> szs = Arrays.asList(map.getOutputSizes(ImageFormat.JPEG));
                Size smallest = szs.get(0);
                for (Size sz : szs) {
                    if (sz.getWidth() <= 800) {
                        smallest = sz;
                        break;
                    }
                }
                _imageReader = ImageReader.newInstance(smallest.getWidth(), smallest.getHeight(), ImageFormat.JPEG, 2);
                _imageReader.setOnImageAvailableListener(_onImageAvailableListener, _backgroundHandler);

                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, smallest);

                int orientation = _textureView.getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    _textureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    _textureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                mCameraId = cameraId;
            }
            catch (CameraAccessException | NullPointerException e) {
                e.printStackTrace();
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio)
        {
            // Collect the supported resolutions that are at least as big as the preview Surface
            List<Size> bigEnough = new ArrayList<>();
            int w = aspectRatio.getWidth();
            int h = aspectRatio.getHeight();
            for (Size option : choices) {
                if (option.getHeight() == option.getWidth() * h / w && option.getWidth() >= width && option.getHeight() >= height) {
                    bigEnough.add(option);
                }
            }

            // Pick the smallest of those, assuming we found any
            if (bigEnough.size() > 0) {
                return Collections.min(bigEnough, new CompareSizesByArea());
            } else {
                return choices[0];
            }
        }

        public void attach(ZRuntime zRuntime, UIWebView appView)
        {
            if (!_attached) {
                _attached = true;
                _backgroundThread = new HandlerThread("CameraBackground");
                _backgroundThread.start();
                _backgroundHandler = new Handler(_backgroundThread.getLooper());

                if (_textureView == null) {
                    _textureView = new AutoFitTextureView(zRuntime.getContext());
                }
                _appView = appView;

                appView.addView(_textureView);

                if (_textureView.isAvailable()) {
                    open(appView.getWidth(), appView.getHeight());
                } else {
                    _textureView.setSurfaceTextureListener(_surfaceTextureListener);
                }
            }
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        public void detach()
        {
            if (_isInited) {
                try {
                    _attached = false;
                    _isInited = false;
                    _cameraOpenCloseLock.acquire();
                    if (_captureSession != null) {
                        _captureSession.close();
                        _captureSession = null;
                    }
                    if (_cameraDevice != null) {
                        _cameraDevice.close();
                        _cameraDevice = null;
                    }
                    if (_imageReader != null) {
                        _imageReader.close();
                        _imageReader = null;
                    }
                }
                catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
                }
                finally {
                    _cameraOpenCloseLock.release();
                }

                _backgroundThread.quitSafely();
                try {
                    _backgroundThread.join();
                    _backgroundThread = null;
                    _backgroundHandler = null;
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    if (_appView != null) {
                        _appView.removeView(_textureView);
                    }
                }
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private void open(int width, int height)
        {
            setUpCameraOutputs(width, height);
            configureTransform(width, height);
            Activity activity = LockActivity.getInstance();
            CameraManager manager = (CameraManager)activity.getSystemService(Context.CAMERA_SERVICE);
            try {
                if (!_cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                }

                manager.openCamera(mCameraId, _stateCallback, _backgroundHandler);
            }
            catch (CameraAccessException | RuntimeException e) {
                e.printStackTrace();
            }
            catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
            }
        }


        private class AutoFitTextureView extends TextureView
        {
            private int mRatioWidth = 0;
            private int mRatioHeight = 0;

            public AutoFitTextureView(Context context) {
                this(context, null);
            }

            public AutoFitTextureView(Context context, AttributeSet attrs) {
                this(context, attrs, 0);
            }

            public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
                super(context, attrs, defStyle);
            }

            public void setAspectRatio(int width, int height)
            {
                if (width < 0 || height < 0) {
                    throw new IllegalArgumentException("Size cannot be negative.");
                }
                mRatioWidth = width;
                mRatioHeight = height;
                requestLayout();
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                //int width = MeasureSpec.getSize(widthMeasureSpec);
                //int height = MeasureSpec.getSize(heightMeasureSpec);
                setMeasuredDimension(1, 1);
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private void createCameraPreviewSession() {
            try {
                SurfaceTexture texture = _textureView.getSurfaceTexture();
                assert texture != null;

                // We configure the size of default buffer to be the size of camera preview we want.
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

                // This is the output Surface we need to start preview.
                Surface surface = new Surface(texture);

                // We set up a CaptureRequest.Builder with the output Surface.
                _previewRequestBuilder = _cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                _previewRequestBuilder.addTarget(surface);

                // Here, we create a CameraCaptureSession for camera preview.
                _cameraDevice.createCaptureSession(Arrays.asList(surface, _imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                        // The camera is already closed
                        if (_cameraDevice == null) {
                            return;
                        }

                        // When the session is ready, we start displaying the preview.
                        _captureSession = cameraCaptureSession;
                        try {
                            // Auto focus should be continuous for camera preview.
                            _previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            // Flash is automatically enabled when necessary.
                            _previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                            // Finally, we start displaying the camera preview.
                            _previewRequest = _previewRequestBuilder.build();
                            if (_captureSession != null) {
                                _captureSession.setRepeatingRequest(_previewRequest, _captureCallback, _backgroundHandler);
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    }
                }, null);
            }
            catch (Exception e) {
                e.printStackTrace();
                detach();
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private void configureTransform(int viewWidth, int viewHeight) {
            Activity activity = LockActivity.getInstance();
            if (null == _textureView || null == activity) {
                return;
            }
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            Matrix matrix = new Matrix();
            RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
            RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();
            if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                float scale = Math.max((float)viewHeight, (float)viewWidth);
                matrix.postScale(scale, scale, centerX, centerY);
                matrix.postRotate(90 * (rotation - 2), centerX, centerY);
            }
            _textureView.setTransform(matrix);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private void runPrecaptureSequence() {
            try {
                // This is how to tell the camera to trigger.
                _previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                // Tell #mCaptureCallback to wait for the precapture sequence to be set.
                mState = STATE_WAITING_PRECAPTURE;
                if (_captureSession != null) {
                    _captureSession.capture(_previewRequestBuilder.build(), _captureCallback, _backgroundHandler);
                }
            }
            catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private void captureStillPicture() {
            try {
                final Activity activity = LockActivity.getInstance();
                if (activity == null || _cameraDevice == null) {
                    return;
                }
                // This is the CaptureRequest.Builder that we use to take a picture.
                final CaptureRequest.Builder captureBuilder = _cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.addTarget(_imageReader.getSurface());

                // Use the same AE and AF modes as the preview.
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                // Orientation
                int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

                CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                        unlockFocus();
                    }
                };

                if (_captureSession != null) {
                    _captureSession.stopRepeating();
                    _captureSession.capture(captureBuilder.build(), captureCallback, null);
                }
            }
            catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private void unlockFocus() {
            try {
                // Reset the autofucos trigger
                _previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                _previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                _captureSession.capture(_previewRequestBuilder.build(), _captureCallback, _backgroundHandler);
                // After this, the camera will go back to the normal state of preview.
                mState = STATE_PREVIEW;
                _captureSession.setRepeatingRequest(_previewRequest, _captureCallback, _backgroundHandler);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        public boolean takePicture(File imageFile)
        {
            boolean ret;
            try {
                _imageFile = imageFile;
                _latch = new CountDownLatch(1);
                LockActivity a = LockActivity.getInstance();
                if (a != null) {
                    final CameraLollipop me = this;
                    a.runOnUiThread(new Runnable() {
                        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void run() {
                            try {
                                // This is how to tell the camera to lock focus.
                                me._previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                                // Tell #mCaptureCallback to wait for the lock.
                                me.mState = STATE_WAITING_LOCK;
                                me._captureSession.setRepeatingRequest(me._previewRequestBuilder.build(), _captureCallback, me._backgroundHandler);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                me._latch.countDown();
                            }
                        }
                    });
                }

                ret = _latch.await(30, TimeUnit.SECONDS);
            }
            catch (Exception e) {
                e.printStackTrace();
                ret = false;
            }

            return ret;
        }
    }

    public ZCamera(Context ctx)
	{
        _zRuntime = ZRuntime.getInstance(null);
        _appView = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            _camera = new CameraLollipop();
        }
        else {
            _camera = new CameraOld();
        }
	}

    public synchronized boolean attachPreview(UIWebView appView)
    {
        try {
            if (!((SystemUtils)_zRuntime.getSystemUtils()).hasCamera()) {
                return false;
            }

            _appView = appView;
            _camera.attach(_zRuntime, _appView);
       }
        catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public synchronized void detachPreview()
    {
        try {
            _camera.detach();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isInited()
    {
        return _camera.isInited();
    }

    public boolean takePicture(File imageFile)
    {
        boolean ret = false;
        if (_camera.isInited()) {
            ret = _camera.takePicture(imageFile);
        }

        return ret;
    }
}
