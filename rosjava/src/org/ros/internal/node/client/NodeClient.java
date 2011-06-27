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

package org.ros.internal.node.client;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.ros.internal.node.server.NodeServer;
import org.ros.internal.node.xmlrpc.XmlRpcClientFactory;

import java.net.MalformedURLException;
import java.net.URI;

/**
 * @author damonkohler@google.com (Damon Kohler)
 * 
 * @param <NodeType>
 */
public class NodeClient<NodeType extends org.ros.internal.node.xmlrpc.Node> {

  private static final int CONNECTION_TIMEOUT = 60 * 1000; // 60 seconds
  private static final int REPLY_TIMEOUT = 60 * 1000; // 60 seconds
  private static final int XMLRPC_TIMEOUT = 10 * 1000; // 10 seconds

  private final URI uri;

  protected final NodeType node;

  public NodeClient(URI uri, Class<NodeType> interfaceClass) {
    this.uri = uri;
    XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
    try {
      config.setServerURL(uri.toURL());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
    config.setConnectionTimeout(CONNECTION_TIMEOUT);
    config.setReplyTimeout(REPLY_TIMEOUT);

    XmlRpcClient client = new XmlRpcClient();
    client.setTransportFactory(new XmlRpcCommonsTransportFactory(client));
    client.setConfig(config);

    XmlRpcClientFactory<NodeType> factory = new XmlRpcClientFactory<NodeType>(client);
    node =
        interfaceClass.cast(factory.newInstance(getClass().getClassLoader(), interfaceClass, "",
            XMLRPC_TIMEOUT));
  }

  /**
   * @return the {@link URI} of the remote {@link NodeServer}
   */
  public URI getRemoteUri() {
    return uri;
  }

}
