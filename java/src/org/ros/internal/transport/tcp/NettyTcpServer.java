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

package org.ros.internal.transport.tcp;

import com.google.common.base.Preconditions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteOrder;
import java.util.concurrent.Executors;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class NettyTcpServer {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(NettyTcpServer.class);

  private final ChannelGroup channelGroup;
  private final ChannelFactory channelFactory;
  private final ServerBootstrap bootstrap;

  private Channel channel;

  private final class ConnectionTrackingHandler extends SimpleChannelHandler {
    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
      channelGroup.add(e.getChannel());
    }
  }

  private final class PipelineFactory implements ChannelPipelineFactory {
    private final ChannelPipeline pipeline;

    public PipelineFactory(SimpleChannelHandler channelHandler) {
      pipeline =
          Channels.pipeline(new ConnectionTrackingHandler(), new LengthFieldBasedFrameDecoder(
              Integer.MAX_VALUE, 0, 4, 0, 4), new LengthFieldPrepender(4), channelHandler);
    }

    @Override
    public ChannelPipeline getPipeline() {
      return pipeline;
    }
  }

  public NettyTcpServer(SimpleChannelHandler channelHandler) {
    channelGroup = new DefaultChannelGroup();
    channelFactory =
        new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool());
    bootstrap = new ServerBootstrap(channelFactory);
    bootstrap.setPipelineFactory(new PipelineFactory(channelHandler));
    bootstrap.setOption("child.bufferFactory",
        new HeapChannelBufferFactory(ByteOrder.LITTLE_ENDIAN));
  }

  public void start(SocketAddress address) {
    channel = bootstrap.bind(address);
    if (DEBUG) {
      log.info("TCP server bound to: " + getAddress());
    }
  }

  public void shutdown() {
    if (DEBUG) {
      log.info("TCP server shutting down." + getAddress());
    }
    ChannelGroupFuture future = channelGroup.close();
    future.awaitUninterruptibly();
    channelFactory.releaseExternalResources();
  }

  public InetSocketAddress getAddress() {
    Preconditions
        .checkNotNull(channel, "Calling getAddress() is only valid after calling start().");
    return (InetSocketAddress) channel.getLocalAddress();
  }

}
