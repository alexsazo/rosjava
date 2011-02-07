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

package org.ros.node.xmlrpc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.junit.Test;
import org.mockito.Matchers;
import org.ros.node.StatusCode;
import org.ros.topic.Publisher;
import org.ros.transport.ProtocolNames;
import org.ros.transport.TcpRosDescription;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class SlaveImplTest {

  @Test
  public void testGetPublicationsEmptyList() {
    org.ros.node.server.Slave mockSlave = mock(org.ros.node.server.Slave.class);
    when(mockSlave.getPublications()).thenReturn(Lists.<Publisher>newArrayList());
    SlaveImpl slave = new SlaveImpl(mockSlave);
    List<Object> response = slave.getPublications("/foo");
    assertEquals(response.get(0), StatusCode.SUCCESS.toInt());
    assertEquals(response.get(2), Lists.newArrayList());
  }

  @Test
  public void testGetPublications() {
    org.ros.node.server.Slave mockSlave = mock(org.ros.node.server.Slave.class);
    Publisher mockPublisher = mock(Publisher.class);
    when(mockSlave.getPublications()).thenReturn(Lists.newArrayList(mockPublisher));
    when(mockPublisher.getTopicName()).thenReturn("/bar");
    when(mockPublisher.getTopicMessageType()).thenReturn("/baz");
    when(mockPublisher.getTopicDescriptionAsList()).thenReturn(Lists.newArrayList("/bar", "/baz"));
    SlaveImpl slave = new SlaveImpl(mockSlave);
    List<Object> response = slave.getPublications("/foo");
    assertEquals(StatusCode.SUCCESS.toInt(), response.get(0));
    List<List<String>> protocols = Lists.newArrayList();
    protocols.add(Lists.newArrayList("/bar", "/baz"));
    assertEquals(protocols, response.get(2));
  }

  @Test
  public void testRequestTopic() {
    org.ros.node.server.Slave mockSlave = mock(org.ros.node.server.Slave.class);
    InetSocketAddress localhost = InetSocketAddress.createUnresolved("localhost", 1234);
    TcpRosDescription protocol = new TcpRosDescription(localhost);
    when(
        mockSlave.requestTopic(Matchers.<String>any(),
            Matchers.eq(Sets.newHashSet(ProtocolNames.TCPROS, ProtocolNames.UDPROS)))).thenReturn(
        protocol);
    SlaveImpl slave = new SlaveImpl(mockSlave);
    Object[][] protocols = new Object[][] { {ProtocolNames.TCPROS}, {ProtocolNames.UDPROS}};
    List<Object> response = slave.requestTopic("/foo", "/bar", protocols);
    assertEquals(response.get(0), StatusCode.SUCCESS.toInt());
    assertEquals(response.get(2), protocol.toList());
  }
}
