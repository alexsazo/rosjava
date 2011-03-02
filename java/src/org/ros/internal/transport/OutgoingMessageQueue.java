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

package org.ros.internal.transport;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.message.Message;

import com.google.common.collect.Lists;

import org.ros.internal.transport.LittleEndianDataOutputStream;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public abstract class OutgoingMessageQueue {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(OutgoingMessageQueue.class);

  private final Collection<LittleEndianDataOutputStream> streams;
  private final BlockingQueue<Message> messages;
  private final MessageSendingThread thread;

  private final class MessageSendingThread extends Thread {
    @Override
    public void run() {
      try {
        while (!Thread.currentThread().isInterrupted()) {
          sendMessageToAllStreams(messages.take());
        }
      } catch (InterruptedException e) {
        // Cancelable
      }
    }

    public void cancel() {
      interrupt();
      for (LittleEndianDataOutputStream stream : streams) {
        try {
          stream.close();
        } catch (IOException e) {
          log.error("Failed to close stream.", e);
        }
      }
    }
  }

  public OutgoingMessageQueue() {
    streams = Lists.newArrayList();
    messages = new LinkedBlockingQueue<Message>();
    thread = new MessageSendingThread();
  }

  public void add(Message message) {
    messages.add(message);
  }

  public void shutdown() {
    thread.cancel();
  }

  public void start() {
    thread.start();
  }

  public void addSocket(Socket socket) throws IOException {
    LittleEndianDataOutputStream out = new LittleEndianDataOutputStream(socket.getOutputStream());
    streams.add(out);
  }

  private void sendMessageToAllStreams(Message message) {
    Iterator<LittleEndianDataOutputStream> iterator = streams.iterator();
    while (iterator.hasNext()) {
      LittleEndianDataOutputStream out = iterator.next();
      if (out == null) {
        log.warn("out is null, removing from streams!");
        iterator.remove();
        continue;
      }
      try {
        sendMessage(message, out);
        out.flush();
      } catch (IOException e) {
        if (DEBUG) {
          log.info("Connection died.", e);
        }
        iterator.remove();
      }
    }
  }

  /**
   * @param message
   * @param out
   * @throws IOException
   */
  protected abstract void sendMessage(Message message, LittleEndianDataOutputStream stream)
      throws IOException;
}
