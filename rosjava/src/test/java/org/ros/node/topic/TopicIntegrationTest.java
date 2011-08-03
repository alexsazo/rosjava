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

package org.ros.node.topic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.ros.address.AdvertiseAddress;
import org.ros.address.BindAddress;
import org.ros.internal.node.server.MasterServer;
import org.ros.internal.node.server.SlaveIdentifier;
import org.ros.internal.node.topic.PublisherDefinition;
import org.ros.internal.node.topic.RepeatingPublisher;
import org.ros.internal.node.topic.TopicDefinition;
import org.ros.message.MessageDefinition;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.DefaultNodeFactory;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class TopicIntegrationTest {

  private MasterServer masterServer;
  private NodeFactory nodeFactory;
  private NodeConfiguration nodeConfiguration;

  @Before
  public void setUp() {
    masterServer = new MasterServer(BindAddress.newPublic(), AdvertiseAddress.newPublic());
    masterServer.start();
    nodeConfiguration = NodeConfiguration.newPrivate(masterServer.getUri());
    nodeFactory = new DefaultNodeFactory();
  }

  @Test
  public void testOnePublisherToOneSubscriber() throws InterruptedException {
    Node publisherNode = nodeFactory.newNode("publisher", nodeConfiguration);
    Publisher<org.ros.message.std_msgs.String> publisher =
        publisherNode.newPublisher("foo", "std_msgs/String");

    final org.ros.message.std_msgs.String helloMessage = new org.ros.message.std_msgs.String();
    helloMessage.data = "Hello, ROS!";

    final CountDownLatch messageReceived = new CountDownLatch(1);
    Node subscriberNode = nodeFactory.newNode("subscriber", nodeConfiguration);
    Subscriber<org.ros.message.std_msgs.String> subscriber =
        subscriberNode.newSubscriber("foo", "std_msgs/String",
            new MessageListener<org.ros.message.std_msgs.String>() {
              @Override
              public void onNewMessage(org.ros.message.std_msgs.String message) {
                assertEquals(helloMessage, message);
                messageReceived.countDown();
              }
            });

    assertTrue(publisher.awaitRegistration(1, TimeUnit.SECONDS));
    assertTrue(subscriber.awaitRegistration(1, TimeUnit.SECONDS));

    RepeatingPublisher<org.ros.message.std_msgs.String> repeatingPublisher =
        new RepeatingPublisher<org.ros.message.std_msgs.String>(publisher, helloMessage, 1000);
    repeatingPublisher.start();

    assertTrue(messageReceived.await(1, TimeUnit.SECONDS));

    repeatingPublisher.cancel();
    publisher.shutdown();
  }

  @Test
  public void testAddDisconnectedPublisher() {
    Node subscriberNode = nodeFactory.newNode("subscriber", nodeConfiguration);
    org.ros.internal.node.topic.DefaultSubscriber<org.ros.message.std_msgs.String> subscriber =
        (org.ros.internal.node.topic.DefaultSubscriber<org.ros.message.std_msgs.String>) subscriberNode
            .<org.ros.message.std_msgs.String>newSubscriber("foo", "std_msgs/String", null);

    try {
      TopicDefinition topicDefinition =
          TopicDefinition.create(new GraphName("foo"),
              MessageDefinition.createFromTypeName("std_msgs/String"));
      subscriber.addPublisher(
          PublisherDefinition.createPublisherDefinition(
              SlaveIdentifier.newFromStrings("foo", "http://foo"), topicDefinition),
          new InetSocketAddress(1234));
      fail();
    } catch (RuntimeException e) {
      // Connecting to a disconnected publisher should fail.
    }
  }

}
