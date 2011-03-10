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

package org.ros.internal.topic;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.MessageListener;
import org.ros.internal.node.server.SlaveIdentifier;
import org.ros.internal.transport.ConnectionHeaderFields;
import org.ros.message.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Subscriber<MessageType extends Message> extends Topic {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(Subscriber.class);

  private final CopyOnWriteArrayList<MessageListener<MessageType>> listeners;
  private final SubscriberMessageQueue<MessageType> in;
  private final MessageReadingThread thread;
  @VisibleForTesting
  final ImmutableMap<String, String> header;

  /* current list of publishers for topic */
  private final List<TopicConnectionInfo> connections;
  private SubscriberIdentifier identifier;

  private final class MessageReadingThread extends Thread {

    @Override
    public void run() {
      try {
        while (!Thread.currentThread().isInterrupted()) {
          MessageType message = in.take();
          if (DEBUG) {
            log.info("Received message: " + message);
          }
          for (MessageListener<MessageType> listener : listeners) {
            if (Thread.currentThread().isInterrupted()) {
              break;
            }
            // TODO(damonkohler): Recycle Message objects to avoid GC.
            listener.onNewMessage(message);
          }
        }
      } catch (InterruptedException e) {
        // Cancelable
        if (DEBUG) {
          log.info("Canceled.");
        }
      }
    }

    public void cancel() {
      interrupt();
    }
  }

  public static <S extends Message> Subscriber<S> create(SlaveIdentifier slaveIdentifier,
      TopicDefinition description, Class<S> messageClass) {
    return new Subscriber<S>(slaveIdentifier, description, messageClass);
  }

  private Subscriber(SlaveIdentifier slaveIdentifier, TopicDefinition description,
      Class<MessageType> messageClass) {
    super(description);
    this.listeners = new CopyOnWriteArrayList<MessageListener<MessageType>>();
    this.in = new SubscriberMessageQueue<MessageType>(messageClass);
    thread = new MessageReadingThread();
    header = ImmutableMap.<String, String>builder()
        .put(ConnectionHeaderFields.CALLER_ID, slaveIdentifier.getName())
        .putAll(description.toHeader()).build();
    connections = new ArrayList<TopicConnectionInfo>();
    identifier = new SubscriberIdentifier(slaveIdentifier, description);
  }

  public void addMessageListener(MessageListener<MessageType> listener) {
    listeners.add(listener);

    if (listeners.size() > 0) {
      // TODO(kwc): send event to start registration with master.
    }
  }

  public void removeMessageListener(MessageListener<MessageType> listener) {
    listeners.remove(listener);

    // TODO(kwc) : Contracts on who does setup/teardown of resources is really
    // unclear right now. Also, we need to do much more cleanup than this, such
    // as unregistering
    // with the master. Similarly, there needs to be logic in
    // addMessageCallbackListener to start the thread back up. Also, should we
    // be using listeners as a proxy for the # of Subscriber handles, or should
    // we track those explicitly?
    if (listeners.size() == 0) {
      thread.interrupt();
    }
  }

  public synchronized void addPublisher(PublisherIdentifier publisherIdentifier,
      InetSocketAddress tcprosServerAddress) throws IOException {
    TcprosConnection socketConnection = TcprosConnection
        .createOutgoing(tcprosServerAddress, header);

    // TODO(kwc): need to upgrade 'in' to allow multiple sockets.
    // TODO(kwc): cleanup API between Connection and socket abstraction
    // leveling.
    in.setSocket(socketConnection.getSocket());
    in.start();
    if (!thread.isAlive()) {
      // TODO(kwc): race condition if thread is in interrupted state

      thread.start();
    }

    connections.add(new TopicConnectionInfo(publisherIdentifier, identifier, socketConnection));
  }

  public void shutdown() {
    thread.cancel();
    in.shutdown();
  }

  /**
   * Updates list of publishers for topic this subscriber is interested in. This
   * method is non-blocking (i.e. connections to new publishers are done in
   * background).
   * 
   * @param publishers
   *          Full list of publishers for topic.
   */
  public synchronized void updatePublishers(List<PublisherIdentifier> publishers) {

    // Find new connections.
    ArrayList<PublisherIdentifier> toAdd = new ArrayList<PublisherIdentifier>();
    for (PublisherIdentifier publisherIdentifier : publishers) {
      boolean newConnection = true;
      for (TopicConnectionInfo connection : connections) {
        if (publisherIdentifier.equals(connection.getPublisherIdentifier())) {
          newConnection = false;
        }
      }
      if (newConnection) {
        toAdd.add(publisherIdentifier);
      }
    }
    // TODO: need a job queue to start creating these connections
  }

}
