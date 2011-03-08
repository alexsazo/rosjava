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

import org.ros.internal.transport.IncomingMessageQueue;
import org.ros.internal.transport.LittleEndianDataInputStream;

import org.ros.message.Message;

import java.io.IOException;

/**
 * @author damonkohler@google.com (Damon Kohler)
 * 
 * @param <MessageType>
 */
public class SubscriberMessageQueue<MessageType extends Message> extends
    IncomingMessageQueue<MessageType> {

  /**
   * @param messageClass
   */
  public SubscriberMessageQueue(Class<MessageType> messageClass) {
    super(messageClass);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ros.transport.IncomingMessageQueue#receiveMessage(java.lang.Class,
   * org.ros.transport.LittleEndianDataInputStream)
   */
  @Override
  protected MessageType receiveMessage(Class<MessageType> messageClass,
      LittleEndianDataInputStream stream) throws IOException, InstantiationException,
      IllegalAccessException {
    int size = stream.readInt();
    byte[] buffer = stream.readByteArray(size);
    MessageType message = messageClass.newInstance();
    message.deserialize(buffer);
    return message;
  }
}
