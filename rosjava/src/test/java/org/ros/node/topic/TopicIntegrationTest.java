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

import org.junit.Test;
import org.ros.RosTest;
import org.ros.concurrent.CancellableLoop;
import org.ros.internal.message.MessageDefinitionReflectionProvider;
import org.ros.internal.message.topic.TopicMessageFactory;
import org.ros.internal.node.topic.DefaultSubscriber;
import org.ros.internal.node.topic.PublisherIdentifier;
import org.ros.message.MessageDefinitionProvider;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.Node;
import org.ros.node.NodeMain;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Make sure publishers can talk with subscribers over a network connection.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public class TopicIntegrationTest extends RosTest {

  private final std_msgs.String expectedMessage;

  public TopicIntegrationTest() {
    MessageDefinitionProvider messageDefinitionProvider = new MessageDefinitionReflectionProvider();
    TopicMessageFactory topicMessageFactory = new TopicMessageFactory(messageDefinitionProvider);
    expectedMessage = topicMessageFactory.newFromType(std_msgs.String._TYPE);
    expectedMessage.setData("Would you like to play a game?");
  }

  @Test
  public void testOnePublisherToOneSubscriber() throws InterruptedException {
    nodeMainExecutor.execute(new NodeMain() {
      @Override
      public void onStart(Node node) {
        Publisher<std_msgs.String> publisher = node.newPublisher("foo", std_msgs.String._TYPE);
        publisher.setLatchMode(true);
        publisher.publish(expectedMessage);
      }

      @Override
      public void onShutdownComplete(Node node) {
      }

      @Override
      public void onShutdown(Node node) {
      }

      @Override
      public GraphName getDefaultNodeName() {
        return new GraphName("publisher");
      }
    }, nodeConfiguration);

    final CountDownLatch messageReceived = new CountDownLatch(1);
    nodeMainExecutor.execute(new NodeMain() {
      @Override
      public void onStart(Node node) {
        Subscriber<std_msgs.String> subscriber = node.newSubscriber("foo", std_msgs.String._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
          @Override
          public void onNewMessage(std_msgs.String message) {
            assertEquals(expectedMessage, message);
            messageReceived.countDown();
          }
        });
      }

      @Override
      public void onShutdownComplete(Node node) {
      }

      @Override
      public void onShutdown(Node node) {
      }

      @Override
      public GraphName getDefaultNodeName() {
        return new GraphName("subscriber");
      }
    }, nodeConfiguration);

    assertTrue(messageReceived.await(10, TimeUnit.SECONDS));
  }

  /**
   * This is a regression test.
   * 
   * @see <a
   *      href="http://answers.ros.org/question/3591/rosjava-subscriber-unreliable">bug
   *      report</a>
   * 
   * @throws InterruptedException
   */
  @Test
  public void testSubscriberStartsBeforePublisher() throws InterruptedException {
    final CountDownSubscriberListener<std_msgs.String> subscriberListener =
        CountDownSubscriberListener.newDefault();
    final CountDownLatch messageReceived = new CountDownLatch(1);
    nodeMainExecutor.execute(new NodeMain() {
      @Override
      public void onStart(Node node) {
        Subscriber<std_msgs.String> subscriber = node.newSubscriber("foo", std_msgs.String._TYPE);
        subscriber.addSubscriberListener(subscriberListener);
        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
          @Override
          public void onNewMessage(std_msgs.String message) {
            assertEquals(expectedMessage, message);
            messageReceived.countDown();
          }
        });
      }

      @Override
      public void onShutdownComplete(Node node) {
      }

      @Override
      public void onShutdown(Node node) {
      }

      @Override
      public GraphName getDefaultNodeName() {
        return new GraphName("subscriber");
      }
    }, nodeConfiguration);

    subscriberListener.awaitMasterRegistrationSuccess(1, TimeUnit.SECONDS);

    nodeMainExecutor.execute(new NodeMain() {
      @Override
      public void onStart(Node node) {
        Publisher<std_msgs.String> publisher = node.newPublisher("foo", std_msgs.String._TYPE);
        publisher.setLatchMode(true);
        publisher.publish(expectedMessage);
      }

      @Override
      public void onShutdownComplete(Node node) {
      }

      @Override
      public void onShutdown(Node node) {
      }

      @Override
      public GraphName getDefaultNodeName() {
        return new GraphName("publisher");
      }
    }, nodeConfiguration);

    assertTrue(messageReceived.await(10, TimeUnit.SECONDS));
  }

  @Test
  public void testAddDisconnectedPublisher() {
    nodeMainExecutor.execute(new NodeMain() {
      @Override
      public void onStart(Node node) {
        DefaultSubscriber<std_msgs.String> subscriber =
            (DefaultSubscriber<std_msgs.String>) node.<std_msgs.String>newSubscriber("foo",
                std_msgs.String._TYPE);
        try {
          subscriber.addPublisher(PublisherIdentifier.newFromStrings("foo", "http://foo", "foo"),
              new InetSocketAddress(1234));
          fail();
        } catch (RuntimeException e) {
          // Connecting to a disconnected publisher should fail.
        }
      }

      @Override
      public void onShutdownComplete(Node node) {
      }

      @Override
      public void onShutdown(Node node) {
      }

      @Override
      public GraphName getDefaultNodeName() {
        return new GraphName("subscriber");
      }
    }, nodeConfiguration);
  }

  private class Listener implements MessageListener<test_ros.TestHeader> {

    private final CountDownLatch latch = new CountDownLatch(10);

    private test_ros.TestHeader lastMessage;

    @Override
    public void onNewMessage(test_ros.TestHeader message) {
      if (lastMessage != null) {
        assertTrue(String.format("message seq %d <= previous seq %d", message.getHeader().getSeq(),
            lastMessage.getHeader().getSeq()), message.getHeader().getSeq() > lastMessage
            .getHeader().getSeq());
        assertTrue(message.getHeader().getStamp().compareTo(lastMessage.getHeader().getStamp()) > 0);
      }
      lastMessage = message;
      latch.countDown();
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
      return latch.await(timeout, unit);
    }
  }

  @Test
  public void testHeader() throws InterruptedException {
    nodeMainExecutor.execute(new NodeMain() {
      @Override
      public void onStart(final Node node) {
        final Publisher<test_ros.TestHeader> publisher =
            node.newPublisher("foo", test_ros.TestHeader._TYPE);
        CancellableLoop cancellableLoop = new CancellableLoop() {
          @Override
          public void loop() throws InterruptedException {
            test_ros.TestHeader testHeader =
                node.getTopicMessageFactory().newFromType(test_ros.TestHeader._TYPE);
            testHeader.getHeader().setFrameId("frame");
            testHeader.getHeader().setStamp(node.getCurrentTime());
            publisher.publish(testHeader);
            // There needs to be some time between messages in order to
            // guarantee that the timestamp increases.
            Thread.sleep(1);
          }
        };
        node.executeCancellableLoop(cancellableLoop);
      }

      @Override
      public void onShutdownComplete(Node node) {
      }

      @Override
      public void onShutdown(Node node) {
      }

      @Override
      public GraphName getDefaultNodeName() {
        return new GraphName("publisher");
      }
    }, nodeConfiguration);

    final Listener listener = new Listener();
    nodeMainExecutor.execute(new NodeMain() {
      @Override
      public void onStart(Node node) {
        Subscriber<test_ros.TestHeader> subscriber =
            node.newSubscriber("foo", test_ros.TestHeader._TYPE);
        subscriber.addMessageListener(listener);
      }

      @Override
      public void onShutdownComplete(Node node) {
      }

      @Override
      public void onShutdown(Node node) {
      }

      @Override
      public GraphName getDefaultNodeName() {
        return new GraphName("subscriber");
      }
    }, nodeConfiguration);

    assertTrue(listener.await(10, TimeUnit.SECONDS));
  }
}
