package org.ros.tutorials;

import org.ros.Callback;

import org.ros.Node;

import org.ros.Publisher;

import org.ros.exceptions.RosInitException;

import org.ros.Ros;
import org.ros.message.Time;
import org.ros.message.geometry.Point;
import org.ros.message.geometry.PoseStamped;
import org.ros.message.geometry.Quaternion;

public class SampleNode {
  Node node;
  
  public static void main(String[] argv) throws RosInitException {
    //Node node = new Node(argv, "sample_node"); this crashes when topic is subscribed to
    final Node node = new Node(argv, "/sample_node");
    node.init();

    Publisher<PoseStamped> pub_pose = node.createPublisher("pose", PoseStamped.class);

    Callback<Quaternion> callback = new Callback<Quaternion>() {

      @Override
      public void onRecieve(Quaternion m) {
        node.logInfo("Toto " + m.w);
      }
    };

    node.createSubscriber("foo", callback, Quaternion.class);
    PoseStamped p = new PoseStamped();
    pub_pose.publish(p);

    Point origin;
    origin = new Point();
    origin.x = 0;
    origin.y = 0;
    origin.z = 0;
    int seq = 0;
    //which one should it be?
    //while (!ros.isShutdown()) {
    while (!node.isShutdown()) {
      float[] quaternion = new float[4];
      Quaternion orientation = new Quaternion();
      orientation.w = quaternion[0];
      orientation.x = quaternion[1];
      orientation.y = quaternion[2];
      orientation.z = quaternion[3];
      PoseStamped pose = new PoseStamped();
      pose.header.frame_id = "/map";
      pose.header.seq = seq++;
      pose.header.stamp = Time.now();
      pose.pose.position = origin;
      pose.pose.orientation = orientation;
      pub_pose.publish(pose);
    }
  }
}
