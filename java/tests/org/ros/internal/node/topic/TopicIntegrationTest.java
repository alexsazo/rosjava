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

package org.ros.internal.node.topic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.ros.MessageListener;
import org.ros.internal.message.MessageDefinition;
import org.ros.internal.namespace.GraphName;
import org.ros.internal.node.Node;
import org.ros.internal.node.address.AdvertiseAddress;
import org.ros.internal.node.address.BindAddress;
import org.ros.internal.node.server.MasterServer;
import org.ros.internal.node.server.SlaveIdentifier;
import org.ros.message.MessageDeserializer;
import org.ros.message.MessageSerializer;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class TopicIntegrationTest {

  private MasterServer masterServer;

  @Before
  public void setUp() {
    masterServer = new MasterServer(BindAddress.createPublic(0), AdvertiseAddress.createPublic());
    masterServer.start();
  }

  @Test
  public void testOnePublisherToOneSubscriber() throws InterruptedException {
    TopicDefinition topicDefinition =
        new TopicDefinition(new GraphName("/foo"), MessageDefinition.create(
            org.ros.message.std_msgs.String.__s_getDataType(),
            org.ros.message.std_msgs.String.__s_getMessageDefinition(),
            org.ros.message.std_msgs.String.__s_getMD5Sum()));

    Node publisherNode =
        Node.createPrivate(new GraphName("/publisher"), masterServer.getUri(), 0, 0);
    final Publisher<org.ros.message.std_msgs.String> publisher =
        publisherNode.createPublisher(topicDefinition,
            new MessageSerializer<org.ros.message.std_msgs.String>());

    Node subscriberNode =
        Node.createPrivate(new GraphName("/subscriber"), masterServer.getUri(), 0, 0);
    Subscriber<org.ros.message.std_msgs.String> subscriber =
        subscriberNode.createSubscriber(topicDefinition, org.ros.message.std_msgs.String.class,
            new MessageDeserializer<org.ros.message.std_msgs.String>(
                org.ros.message.std_msgs.String.class));

    final org.ros.message.std_msgs.String helloMessage = new org.ros.message.std_msgs.String();
    helloMessage.data = "Hello, ROS!";

    final CountDownLatch messageReceived = new CountDownLatch(1);
    subscriber.addMessageListener(new MessageListener<org.ros.message.std_msgs.String>() {
      @Override
      public void onNewMessage(org.ros.message.std_msgs.String message) {
        assertEquals(helloMessage, message);
        messageReceived.countDown();
      }
    });

    publisher.awaitRegistration(100, TimeUnit.MILLISECONDS);
    subscriber.awaitRegistration(100, TimeUnit.MILLISECONDS);

    RepeatingPublisher<org.ros.message.std_msgs.String> repeatingPublisher =
        new RepeatingPublisher<org.ros.message.std_msgs.String>(publisher, helloMessage, 1000);
    repeatingPublisher.start();
    
    assertTrue(messageReceived.await(100, TimeUnit.MILLISECONDS));

    repeatingPublisher.cancel();
    publisher.shutdown();
  }

  @Test
  public void testAddDisconnectedPublisher() {
    TopicDefinition topicDefinition =
        new TopicDefinition(new GraphName("/foo"), MessageDefinition.create(
            org.ros.message.std_msgs.String.__s_getDataType(),
            org.ros.message.std_msgs.String.__s_getMessageDefinition(),
            org.ros.message.std_msgs.String.__s_getMD5Sum()));

    Node subscriberNode =
        Node.createPrivate(new GraphName("/subscriber"), masterServer.getUri(), 0, 0);
    Subscriber<org.ros.message.std_msgs.String> subscriber =
        subscriberNode.createSubscriber(topicDefinition, org.ros.message.std_msgs.String.class,
            new MessageDeserializer<org.ros.message.std_msgs.String>(
                org.ros.message.std_msgs.String.class));

    try {
      subscriber.addPublisher(
          new PublisherIdentifier(SlaveIdentifier.createFromStrings("foo", "http://foo"),
              topicDefinition), new InetSocketAddress(1234));
      fail();
    } catch (RuntimeException e) {
      // Connecting to a disconnected publisher should fail.
    }
  }

}
