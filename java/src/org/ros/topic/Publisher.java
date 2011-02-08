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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.message.Message;
import org.ros.node.server.SlaveDescription;
import org.ros.transport.ConnectionHeader;
import org.ros.transport.ConnectionHeaderFields;
import org.ros.transport.tcp.OutgoingMessageQueue;
import org.ros.transport.tcp.TcpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Publisher extends Topic {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(Publisher.class);

  private final OutgoingMessageQueue out;
  private final List<SubscriberDescription> subscribers;
  private final TcpServer server;
  private final TopicDescription topicDescription;

  private class Server extends TcpServer {
    public Server(String hostname, int port) throws IOException {
      super(hostname, port);
    }

    @Override
    protected void onNewConnection(Socket socket) {
      try {
        handshake(socket);
        Preconditions.checkState(socket.isConnected());
        out.addSocket(socket);
      } catch (IOException e) {
        log.error("Failed to accept connection.", e);
      }
    }
  }

  public Publisher(TopicDescription topicDescription, String hostname, int port) throws IOException {
    super(topicDescription);
    this.topicDescription = topicDescription;
    out = new OutgoingMessageQueue();
    subscribers = Lists.newArrayList();
    server = new Server(hostname, port);
  }

  public void start() {
    server.start();
    out.start();
  }

  public void shutdown() {
    server.shutdown();
    out.shutdown();
  }

  public PublisherDescription toPublisherDescription(SlaveDescription description) {
    return new PublisherDescription(description, topicDescription);
  }

  public InetSocketAddress getAddress() {
    return server.getAddress();
  }

  public void publish(Message message) {
    if (DEBUG) {
      log.info("Publishing message: " + message);
    }
    out.add(message);
  }

  @VisibleForTesting
  void handshake(Socket socket) throws IOException {
    Map<String, String> incomingHeader = ConnectionHeader.readHeader(socket.getInputStream());
    Map<String, String> header = topicDescription.toHeader();
    if (DEBUG) {
      log.info("Incoming handshake header: " + incomingHeader);
      log.info("Expected handshake header: " + header);
    }
    Preconditions.checkState(incomingHeader.get(ConnectionHeaderFields.TYPE).equals(
        header.get(ConnectionHeaderFields.TYPE)));
    Preconditions.checkState(incomingHeader.get(ConnectionHeaderFields.MD5_CHECKSUM).equals(
        header.get(ConnectionHeaderFields.MD5_CHECKSUM)));
    SubscriberDescription subscriber = SubscriberDescription.createFromHeader(incomingHeader);
    subscribers.add(subscriber);
    ConnectionHeader.writeHeader(header, socket.getOutputStream());
  }
}
