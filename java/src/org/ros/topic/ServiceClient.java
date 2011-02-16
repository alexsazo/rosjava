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
import com.google.common.collect.ImmutableMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.message.Message;
import org.ros.transport.ConnectionHeader;
import org.ros.transport.ConnectionHeaderFields;
import org.ros.transport.tcp.IncomingMessageQueue;
import org.ros.transport.tcp.OutgoingMessageQueue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class ServiceClient<T extends Message> {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(ServiceClient.class);
  
  private final OutgoingMessageQueue out;
  private final IncomingMessageQueue<T> in;

  public static <S extends Message> ServiceClient<S> create(
      Class<S> incomingMessageClass, String name, ServiceDefinition serviceDefinition) {
    return new ServiceClient<S>(incomingMessageClass, name, serviceDefinition);
  }

  private Map<String, String> header;
  
  private ServiceClient(Class<T> incomingMessageClass, String name,
      ServiceDefinition serviceDefinition) {
    header = ImmutableMap.<String, String>builder()
        .put(ConnectionHeaderFields.CALLER_ID, name)
        .putAll(serviceDefinition.toHeader())
        .build();
    in = IncomingMessageQueue.create(incomingMessageClass);
    out = new OutgoingMessageQueue();
  }

  public void start(InetSocketAddress server) throws UnknownHostException, IOException {
    Socket socket = new Socket(server.getHostName(), server.getPort());
    handshake(socket);
    in.setSocket(socket);
    in.start();
    out.addSocket(socket);
    out.start();
  }

  @VisibleForTesting
  void handshake(Socket socket) throws IOException {
    ConnectionHeader.writeHeader(header, socket.getOutputStream());
    Map<String, String> incomingHeader = ConnectionHeader.readHeader(socket.getInputStream());
    if (DEBUG) {
      log.info("Incoming handshake header: " + incomingHeader);
      log.info("Expected handshake header: " + header);
    }
    Preconditions.checkState(incomingHeader.get(ConnectionHeaderFields.TYPE).equals(
        header.get(ConnectionHeaderFields.TYPE)));
    Preconditions.checkState(incomingHeader.get(ConnectionHeaderFields.MD5_CHECKSUM).equals(
        header.get(ConnectionHeaderFields.MD5_CHECKSUM)));
  }

  /**
   * @param message
   * @throws InterruptedException 
   */
  public T call(Message message) throws InterruptedException {
    out.add(message);
    return in.take();
  }

}
