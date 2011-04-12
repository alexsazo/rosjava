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

import org.ros.Subscriber;

import android.widget.Toast;

import org.ros.MessageListener;
import org.ros.message.app_manager.AppStatus;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import org.ros.Node;
import org.ros.Publisher;
import org.ros.app_manager.AppManagerNotAvailableException;
import org.ros.app_manager.AppNotInstalledException;
import org.ros.exceptions.RosInitException;
import org.ros.message.geometry_msgs.Twist;
import org.ros.namespace.Namespace;
import ros.android.activity.RosAppActivity;
import ros.android.sensor.GravityTeleop;
import ros.android.views.SensorImageView;

/**
 * @author kwc@willowgarage.com (Ken Conley)
 */
public class Teleop extends RosAppActivity implements OnTouchListener {
  private Publisher<Twist> twistPub;
  private SensorImageView imageView;
  private Thread pubThread;
  private GravityTeleop sensor;
  private boolean deadman;
  private Twist touchCartesianMessage;
  private Twist stopMessage;
  private float motionY;
  private float motionX;
  private boolean gravityMode;
  private Subscriber<AppStatus> statusSub;

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

    imageView = (SensorImageView) findViewById(R.id.image);
    //imageView.setOnTouchListener(this);
    sensor = new GravityTeleop();
    deadman = false;
    stopMessage = new Twist();
    touchCartesianMessage = new Twist();
  }

  @Override
  protected void onPause() {
    deadman = false;
    sensor.stop(this);
    twistPub = null;
    if (imageView != null) {
      imageView.stop();
      imageView = null;
    }
    if (statusSub != null) { 
      statusSub.cancel();
      statusSub = null;
    }
    if (pubThread != null) {
      pubThread.interrupt();
      pubThread = null;
    }
    super.onPause();
  }

  protected void attachToastToAppStatus() throws RosInitException { 
    Namespace appNamespace = getAppNamespace();
    statusSub = appNamespace.createSubscriber("app_status", new MessageListener<AppStatus>() {
      @Override
      public void onNewMessage(final AppStatus message) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Toast.makeText(Teleop.this, message.status, Toast.LENGTH_LONG).show();
          }
        });

      }
    }, AppStatus.class);
  }
  
  @Override
  protected void onResume() {
    // TODO(kwc): needs a whole lot of tuning
    Toast.makeText(Teleop.this, "loading", Toast.LENGTH_SHORT).show();
    super.onResume();

    try {
      Toast.makeText(Teleop.this, "starting app", Toast.LENGTH_SHORT).show();
      ensureAppRunning("turtlebot_teleop/android_teleop");
      Toast.makeText(Teleop.this, "app started", Toast.LENGTH_SHORT).show();
      
      attachToastToAppStatus();
      
      sensor.start(this);

      Node node = getNode();
      Namespace appNamespace = getAppNamespace();
      imageView = (SensorImageView) findViewById(R.id.image);
      imageView.init(node, appNamespace.resolveName("camera/rgb/image_color/compressed"));
      imageView.setSelected(true);

      // TODO(kwc): I don't like cmd_vel being in the turtlebot_node namespace
      twistPub = appNamespace.createPublisher("turtlebot_node/cmd_vel", Twist.class);

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
    } catch (AppManagerNotAvailableException e) {
      // TODO(kwc) need permanent way of display app launch failure to user
      Log.e("Teleop", e.getMessage());
      Toast.makeText(Teleop.this, e.getMessage(), Toast.LENGTH_LONG).show();
    } catch (AppNotInstalledException e) {
      // TODO(kwc) display message to user that app cannot be run on this robot
      Log.e("Teleop", e.getMessage());
      Toast.makeText(Teleop.this, e.getMessage(), Toast.LENGTH_LONG).show();
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