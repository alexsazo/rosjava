/*
 * Copyright (C) 2009 Google Inc.
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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Preconditions;

/**
 * @author Damon Kohler (damonkohler@gmail.com)
 */
public abstract class Topic {

  private static final Log log = LogFactory.getLog(Topic.class);

  protected final TopicDescription description;
  protected final String hostname;

  private ServerThread thread;
  private ServerSocket server;

  public Topic(TopicDescription description, String hostname) {
    this.description = description;
    this.hostname = hostname;
  }

  protected abstract void onNewConnection(Socket socket);

  private final class ServerThread extends Thread {
    
    public ServerThread(int port) throws IOException {
      server = new ServerSocket(port);
    }

    @Override
    public void run() {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          onNewConnection(server.accept());
        } catch (IOException e) {
          log.error("Connection failed.", e);
        }
      }
    }

    public void cancel() {
      interrupt();
      try {
        server.close();
        server = null;
      } catch (IOException e) {
        log.error("Server shutdown failed.", e);
      }
    }
  }

  public void start(int port) throws IOException {
    Preconditions.checkState(thread == null);
    Preconditions.checkState(server == null);
    thread = new ServerThread(port);
    thread.start();
    log.info("Topic " + description.getName() + " bound to: " + getAddress());
  }

  public void shutdown() {
    Preconditions.checkNotNull(thread);
    Preconditions.checkNotNull(server);
    thread.cancel();
    thread = null;
  }

  public InetSocketAddress getAddress() {
    return InetSocketAddress.createUnresolved(hostname, server.getLocalPort());
  }

  public String getTopicName() {
    return description.getName();
  }

  public String getTopicType() {
    return description.getMessageType();
  }
}