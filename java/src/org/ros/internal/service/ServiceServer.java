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

package org.ros.internal.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.ros.internal.transport.tcp.TcpServer;

import org.ros.internal.transport.ConnectionHeader;
import org.ros.internal.transport.ConnectionHeaderFields;

import org.ros.internal.topic.Publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.message.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public abstract class ServiceServer<RequestMessageType extends Message> {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(Publisher.class);

  private final TcpServer server;
  private final Class<RequestMessageType> requestMessageClass;
  private final Collection<PersistentSession> persistentSessions;
  private final ServiceDefinition definition;
  private final Map<String, String> header;
  private final String name;

  private class Server extends TcpServer {
    public Server(String hostname, int port) throws IOException {
      super(hostname, port);
    }

    @Override
    protected void onNewConnection(Socket socket) {
      try {
        handshake(socket);
        PersistentSession session = new PersistentSession(socket);
        persistentSessions.add(session);
        session.start();
      } catch (IOException e) {
        log.error("Failed to accept connection.", e);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ros.transport.tcp.TcpServer#shutdown()
     */
    @Override
    public void shutdown() {
      super.shutdown();
      for (PersistentSession session : persistentSessions) {
        session.cancel();
      }
    }
  }

  private class PersistentSession extends Thread {
    private final ServiceServerOutgoingMessageQueue out;
    private final ServiceServerIncomingMessageQueue<RequestMessageType> in;

    public PersistentSession(Socket socket) throws IOException {
      in = new ServiceServerIncomingMessageQueue<RequestMessageType>(requestMessageClass);
      in.setSocket(socket);
      out = new ServiceServerOutgoingMessageQueue();
      out.addSocket(socket);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
      in.start();
      out.start();
      while (!Thread.currentThread().isInterrupted()) {
        try {
          RequestMessageType message = in.take();
          out.add(buildResponse(message));
        } catch (InterruptedException e) {
          // Cancelable
        }
      }
    }

    public void cancel() {
      interrupt();
      server.shutdown();
      in.shutdown();
      out.shutdown();
    }
  }

  public ServiceServer(Class<RequestMessageType> requestMessageClass, String name,
      ServiceDefinition definition, String hostname, int port) throws IOException {
    this.requestMessageClass = requestMessageClass;
    this.name = name;
    this.definition = definition;
    server = new Server(hostname, port);
    persistentSessions = Lists.newArrayList();
    header = ImmutableMap.<String, String>builder()
        .put(ConnectionHeaderFields.SERVICE, name)
        .putAll(definition.toHeader()).build();
  }

  /**
   * @param requestMessage
   * @return
   */
  public abstract Message buildResponse(RequestMessageType requestMessage);

  @VisibleForTesting
  void handshake(Socket socket) throws IOException {
    Map<String, String> incomingHeader = ConnectionHeader.readHeader(socket.getInputStream());
    if (DEBUG) {
      log.info("Incoming handshake header: " + incomingHeader);
      log.info("Outgoing handshake header: " + header);
    }
    if (incomingHeader.containsKey(ConnectionHeaderFields.PROBE)) {
      // TODO(damonkohler): Either return a bool or handle the closed socket later.
      socket.close();
      return;
    }
    Preconditions.checkState(incomingHeader.get(ConnectionHeaderFields.MD5_CHECKSUM).equals(
        header.get(ConnectionHeaderFields.MD5_CHECKSUM)));
    ConnectionHeader.writeHeader(header, socket.getOutputStream());
  }

  public void start() {
    server.start();
  }

  public void shutdown() {
    server.shutdown();
  }

  /**
   * @return
   */
  public InetSocketAddress getAddress() {
    return server.getAddress();
  }

  public URI getUri() throws URISyntaxException {
    return new URI("rosrpc://" + server.getAddress().getHostName() + ":"
        + server.getAddress().getPort());
  }

  /**
   * @return
   */
  public ServiceDefinition getServiceDefinition() {
    return definition;
  }

  /**
   * @return
   */
  public String getName() {
    return name;
  }

}
