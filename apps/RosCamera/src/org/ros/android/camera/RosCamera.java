/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.ros.android.camera;

import org.ros.android.camera.R;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import org.ros.exceptions.RosInitException;
import ros.android.activity.RosActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ethan.rublee@gmail.com (Ethan Rublee)
 * 
 */
public class RosCamera extends RosActivity {
  private Preview mPreview;

  Camera mCamera;
  int cameraCurrentlyLocked;

  // The first rear facing camera
  int defaultCameraId;

  private RosCameraPub rosCameraPublisher;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Hide the window title.
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

    // Create a RelativeLayout container that will hold a SurfaceView,
    // and set it as the content of our activity.
    mPreview = new Preview(this);
    setContentView(mPreview);

    rosCameraPublisher = new RosCameraPub();
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Open the default i.e. the first rear facing camera.
    mCamera = Camera.open();
    cameraCurrentlyLocked = defaultCameraId;
    mPreview.setCamera(mCamera);

    try {
      rosCameraPublisher.init(getNode());
      mPreview.setCallback(rosCameraPublisher);
    } catch (RosInitException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  @Override
  protected void onPause() {
    super.onPause();
    rosCameraPublisher.stop();
    // Because the Camera object is a shared resource, it's very
    // important to release it when the activity is paused.
    if (mCamera != null) {
      mPreview.setCamera(null);
      mCamera.release();
      mCamera = null;
    }

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    // Inflate our menu which can gather user input for switching camera
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.cameramenu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {
    case R.id.switch_cam:

    default:
      return super.onOptionsItemSelected(item);
    }
  }

}

// ----------------------------------------------------------------------

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered
 * preview of the Camera to the surface. We need to center the SurfaceView
 * because not all devices have cameras that support preview sizes at the same
 * aspect ratio as the device's display.
 */
class Preview extends ViewGroup implements SurfaceHolder.Callback, PreviewCallback {
  private final String TAG = "Preview";

  SurfaceView mSurfaceView;
  SurfaceHolder mHolder;
  Size mPreviewSize;
  List<Size> mSupportedPreviewSizes;
  Camera mCamera;

  Preview(Context context) {
    super(context);

    this.callback = null;
    mSurfaceView = new SurfaceView(context);
    addView(mSurfaceView);

    // Install a SurfaceHolder.Callback so we get notified when the
    // underlying surface is created and destroyed.
    mHolder = mSurfaceView.getHolder();
    mHolder.addCallback(this);
    mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
  }

  public void setCallback(PreviewCallback callback) {

    this.callback = callback;
  }

  public void setCamera(Camera camera) {
    mCamera = camera;
    if (mCamera != null) {
      mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
      requestLayout();
      setupPreview();
    }
  }

  public void switchCamera(Camera camera) {
    setCamera(camera);
    try {
      camera.setPreviewDisplay(mHolder);
    } catch (IOException exception) {
      Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
    }
    Camera.Parameters parameters = camera.getParameters();
    parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
    requestLayout();

    camera.setParameters(parameters);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // We purposely disregard child measurements because act as a
    // wrapper to a SurfaceView that centers the camera preview instead
    // of stretching it.
    final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
    final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
    setMeasuredDimension(width, height);

    if (mSupportedPreviewSizes != null) {
      mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    if (changed && getChildCount() > 0) {
      final View child = getChildAt(0);

      final int width = r - l;
      final int height = b - t;

      int previewWidth = width;
      int previewHeight = height;
      if (mPreviewSize != null) {
        previewWidth = mPreviewSize.width;
        previewHeight = mPreviewSize.height;
      }

      // Center the child SurfaceView within the parent.
      if (width * previewHeight > height * previewWidth) {
        final int scaledChildWidth = previewWidth * height / previewHeight;
        child.layout((width - scaledChildWidth) / 2, 0, (width + scaledChildWidth) / 2, height);
      } else {
        final int scaledChildHeight = previewHeight * width / previewWidth;
        child.layout(0, (height - scaledChildHeight) / 2, width, (height + scaledChildHeight) / 2);
      }
    }
  }

  public void surfaceCreated(SurfaceHolder holder) {
    // The Surface has been created, acquire the camera and tell it where
    // to draw.
    try {
      if (mCamera != null) {
        mCamera.setPreviewDisplay(holder);
      }
    } catch (IOException exception) {
      Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
    }
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    // Surface will be destroyed when we return, so stop the preview.
    if (mCamera != null) {
      mCamera.stopPreview();
    }
  }

  private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
    final double ASPECT_TOLERANCE = 0.1;
    double targetRatio = (double) w / h;
    if (sizes == null)
      return null;

    Size optimalSize = null;
    double minDiff = Double.MAX_VALUE;

    int targetHeight = h;

    // Try to find an size match aspect ratio and size
    for (Size size : sizes) {
      double ratio = (double) size.width / size.height;
      if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
        continue;
      if (Math.abs(size.height - targetHeight) < minDiff) {
        optimalSize = size;
        minDiff = Math.abs(size.height - targetHeight);
      }
    }

    // Cannot find the one match the aspect ratio, ignore the requirement
    if (optimalSize == null) {
      minDiff = Double.MAX_VALUE;
      for (Size size : sizes) {
        if (Math.abs(size.height - targetHeight) < minDiff) {
          optimalSize = size;
          minDiff = Math.abs(size.height - targetHeight);
        }
      }
    }
    return optimalSize;
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    // Now that the size is known, set up the camera parameters and begin
    // the preview.
    Camera.Parameters parameters = mCamera.getParameters();
    parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
    requestLayout();
    parameters.setPreviewFormat(ImageFormat.NV21);
    mCamera.setParameters(parameters);
    mCamera.startPreview();
    setupPreview();
  }

  private ArrayList<byte[]> previewBuffers;

  private PreviewCallback callback;

  void setupPreview() {

    previewBuffers = new ArrayList<byte[]>();
    Size sz = mCamera.getParameters().getPreviewSize();
    int format = mCamera.getParameters().getPreviewFormat();
    int bits_per_pixel = ImageFormat.getBitsPerPixel(format);
    previewBuffers.add(new byte[sz.height * sz.width * bits_per_pixel / 8]);
    for (byte[] x : previewBuffers) {
      mCamera.addCallbackBuffer(x);
    }
    mCamera.setPreviewCallback(this);
  }

  @Override
  public void onPreviewFrame(byte[] data, Camera camera) {
    if (callback != null)
      callback.onPreviewFrame(data, camera);
    mCamera.addCallbackBuffer(data);
  }

}