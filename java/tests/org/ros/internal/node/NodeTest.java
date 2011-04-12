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

package org.ros.internal.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.ros.internal.namespace.GraphName;
import org.ros.internal.node.address.AdvertiseAddress;
import org.ros.internal.node.address.BindAddress;
import org.ros.internal.node.server.MasterServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * @author kwc@willowgarage.com (Ken Conley)
 */
public class NodeTest {

  @Test
  public void testCreatePublic() throws Exception {
    MasterServer masterServer =
        new MasterServer(BindAddress.createPublic(0), AdvertiseAddress.createPublic());
    masterServer.start();


    String hostName = InetAddress.getLocalHost().getCanonicalHostName();
    
    Node node = Node.createPublic(new GraphName("/node_name"), masterServer.getUri(), hostName, 0, 0);

    InetSocketAddress tcpRosAddress = node.getTcpRosServer().getAddress();
    assertTrue(tcpRosAddress.getPort() > 0);
    assertEquals(tcpRosAddress.getHostName(), hostName);

    URI uri = node.getSlaveServer().getUri();
    assertTrue(uri.getPort() > 0);
    assertEquals(hostName, uri.getHost());

    node.stop();
  }

}
