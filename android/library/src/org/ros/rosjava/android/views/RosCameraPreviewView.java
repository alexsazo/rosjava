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

package org.ros.rosjava.android.views;

import com.google.common.base.Preconditions;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;

import org.ros.Node;
import org.ros.NodeConfiguration;
import org.ros.NodeMain;
import org.ros.Publisher;
import org.ros.message.Time;
import org.ros.message.sensor_msgs.CameraInfo;
import org.ros.message.sensor_msgs.Image;
import org.ros.namespace.NameResolver;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class RosCameraPreviewView extends CameraPreviewView implements NodeMain {
  
  private Node node;
  private Publisher<Image> imagePublisher;
  private Publisher<CameraInfo> cameraInfoPublisher;
  
  private final class PublishingPreviewCallback implements PreviewCallback {
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
      Image image = new Image();
      CameraInfo cameraInfo = new CameraInfo();
      String frameId = "android_camera";

      // TODO(ethan) right now serialization is deferred. When serialization
      // happens inline, we won't need to copy.
      image.data = new byte[data.length];
      System.arraycopy(data, 0, image.data, 0, data.length);

      image.encoding = "8UC1";
      Size size = camera.getParameters().getPreviewSize();
      image.step = size.width;
      image.width = size.width;
      image.height = size.height + size.height / 2;
      image.header.stamp = Time.fromMillis(System.currentTimeMillis());
      image.header.frame_id = frameId;
      imagePublisher.publish(image);
      
      cameraInfo.header.stamp = image.header.stamp;
      cameraInfo.header.frame_id = frameId;
      cameraInfo.width = size.width;
      cameraInfo.height = size.height;
      cameraInfoPublisher.publish(cameraInfo);
    }
  }
  
  public RosCameraPreviewView(Context context) {
    super(context);
  }

  public RosCameraPreviewView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public RosCameraPreviewView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  public void main(NodeConfiguration nodeConfiguration) throws Exception {
    Preconditions.checkState(node == null);
    node = new Node("/anonymous", nodeConfiguration);
    NameResolver resolver = node.getResolver().createResolver("camera");
    imagePublisher = node.createPublisher(resolver.resolveName("image_raw"), Image.class);
    cameraInfoPublisher =
        node.createPublisher(resolver.resolveName("camera_info"), CameraInfo.class);
    setPreviewCallback(new PublishingPreviewCallback());
  }
  
  public void stop() {
    Preconditions.checkNotNull(node);
    node.stop();
  }

}
