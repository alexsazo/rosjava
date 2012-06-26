/*
 * Copyright (C) 2012 Google Inc.
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

package org.ros.internal.message;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.StackObjectPool;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.exception.RosRuntimeException;

import java.nio.ByteOrder;

/**
 * Provides {@link ChannelBuffer}s for serializing and deserializing messages.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MessageBuffers {

  private static final int ESTIMATED_LENGTH = 256;

  private final ObjectPool<ChannelBuffer> pool;

  /**
   * @return a new {@link ChannelBuffer} for {@link Message} serialization that
   *         grows dynamically
   */
  public static ChannelBuffer dynamicBuffer() {
    return ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, ESTIMATED_LENGTH);
  }

  public MessageBuffers() {
    pool = new StackObjectPool<ChannelBuffer>(new PoolableObjectFactory<ChannelBuffer>() {
      @Override
      public ChannelBuffer makeObject() throws Exception {
        return dynamicBuffer();
      }

      @Override
      public void destroyObject(ChannelBuffer channelBuffer) throws Exception {
      }

      @Override
      public boolean validateObject(ChannelBuffer channelBuffer) {
        return true;
      }

      @Override
      public void activateObject(ChannelBuffer channelBuffer) throws Exception {
      }

      @Override
      public void passivateObject(ChannelBuffer channelBuffer) throws Exception {
      }
    });
  }

  /**
   * Borrowed {@link ChannelBuffer}s must be returned using
   * {@link #returnChannelBuffer(ChannelBuffer)}.
   * 
   * @return a borrowed {@link ChannelBuffer}
   */
  public ChannelBuffer borrowChannelBuffer() {
    try {
      return pool.borrowObject();
    } catch (Exception e) {
      throw new RosRuntimeException(e);
    }
  }

  /**
   * Return a previously borrowed {@link ChannelBuffer}.
   * 
   * @param channelBuffer
   *          the {@link ChannelBuffer} to return
   */
  public void returnChannelBuffer(ChannelBuffer channelBuffer) {
    try {
      pool.returnObject(channelBuffer);
    } catch (Exception e) {
      throw new RosRuntimeException(e);
    }
  }
}
