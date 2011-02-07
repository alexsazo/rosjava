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

package com.googlecode.rosjava.android;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import org.apache.xmlrpc.XmlRpcException;
import org.ros.communication.MessageDescription;
import org.ros.communication.geometry_msgs.Point;
import org.ros.communication.geometry_msgs.Pose;
import org.ros.communication.geometry_msgs.Quaternion;
import org.ros.node.RemoteException;
import org.ros.node.client.Master;
import org.ros.node.server.Slave;
import org.ros.topic.Publisher;
import org.ros.topic.TopicDescription;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class Main extends Activity {

  private final Point origin;

  public Main() throws IOException {
    super();
    origin = new Point();
    origin.x = 0;
    origin.y = 0;
    origin.z = 0;

  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    Master master;
    try {
      master = new Master(new URL("http://10.0.2.2:11311/"));
    } catch (MalformedURLException e) {
      e.printStackTrace();
      return;
    }
    
    TopicDescription topicDescription =
        new TopicDescription("/android/pose",
            MessageDescription.createFromMessage(new org.ros.communication.geometry_msgs.Pose()));
    final Publisher publisher;
    try {
      publisher = new Publisher(topicDescription, "localhost", 7332);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    publisher.start();
    
    Slave slave = new Slave("/android", master, "localhost", 7331);
    try {
      slave.start();
      slave.addPublisher(publisher);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    } 

    SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    sensorManager.registerListener(new SensorEventListener() {

      @Override
      public void onAccuracyChanged(Sensor sensor, int accuracy) {
      }

      @Override
      public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
          float[] quaternion = new float[4];
          SensorManager.getQuaternionFromVector(quaternion, event.values);
          Quaternion orientation = new Quaternion();
          orientation.w = quaternion[0];
          orientation.x = quaternion[1];
          orientation.y = quaternion[2];
          orientation.z = quaternion[3];
          Pose pose = new Pose();
          pose.position = origin;
          pose.orientation = orientation;
          publisher.publish(pose);
        }
      }

    }, sensor, SensorManager.SENSOR_DELAY_FASTEST);
  }

}
