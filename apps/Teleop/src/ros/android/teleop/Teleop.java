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

package ros.android.teleop;

import android.view.MenuInflater;

import android.view.Menu;
import android.view.MenuItem;

import android.view.WindowManager;

import android.view.Window;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import org.ros.Node;
import org.ros.Publisher;
import org.ros.exceptions.RosInitException;
import org.ros.message.geometry_msgs.Twist;
import org.ros.namespace.NameResolver;
import ros.android.activity.RosActivity;
import ros.android.sensor.GravityTeleop;
import ros.android.views.SensorImageView;

/**
 * @author kwc@willowgarage.com (Ken Conley)
 */
public class Teleop extends RosActivity implements OnTouchListener {
  private Publisher<Twist> twistPub;
  private SensorImageView imageSub;
  private Thread pubThread;
  private GravityTeleop sensor;
  private boolean deadman;
  private Twist touchCartesianMessage;
  private Twist stopMessage;
  private float motionY;
  private float motionX;
  private boolean gravityMode;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.main);

    View mainView = findViewById(R.id.image);
    mainView.setOnTouchListener(this);

    imageSub = (SensorImageView) findViewById(R.id.image);
    imageSub.setOnTouchListener(this);
    sensor = new GravityTeleop();
    deadman = false;
    stopMessage = new Twist();
    touchCartesianMessage = new Twist();
  }

  @Override
  protected void onPause() {
    super.onPause();
    deadman = false;
    sensor.stop(this);
    twistPub = null;
    if (imageSub != null) {
      // imageSub.stop();
      imageSub = null;
    }
    if (pubThread != null) {
      pubThread.interrupt();
      pubThread = null;
    }
  }

  @Override
  protected void onResume() {
    // TODO(kwc): need to load app manager, make sure teleop control app is
    // running

    // TODO(kwc): needs a whole lot of tuning
    super.onResume();
    try {
      sensor.start(this);

      Node node = getNode();

      imageSub = (SensorImageView) findViewById(R.id.image);
      imageSub.init(node, "/camera/rgb/image_color/compressed");
      imageSub.setSelected(true);

      NameResolver resolver = node.getResolver().createResolver("turtlebot_node");
      twistPub = node.createPublisher(resolver.resolveName("cmd_vel"), Twist.class);

      pubThread = new Thread(new Runnable() {

        @Override
        public void run() {
          Twist message;
          try {
            while (true) {
              // 10Hz
              if (gravityMode) {
                message = sensor.getTwist();
              } else {
                message = touchCartesianMessage;
              }

              if (deadman && message != null) {
                twistPub.publish(message);
                Log.i("Teleop", "twist: " + message.angular.x + " " + message.angular.z);
              } else {
                Log.i("Teleop", "stop");
                twistPub.publish(stopMessage);
              }
              Thread.sleep(100);
            }
          } catch (InterruptedException e) {
          }
        }
      });
      pubThread.start();
    } catch (RosInitException e) {
      Log.e("Teleop", e.getMessage());
    }

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.teleop_switch, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.gravity:
      gravityMode = true;
      break;
    case R.id.touch:
      gravityMode = false;
      break;
    }
    // TODO Auto-generated method stub
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onTouch(View arg0, MotionEvent motionEvent) {
    int action = motionEvent.getAction();
    if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
      deadman = true;
      
        motionX = (motionEvent.getX() - (arg0.getWidth() / 2)) / (arg0.getWidth());
        motionY = (motionEvent.getY() - (arg0.getHeight() / 2)) / (arg0.getHeight());
        
        touchCartesianMessage.linear.x = -motionY;
        touchCartesianMessage.linear.y = 0;
        touchCartesianMessage.linear.z = 0;
        touchCartesianMessage.angular.x = 0;
        touchCartesianMessage.angular.y = 0;
        touchCartesianMessage.angular.z = -2 * motionX;
      
    } else {
      deadman = false;
    }
    return true;
  }

}