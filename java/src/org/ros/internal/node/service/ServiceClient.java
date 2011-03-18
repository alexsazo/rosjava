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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.ros.internal.transport.ConnectionHeader;
import org.ros.internal.transport.ConnectionHeaderFields;
import org.ros.internal.transport.tcp.TcpClientPipelineFactory;
import org.ros.message.Message;

import java.net.SocketAddress;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class ServiceClient<ResponseMessageType extends Message> {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(ServiceClient.class);

  private final Queue<ServiceCallback<ResponseMessageType>> callbacks;
  final Class<ResponseMessageType> responseMessageClass;
  private final Map<String, String> header;
  private final ChannelFactory channelFactory;
  private final ClientBootstrap bootstrap;

  private Channel channel;

  private enum DecodingState {
    ERROR_CODE, MESSAGE_LENGTH, MESSAGE
  }

  private final class ResponseDecoder extends ReplayingDecoder<DecodingState> {
    private ServiceServerResponse response;

    public ResponseDecoder() {
      reset();
    }

    @SuppressWarnings("fallthrough")
    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer,
        DecodingState state) throws Exception {
      switch (state) {
        case ERROR_CODE:
          response.setErrorCode(buffer.readByte());
          checkpoint(DecodingState.MESSAGE_LENGTH);
        case MESSAGE_LENGTH:
          response.setMessageLength(buffer.readInt());
          checkpoint(DecodingState.MESSAGE);
        case MESSAGE:
          response.setMessage(buffer.readBytes(response.getMessageLength()));
          try {
            return response;
          } finally {
            reset();
          }
        default:
          throw new IllegalStateException();
      }
    }

    private void reset() {
      checkpoint(DecodingState.ERROR_CODE);
      response = new ServiceServerResponse();
    }
  }

  private final class HandshakeHandler extends SimpleChannelHandler {
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
      ChannelBuffer incomingBuffer = (ChannelBuffer) e.getMessage();
      // TODO(damonkohler): Handle handshake errors.
      handshake(incomingBuffer);
      ChannelPipeline pipeline = e.getChannel().getPipeline();
      pipeline.remove(TcpClientPipelineFactory.LENGTH_FIELD_BASED_FRAME_DECODER);
      pipeline.remove(this);
      pipeline.addLast("ResponseDecoder", new ResponseDecoder());
      pipeline.addLast("ResponseHandler", new ResponseHandler());
    }
  }

  private final class ResponseHandler extends SimpleChannelHandler {
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
      ServiceCallback<ResponseMessageType> callback = callbacks.poll();
      Preconditions.checkNotNull(callback);
      ServiceServerResponse response = (ServiceServerResponse) e.getMessage();
      ResponseMessageType message = responseMessageClass.newInstance();
      message.deserialize(response.getMessage().toByteBuffer());
      callback.run(message);
    }
  }

  public static <S extends Message> ServiceClient<S> create(Class<S> incomingMessageClass,
      String name, ServiceIdentifier serviceIdentifier) {
    return new ServiceClient<S>(incomingMessageClass, name, serviceIdentifier);
  }

  private ServiceClient(Class<ResponseMessageType> responseMessageClass, String nodeName,
      ServiceIdentifier serviceIdentifier) {
    Preconditions.checkNotNull(nodeName);
    Preconditions.checkArgument(nodeName.startsWith("/"));
    this.responseMessageClass = responseMessageClass;
    callbacks = Lists.newLinkedList();
    header = ImmutableMap.<String, String>builder().put(ConnectionHeaderFields.CALLER_ID, nodeName)
    // TODO(damonkohler): Support non-persistent connections.
        .put(ConnectionHeaderFields.PERSISTENT, "1").putAll(serviceIdentifier.toHeader()).build();
    channelFactory =
        new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool());
    bootstrap = new ClientBootstrap(channelFactory);
    TcpClientPipelineFactory factory = new TcpClientPipelineFactory();
    factory.getPipeline().addLast("HandshakeHandler", new HandshakeHandler());
    bootstrap.setPipelineFactory(factory);
    bootstrap.setOption("bufferFactory", new HeapChannelBufferFactory(ByteOrder.LITTLE_ENDIAN));
  }

  public void connect(SocketAddress address) {
    // TODO(damonkohler): Add timeouts.
    ChannelFuture future = bootstrap.connect(address).awaitUninterruptibly();
    if (future.isSuccess()) {
      channel = future.getChannel();
      if (DEBUG) {
        log.info("Connected to: " + channel.getRemoteAddress());
      }
    } else {
      throw new RuntimeException(future.getCause());
    }
    ChannelBuffer encodedHeader = ConnectionHeader.encode(header);
    channel.write(encodedHeader).awaitUninterruptibly();
  }

  public void shutdown() {
    channel.close().awaitUninterruptibly();
    channelFactory.releaseExternalResources();
    bootstrap.releaseExternalResources();
  }

  private void handshake(ChannelBuffer buffer) {
    Map<String, String> incomingHeader = ConnectionHeader.decode(buffer);
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
   */
  public void call(Message message, ServiceCallback<ResponseMessageType> callback) {
    // TODO(damonkohler): Make use of sequence number.
    ChannelBuffer buffer =
        ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, message.serialize(0));
    callbacks.add(callback);
    channel.write(buffer);
  }

}
