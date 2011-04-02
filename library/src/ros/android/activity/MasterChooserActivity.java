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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import ros.android.util.zxing.IntentResult;
import ros.android.util.zxing.IntentIntegrator;
import ros.android.util.SdCardSetup;

public class MasterChooserActivity extends Activity {

  public static final String MASTER_URI_EXTRA = "org.ros.android.MasterURI";

  private List<String> master_uris_;

  public MasterChooserActivity() {
    master_uris_ = new ArrayList<String>();
  }

  private File getMasterFile() {
    if( !SdCardSetup.isReady() )
    {
      SdCardSetup.promptUserForMount( this );
      return null;
    }
    else
    {
      try
      {
        File ros_dir = SdCardSetup.getRosDir();
        File master_list_file = new File( ros_dir, "master_uris" );
        if( ! master_list_file.exists() )
        {
          Log.i( "RosAndroid", "masters file does not exist, creating." );
          master_list_file.createNewFile();
        }
        return master_list_file;
      }
      catch( Exception ex )
      {
        Log.e( "RosAndroid", "exception in getMasterFile: " + ex.getMessage() );
        return null;
      }
    }
  }

  private void writeNewMaster( String new_master_uri ) {
    File master_list_file = getMasterFile();
    if( master_list_file == null )
    {
      Log.e( "RosAndroid", "writeNewMaster(): no masters file." );
      return;
    }

    try
    {
      FileWriter writer = new FileWriter( master_list_file, true ); // append to the file
      writer.write( new_master_uri + "\n" );
      writer.close();
      Log.i( "RosAndroid", "Appended '" + new_master_uri + "' to masters file." );
    }
    catch( Exception ex )
    {
      Log.e( "RosAndroid", "exception writing new master to sdcard: " + ex.getMessage() );
    }
  }

  private void readMasterList() {
    try
    {
      File master_list_file = getMasterFile();
      if( master_list_file == null )
      {
        Log.e( "RosAndroid", "readMasterList(): no masters file." );
        return;
      }

      BufferedReader reader = new BufferedReader( new FileReader( master_list_file ));
      try
      {
        master_uris_.clear();
        for( String line = reader.readLine(); line != null; line = reader.readLine() )
        {
          if( line != "" )
          {
            master_uris_.add( line );
          }
        }
      }
      finally
      {
        reader.close();
      }
    }
    catch( Exception ex )
    {
      Log.e( "RosAndroid", "exception reading list of previous master URIs: " + ex.getMessage() );
    }
  }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    readMasterList();

    setTitle( "Choose a ROS Master" );
    setContentView(R.layout.master_chooser);
    ListView listview = (ListView) findViewById(R.id.master_list);

    listview.setAdapter( new MasterAdapter( this, master_uris_ ));

    listview.setOnItemClickListener(new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
          choose( position );
        }
      });
  }

  private void choose( int position ) {
    Intent result_intent = new Intent();
    result_intent.putExtra( MASTER_URI_EXTRA, master_uris_.get( position ));
    setResult( RESULT_OK, result_intent );
    finish();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
    if (scanResult != null) {
      master_uris_.add( scanResult.getContents() );
      writeNewMaster( scanResult.getContents() );
    }
    else
    {
      Toast.makeText(this, "Scan failed", Toast.LENGTH_SHORT).show();
    }
  }

  public void scanNewRobotClicked( View view ) {
    IntentIntegrator.initiateScan( this );
  }
}