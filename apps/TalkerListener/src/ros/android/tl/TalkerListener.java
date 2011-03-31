package ros.android.tl;

import org.ros.Publisher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import org.ros.MessageListener;
import org.ros.Node;
import org.ros.NodeContext;
import ros.android.BarcodeLoader;
import ros.android.utils.master.MasterChooser;

public class TalkerListener extends Activity {

  private void setText(String text) {
    TextView t = (TextView) findViewById(R.id.text_view);
    t.setText(text);
  }

  Node node;
  private Publisher<org.ros.message.std_msgs.String> pub;
  private Thread pubThread;

  //
  // @Override
  // public Object onRetainNonConfigurationInstance() {
  // final LoadedPhoto[] list = new LoadedPhoto[numberOfPhotos];
  // keepPhotos(list);
  // return list;
  // }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

  }

  @Override
  protected void onPause() {
    super.onPause();
    if (pubThread != null) {
      pubThread.interrupt();
    }
    pubThread = null;
    if (node != null) {
      node.stop();
    }
    node = null;
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (node == null) {

      setText("loading");
      Log.i("RosAndroid", "loading.... should only happen once");

      try {

        BarcodeLoader loader = new BarcodeLoader(this, 0);
        loader.makeNotification(this);
        NodeContext context;
        context = loader.createContext();
        node = new Node("listener", context);
        // final Log log = node.getLog();
        Log.i("RosAndroid", "Setting up sub on /chatter");
        node.createSubscriber("chatter", new MessageListener<org.ros.message.std_msgs.String>() {
          @Override
          public void onNewMessage(final org.ros.message.std_msgs.String message) {
           // Log.i("RosAndroid", "I heard: \"" + message.data + "\"");
            runOnUiThread(new Runnable() {

              @Override
              public void run() {
                setText(message.data);

              }
            });

          }
        }, org.ros.message.std_msgs.String.class);
        final org.ros.message.std_msgs.String message = new org.ros.message.std_msgs.String();
        message.data = "hello from " + getString(R.string.app_name);
        
        Log.i("RosAndroid", "Setting up pub on /android_chatter");
        pub = node.createPublisher("android_chatter", org.ros.message.std_msgs.String.class);
        pubThread = new Thread(new Runnable() {

          @Override
          public void run() {

            try {
              while (true) {
                pub.publish(message);
                Thread.sleep(100);
              }
            } catch (InterruptedException e) {
            }
            Log.i("RosAndroid", "pub thread is exiting");
          }
        });
        pubThread.start();
        // RosImageView imageView =
        // (RosImageView)findViewById(R.id.ros_image_view);
        // imageView.init(node);

      } catch (Exception e) {
        setText("failed to create node" + e.getMessage());
      }
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == 0) {
      MasterChooser.uriFromResult(this, resultCode, data);
    }
  }
}