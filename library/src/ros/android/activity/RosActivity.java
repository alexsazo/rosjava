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

package ros.android.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.ros.Node;
import org.ros.exceptions.RosInitException;

import ros.android.util.MasterChooser;
import ros.android.util.RobotDescription;

public class RosActivity extends Activity {
  private MasterChooser masterChooser;
  private Node node;
  private Exception errorException;
  private String errorMessage;
  private WifiManager wifiManager;

  private static final int WIFI_DISABLED_DIALOG_ID = 9999999;
  private static final int WIFI_ENABLED_BUT_NOT_CONNECTED_DIALOG_ID = 9999998;

  public RosActivity() {
    masterChooser = new MasterChooser( this );
  }

  public Exception getErrorException() {
    return errorException;
  }

  public void setErrorException(Exception errorException) {
    this.errorException = errorException;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public RobotDescription getCurrentRobot() {
    return masterChooser.getCurrentRobot();
  }

  /**
   * Re-launch the MasterChooserActivity to choose a new ROS master. The results
   * are handled in onActivityResult() and onResume() since launching a new
   * activity necessarily pauses this current one.
   */
  public void chooseNewMaster() {
    masterChooser.launchChooserActivity();
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (node != null) {
      node.stop();
    }
    node = null;
  }

  @Override
  protected void onActivityResult( int requestCode, int resultCode, Intent result_intent ) {
    if (masterChooser.handleActivityResult(requestCode, resultCode, result_intent)) {
      // Save before checking validity in case someone wants to force
      // the next app to use the chooser.
      masterChooser.saveCurrentRobot();
      if (!masterChooser.hasRobot()) {
        Toast.makeText( this, "Cannot run without a ROS master.", Toast.LENGTH_LONG ).show();
        finish();
      }
    }
  }

  @Override
  protected Dialog onCreateDialog( int id ) {
    Dialog dialog;
    Button button;
    switch( id )
    {
    case WIFI_DISABLED_DIALOG_ID:
      dialog = new Dialog( this );
      dialog.setContentView( R.layout.wireless_disabled_dialog );
      dialog.setTitle( "Wifi network disabled." );
      button = (Button) dialog.findViewById( R.id.ok_button );
      button.setOnClickListener( new View.OnClickListener() {
          @Override
          public void onClick( View v ) {
            dismissDialog( WIFI_DISABLED_DIALOG_ID );
          }
        });
      button = (Button) dialog.findViewById( R.id.enable_button );
      button.setOnClickListener( new View.OnClickListener() {
          @Override
          public void onClick( View v ) {
            wifiManager.setWifiEnabled( true );
            dismissDialog( WIFI_DISABLED_DIALOG_ID );
          }
        });
      break;
    case WIFI_ENABLED_BUT_NOT_CONNECTED_DIALOG_ID:
      dialog = new Dialog( this );
      dialog.setContentView( R.layout.wireless_enabled_but_not_connected_dialog );
      dialog.setTitle( "Wifi not connected." );
      button = (Button) dialog.findViewById( R.id.ok_button );
      button.setOnClickListener( new View.OnClickListener() {
          @Override
          public void onClick( View v ) {
            dismissDialog( WIFI_ENABLED_BUT_NOT_CONNECTED_DIALOG_ID );
          }
        });
    default:
      dialog = null;
    }
    return dialog;
  }

  private void warnIfWifiDown() {
    if( !wifiManager.isWifiEnabled() ) {
      showDialog( WIFI_DISABLED_DIALOG_ID );
    } else if( wifiManager.getConnectionInfo() == null ) {
      showDialog( WIFI_ENABLED_BUT_NOT_CONNECTED_DIALOG_ID );
    } else {
      Log.i( "RosAndroid", "wifi seems OK." );
    }
  }

  /**
   * Read the current ROS master URI from external storage and set up the ROS
   * node from the resulting node context. If the current master is not set or
   * is invalid, launch the MasterChooserActivity to choose one or scan a new
   * one.
   */
  @Override
  protected void onResume() {
    super.onResume();
    Log.i("RosAndroid", "getting wifi manager");
    wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    Log.i("RosAndroid", "got wifi manager");
    warnIfWifiDown();
    Log.i("RosAndroid", "maybe doing warning dialog");
    if( node == null ) {
      masterChooser.loadCurrentRobot();
      if (masterChooser.hasRobot()) {
        Toast.makeText(this, "attaching to robot", Toast.LENGTH_SHORT).show();
        try {
          node = new Node("android", masterChooser.createContext());
        } catch (Exception e) {
          Log.e( "RosAndroid", "Exception while creating node: " + e.getMessage() );
          node = null;
          setErrorMessage("failed to create node" + e.getMessage());
          setErrorException(e);
        }
      } else {
        Toast.makeText(this, "finding a robot", Toast.LENGTH_SHORT).show();
        // we don't have a master yet.
        masterChooser.launchChooserActivity();
        // Launching the master chooser activity causes this activity
        // to pause.  this.onActivityResult() is called with the
        // result before onResume() is called again, so
        // masterChooser.hasRobot() should be true.
      }
    }
  }

  /**
   * Retrieve the ROS {@link Node} for this {@link Activity}. The {@link Node}
   * is stopped during {@code onPause()} and reinitialized during
   * {@code onResume()}. It is not safe to maintain a handle on the {@link Node}
   * instance.
   * 
   * @return Initialized {@link Node} instance.
   * @throws RosInitException
   *           If {@link Node} was not successfully initialized. Exception will
   *           contain original initialization exception.
   */
  public Node getNode() throws RosInitException {
    if (node == null) {
      throw new RosInitException(getErrorException());
    }
    return node;
  }

}