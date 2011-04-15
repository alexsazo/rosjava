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

package org.ros.internal.node.service;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;


/**
 * @author damonkohler@google.com (Damon Kohler)
 */
class ServiceRequestHandler extends SimpleChannelHandler {

  private final ServiceResponseBuilder<?, ?> responseBuilder;

  ServiceRequestHandler(ServiceResponseBuilder<?, ?> responseBuilder) {
    this.responseBuilder = responseBuilder;
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    ChannelBuffer requestBuffer = (ChannelBuffer) e.getMessage();
    ChannelBuffer responseBuffer =
        ChannelBuffers.wrappedBuffer(responseBuilder.handleRequest(requestBuffer.toByteBuffer()));
    ServiceServerResponse response = new ServiceServerResponse();
    // TODO(damonkohler): Support changing error code.
    response.setErrorCode(1);
    response.setMessageLength(responseBuffer.readableBytes());
    response.setMessage(responseBuffer);
    ctx.getChannel().write(response);
  }

}