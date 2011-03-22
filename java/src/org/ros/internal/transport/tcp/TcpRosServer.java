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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.ros.internal.node.address.AdvertiseAddress;
import org.ros.internal.node.address.BindAddress;
import org.ros.internal.node.server.ServiceManager;
import org.ros.internal.node.topic.TopicManager;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class TcpRosServer {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(TcpRosServer.class);

  private final BindAddress bindAddress;
  private final AdvertiseAddress advertiseAddress;
  private final ChannelGroup channelGroup;
  private final ChannelFactory channelFactory;
  private final ServerBootstrap bootstrap;

  private Channel channel;

  public TcpRosServer(BindAddress bindAddress, AdvertiseAddress advertiseAddress,
      TopicManager topicManager, ServiceManager serviceManager) {
    this.bindAddress = bindAddress;
    this.advertiseAddress = advertiseAddress;
    channelGroup = new DefaultChannelGroup();
    channelFactory =
        new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool());
    bootstrap = new ServerBootstrap(channelFactory);
    bootstrap.setOption("child.bufferFactory",
        new HeapChannelBufferFactory(ByteOrder.LITTLE_ENDIAN));
    TcpServerPipelineFactory pipelineFactory =
        new TcpServerPipelineFactory(channelGroup, topicManager, serviceManager);
    bootstrap.setPipelineFactory(pipelineFactory);
  }

  public void start() {
    channel = bootstrap.bind(bindAddress.toInetSocketAddress());
    advertiseAddress.setPortCallable(new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        return ((InetSocketAddress) channel.getLocalAddress()).getPort();
      }
    });
    if (DEBUG) {
      log.info("Bound to: " + bindAddress);
      log.info("Advertising: " + advertiseAddress);
    }
  }

  public void shutdown() {
    if (DEBUG) {
      log.info("Shutting down: " + getAddress());
    }
    ChannelGroupFuture future = channelGroup.close();
    future.awaitUninterruptibly();
    channelFactory.releaseExternalResources();
    bootstrap.releaseExternalResources();
    channel = null;
  }

  /**
   * @return the advertisable address of this {@link TcpRosServer}
   */
  public InetSocketAddress getAddress() {
    return advertiseAddress.toInetSocketAddress();
  }

  /**
   * @return the {@link AdvertiseAddress} of this {@link TcpRosServer}
   */
  public AdvertiseAddress getAdvertiseAddress() {
    return advertiseAddress;
  }

}
