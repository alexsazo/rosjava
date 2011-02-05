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

package org.ros.topic;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.communication.Message;
import org.ros.transport.Header;
import org.ros.transport.HeaderFields;
import org.ros.transport.IncomingMessageQueue;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Subscriber<T extends Message> extends Topic {

  private static final boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(Subscriber.class);

  private final CopyOnWriteArrayList<SubscriberListener<T>> listeners;
  private final IncomingMessageQueue<T> in;
  private final MessageReadingThread thread;
  private final ImmutableMap<String, String> header;
  private final Socket publisherSocket;

  public interface SubscriberListener<T extends Message> {
    public void onNewMessage(T message);
  }

  private final class MessageReadingThread extends Thread {
    @Override
    public void run() {
      try {
        while (!Thread.currentThread().isInterrupted()) {
          T message = in.take();
          if (DEBUG) {
            log.info("Received message: " + message);
          }
          for (SubscriberListener<T> listener : listeners) {
            if (isInterrupted()) {
              break;
            }
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

  public Subscriber(TopicDescription description, String name, Class<T> messageClass, Socket publisherSocket)
      throws IOException {
    super(description);
    this.publisherSocket = publisherSocket;
    this.listeners = new CopyOnWriteArrayList<Subscriber.SubscriberListener<T>>();
    this.in = new IncomingMessageQueue<T>(messageClass);
    thread = new MessageReadingThread();
    header = ImmutableMap.<String, String>builder()
        .put(HeaderFields.CALLER_ID, name)
        .putAll(description.toHeader())
        .build();
  }

  public void addListener(SubscriberListener<T> listener) {
    listeners.add(listener);
  }

  public void start() throws IOException {
    handshake(publisherSocket);
    in.setSocket(publisherSocket);
    in.start();
    thread.start();
  }

  public void shutdown() {
    thread.cancel();
    in.shutdown();
  }

  @VisibleForTesting
  void handshake(Socket socket) throws IOException {
    Header.writeHeader(header, socket.getOutputStream());
    Map<String, String> incomingHeader = Header.readHeader(socket.getInputStream());
    if (DEBUG) {
      log.info("Sent handshake header: " + header);
      log.info("Received handshake header: " + incomingHeader);
    }
    Preconditions.checkState(incomingHeader.get(HeaderFields.TYPE).equals(
        header.get(HeaderFields.TYPE)));
    Preconditions.checkState(incomingHeader.get(HeaderFields.MD5_CHECKSUM).equals(
        header.get(HeaderFields.MD5_CHECKSUM)));
  }
}
