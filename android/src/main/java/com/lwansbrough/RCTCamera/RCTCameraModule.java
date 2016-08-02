/**
 * Created by Fabrice Armisen (farmisen@gmail.com) on 1/4/16.
 */

package com.lwansbrough.RCTCamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Surface;
import com.facebook.react.bridge.*;

import javax.annotation.Nullable;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class RCTCameraModule extends ReactContextBaseJavaModule implements MediaRecorder.OnInfoListener {
    private static final String TAG = "RCTCameraModule";

    public static final int RCT_CAMERA_ASPECT_FILL = 0;
    public static final int RCT_CAMERA_ASPECT_FIT = 1;
    public static final int RCT_CAMERA_ASPECT_STRETCH = 2;
    public static final int RCT_CAMERA_CAPTURE_MODE_STILL = 0;
    public static final int RCT_CAMERA_CAPTURE_MODE_VIDEO = 1;
    public static final int RCT_CAMERA_CAPTURE_TARGET_MEMORY = 0;
    public static final int RCT_CAMERA_CAPTURE_TARGET_DISK = 1;
    public static final int RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL = 2;
    public static final int RCT_CAMERA_CAPTURE_TARGET_TEMP = 3;
    public static final int RCT_CAMERA_ORIENTATION_AUTO = Integer.MAX_VALUE;
    public static final int RCT_CAMERA_ORIENTATION_PORTRAIT = Surface.ROTATION_0;
    public static final int RCT_CAMERA_ORIENTATION_PORTRAIT_UPSIDE_DOWN = Surface.ROTATION_180;
    public static final int RCT_CAMERA_ORIENTATION_LANDSCAPE_LEFT = Surface.ROTATION_90;
    public static final int RCT_CAMERA_ORIENTATION_LANDSCAPE_RIGHT = Surface.ROTATION_270;
    public static final int RCT_CAMERA_TYPE_FRONT = 1;
    public static final int RCT_CAMERA_TYPE_BACK = 2;
    public static final int RCT_CAMERA_FLASH_MODE_OFF = 0;
    public static final int RCT_CAMERA_FLASH_MODE_ON = 1;
    public static final int RCT_CAMERA_FLASH_MODE_AUTO = 2;
    public static final int RCT_CAMERA_TORCH_MODE_OFF = 0;
    public static final int RCT_CAMERA_TORCH_MODE_ON = 1;
    public static final int RCT_CAMERA_TORCH_MODE_AUTO = 2;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private final ReactApplicationContext _reactContext;
    private RCTSensorOrientationChecker _sensorOrientationChecker;

    private MediaRecorder mMediaRecorder = null;
    private Promise mVideoPromise = null;
    private Camera mCamera = null;
    private String mVideoDestinationUri;

    public RCTCameraModule(ReactApplicationContext reactContext) {
        super(reactContext);
        _reactContext = reactContext;
        _sensorOrientationChecker = new RCTSensorOrientationChecker(_reactContext);
    }

    @Override
    public String getName() {
        return "RCTCameraModule";
    }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        return Collections.unmodifiableMap(new HashMap<String, Object>() {
            {
                put("Aspect", getAspectConstants());
                put("BarCodeType", getBarCodeConstants());
                put("Type", getTypeConstants());
                put("CaptureQuality", getCaptureQualityConstants());
                put("CaptureMode", getCaptureModeConstants());
                put("CaptureTarget", getCaptureTargetConstants());
                put("Orientation", getOrientationConstants());
                put("FlashMode", getFlashModeConstants());
                put("TorchMode", getTorchModeConstants());
            }

            private Map<String, Object> getAspectConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("stretch", RCT_CAMERA_ASPECT_STRETCH);
                        put("fit", RCT_CAMERA_ASPECT_FIT);
                        put("fill", RCT_CAMERA_ASPECT_FILL);
                    }
                });
            }

            private Map<String, Object> getBarCodeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        // @TODO add barcode types
                    }
                });
            }

            private Map<String, Object> getTypeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("front", RCT_CAMERA_TYPE_FRONT);
                        put("back", RCT_CAMERA_TYPE_BACK);
                    }
                });
            }

            private Map<String, Object> getCaptureQualityConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("low", "low");
                        put("medium", "medium");
                        put("high", "high");
                        put("photo","high");
                    }
                });
            }

            private Map<String, Object> getCaptureModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("still", RCT_CAMERA_CAPTURE_MODE_STILL);
                        put("video", RCT_CAMERA_CAPTURE_MODE_VIDEO);
                    }
                });
            }

            private Map<String, Object> getCaptureTargetConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("memory", RCT_CAMERA_CAPTURE_TARGET_MEMORY);
                        put("disk", RCT_CAMERA_CAPTURE_TARGET_DISK);
                        put("cameraRoll", RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL);
                        put("temp", RCT_CAMERA_CAPTURE_TARGET_TEMP);
                    }
                });
            }

            private Map<String, Object> getOrientationConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("auto", RCT_CAMERA_ORIENTATION_AUTO);
                        put("landscapeLeft", RCT_CAMERA_ORIENTATION_LANDSCAPE_LEFT);
                        put("landscapeRight", RCT_CAMERA_ORIENTATION_LANDSCAPE_RIGHT);
                        put("portrait", RCT_CAMERA_ORIENTATION_PORTRAIT);
                        put("portraitUpsideDown", RCT_CAMERA_ORIENTATION_PORTRAIT_UPSIDE_DOWN);
                    }
                });
            }

            private Map<String, Object> getFlashModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("off", RCT_CAMERA_FLASH_MODE_OFF);
                        put("on", RCT_CAMERA_FLASH_MODE_ON);
                        put("auto", RCT_CAMERA_FLASH_MODE_AUTO);
                    }
                });
            }

            private Map<String, Object> getTorchModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("off", RCT_CAMERA_TORCH_MODE_OFF);
                        put("on", RCT_CAMERA_TORCH_MODE_ON);
                        put("auto", RCT_CAMERA_TORCH_MODE_AUTO);
                    }
                });
            }
        });
    }

    @ReactMethod
    public void capture(final ReadableMap options, final Promise promise) {
        int orientation = options.hasKey("orientation") ? options.getInt("orientation") : RCTCamera.getInstance().getOrientation();
        if (orientation == RCT_CAMERA_ORIENTATION_AUTO) {
            _sensorOrientationChecker.onResume();
            _sensorOrientationChecker.registerOrientationListener(new RCTSensorOrientationListener() {
                @Override
                public void orientationEvent() {
                    int deviceOrientation = _sensorOrientationChecker.getOrientation();
                    _sensorOrientationChecker.unregisterOrientationListener();
                    _sensorOrientationChecker.onPause();
                    captureWithOrientation(options, promise, deviceOrientation);
                }
            });
        } else {
            captureWithOrientation(options, promise, orientation);
        }
    }

    public void captureWithOrientation(final ReadableMap options, final Promise promise, int deviceOrientation) {
        RCTCamera rCamera = RCTCamera.getInstance();
        Camera camera = rCamera.acquireCameraInstance(options.getInt("type"));
        if (null == camera) {
            promise.reject("No camera found.");
            return;
        }

        if (options.hasKey("playSoundOnCapture") && options.getBoolean("playSoundOnCapture")) {
            MediaActionSound sound = new MediaActionSound();
            sound.play(MediaActionSound.SHUTTER_CLICK);
        }

        if (options.hasKey("quality")) {
            rCamera.setCaptureQuality(options.getInt("type"), options.getString("quality"));
        }

        rCamera.adjustCameraRotationToDeviceOrientation(options.getInt("type"), deviceOrientation);

        if (options.getInt("mode") == RCT_CAMERA_CAPTURE_MODE_VIDEO) {
            captureVideo(options, rCamera, promise);
            return;
        }

        if (options.getInt("mode") == RCT_CAMERA_CAPTURE_MODE_STILL) {
            capturePicture(options, rCamera, promise);
            return;
        }

        promise.reject("Unable to find given capture mode", null);
        return;

    }

    private void capturePicture(final ReadableMap options, RCTCamera rCamera, final Promise promise) {
        Camera camera = rCamera.acquireCameraInstance(options.getInt("type"));
        camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                camera.stopPreview();
                camera.startPreview();
                WritableMap response = new WritableNativeMap();
                switch (options.getInt("target")) {
                    case RCT_CAMERA_CAPTURE_TARGET_MEMORY:
                        String encoded = Base64.encodeToString(data, Base64.DEFAULT);
                        response.putString("data", encoded);
                        promise.resolve(response);
                        break;
                    case RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL:
                        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, bitmapOptions);
                        String url = MediaStore.Images.Media.insertImage(
                                _reactContext.getContentResolver(),
                                bitmap, options.getString("title"),
                                options.getString("description"));
                        response.putString("path", url);
                        promise.resolve(response);
                        break;
                    case RCT_CAMERA_CAPTURE_TARGET_DISK:
                        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                        if (pictureFile == null) {
                            promise.reject("Error creating media file.");
                            return;
                        }

                        try {
                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(data);
                            fos.close();
                        } catch (FileNotFoundException e) {
                            promise.reject("File not found: " + e.getMessage());
                        } catch (IOException e) {
                            promise.reject("Error accessing file: " + e.getMessage());
                        }
                        response.putString("path", Uri.fromFile(pictureFile).toString());
                        promise.resolve(response);
                        break;
                    case RCT_CAMERA_CAPTURE_TARGET_TEMP:
                        File tempFile = getTempMediaFile(MEDIA_TYPE_IMAGE);

                        if (tempFile == null) {
                            promise.reject("Error creating media file.");
                            return;
                        }

                        try {
                            FileOutputStream fos = new FileOutputStream(tempFile);
                            fos.write(data);
                            fos.close();
                        } catch (FileNotFoundException e) {
                            promise.reject("File not found: " + e.getMessage());
                        } catch (IOException e) {
                            promise.reject("Error accessing file: " + e.getMessage());
                        }
                        response.putString("path", Uri.fromFile(tempFile).toString());
                        promise.resolve(response);
                        break;
                }
            }
        });
    }

    protected void captureVideo(ReadableMap options, RCTCamera rCamera, Promise promise) {
        Camera camera = rCamera.acquireCameraInstance(options.getInt("type"));
        mCamera = camera;
        // Get the video destination
        File destination;
        switch (options.getInt("target")) {
            case RCT_CAMERA_CAPTURE_TARGET_DISK:
                destination = getOutputMediaFile(MEDIA_TYPE_VIDEO);
                break;
            case RCT_CAMERA_CAPTURE_TARGET_TEMP:
            default:
                destination = getTempMediaFile(MEDIA_TYPE_VIDEO);
                break;
        }

        // Store promise, camera and file location for later
        mVideoPromise = promise;
        mVideoDestinationUri = Uri.fromFile(destination).toString();

        // Try a hack for samsung HQ, didn't help (http://stackoverflow.com/questions/7225571/camcorderprofile-quality-high-resolution-produces-green-flickering-video)
        // Camera.Parameters parameters = mCamera.getParameters();
        // Camera.Size previewSize = parameters.getPreviewSize();
        // parameters.set("cam_mode", 1);
        // parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
        // mCamera.setParameters(parameters);

        // Setup media recorder (watch out with modifying these options, the order is important!)
        // @see http://developer.android.com/guide/topics/media/camera.html#capture-video
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        // Attach callback to handle maxDuration (@see onInfo method in this file)
        mMediaRecorder.setOnInfoListener(this);
        mMediaRecorder.setCamera(mCamera);

        // Set AV sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Set orientation hit
//        int orientation = rCamera.getCorrectCameraOrientation(options.getInt("type"));
//        mMediaRecorder.setOrientationHint(orientation);
        // Create a profile
        int quality;
        switch (options.getString("quality")) {
            case "low":
                quality = CamcorderProfile.QUALITY_LOW; // select the lowest res
                break;
            case "medium":
                quality = CamcorderProfile.QUALITY_720P; // select medium
                break;
            case "high":
                quality = CamcorderProfile.QUALITY_HIGH; // select the highest res (default)
                break;
            default:
                releaseMediaRecorder();
                promise.reject("No valid quality option given");
                return;
        }
        CamcorderProfile profile = CamcorderProfile.get(quality);
        // Modify profile
        // profile.fileFormat = MediaRecorder.OutputFormat.THREE_GPP;
        profile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
        profile.audioCodec = MediaRecorder.AudioEncoder.AMR_NB;
        profile.videoCodec = MediaRecorder.VideoEncoder.H264;
        // profile.videoBitRate = 15;
        // profile.videoFrameRate = 30;
        mMediaRecorder.setProfile(profile);
        // Same as profile settings from above, do not use both at the same time
        // mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        // mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        // mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        // mMediaRecorder.setVideoEncodingBitRate(15);
        // mMediaRecorder.setVideoFrameRate(30);
        // mMediaRecorder.setAudioEncodingBitRate(50000);

        mMediaRecorder.setOutputFile(destination.getAbsolutePath());

        // Set maxDuration when given as option, didn't seem to work when passed in to the profile
        // On my devices, maxDuration couln't be less than 4 seconds
        if (options.hasKey("totalSeconds")) {
            int totalSeconds = options.getInt("totalSeconds");
            // @todo not sure if this is the case on all platforms:
            if (totalSeconds < 4) {
                totalSeconds = 4;
            }
            mMediaRecorder.setMaxDuration(totalSeconds * 1000);
        }
        mMediaRecorder.setMaxFileSize(100 * 1000 * 1000); // = 100 MB

        // Step 5: Set the preview output
//        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        try {
            // prepare and start recording
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException: " + e.getMessage(), e);
            releaseMediaRecorder();
            promise.reject("IllegalStateException: " + e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage(), e);
            releaseMediaRecorder();
            promise.reject("IOException: " + e.getMessage(), e);
        }
        // Start the recording in the background (@see RecordVideoTask down below)
        // new RecordVideoTask().execute(null, null, null);
        mMediaRecorder.start();
    }

    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (
            what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
            what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED
        ) {
            releaseMediaRecorder();
        }
    }

    // class RecordVideoTask extends AsyncTask<Void, Void, Boolean> {
    //     @Override
    //     protected Boolean doInBackground(Void... voids) {
    //         mMediaRecorder.start();
    //         return true;
    //     }
    // }

    public void releaseMediaRecorder() {
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.setOnInfoListener(null);
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "error releasing media recorder", e);
        }
        if (mCamera != null) {
            mCamera.lock();
            mCamera = null;
        }
        if (mVideoPromise != null) {
            mVideoPromise.resolve(mVideoDestinationUri);
            mVideoPromise = null;
        }
    }


    @ReactMethod
    public void stopCapture(final Promise promise) {
        releaseMediaRecorder();
        promise.resolve("Stopped capture");
    }

    @ReactMethod
    public void hasFlash(ReadableMap options, final Promise promise) {
        Camera camera = RCTCamera.getInstance().acquireCameraInstance(options.getInt("type"));
        if (null == camera) {
            promise.reject("No camera found.");
            return;
        }
        List<String> flashModes = camera.getParameters().getSupportedFlashModes();
        promise.resolve(null != flashModes && !flashModes.isEmpty());
    }

    private File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "RCTCameraModule");

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "failed to create directory:" + mediaStorageDir.getAbsolutePath());
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            Log.e(TAG, "Unsupported media type:" + type);
            return null;
        }
        return mediaFile;
    }


    private File getTempMediaFile(int type) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File outputDir = _reactContext.getCacheDir();
            File outputFile;

            if (type == MEDIA_TYPE_IMAGE) {
                outputFile = File.createTempFile("IMG_" + timeStamp, ".jpg", outputDir);
            } else if (type == MEDIA_TYPE_VIDEO) {
                outputFile = File.createTempFile("VID_" + timeStamp, ".mp4", outputDir);
            } else {
                Log.e(TAG, "Unsupported media type:" + type);
                return null;
            }
            return outputFile;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }
}
