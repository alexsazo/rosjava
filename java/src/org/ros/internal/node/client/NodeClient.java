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
import org.apache.xmlrpc.client.util.ClientFactory;

import java.net.MalformedURLException;
import java.net.URI;

/**
 * @author damonkohler@google.com (Damon Kohler)
 * 
 * @param <T>
 */
public class NodeClient<T extends org.ros.internal.node.xmlrpc.Node> {
  
  protected final T node;
  private final URI uri;
  
  public NodeClient(URI uri, Class<T> interfaceClass) throws MalformedURLException {
    this.uri = uri;
    
    XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
    config.setServerURL(uri.toURL());
    config.setEnabledForExtensions(true);
    config.setConnectionTimeout(60 * 1000);
    config.setReplyTimeout(60 * 1000);

    XmlRpcClient client = new XmlRpcClient();
    client.setTransportFactory(new XmlRpcCommonsTransportFactory(client));
    client.setConfig(config);

    ClientFactory factory = new ClientFactory(client);
    node = interfaceClass.cast(factory.newInstance(getClass().getClassLoader(), interfaceClass, ""));
  }

  /**
   * @return the URL address of the remote node
   */
  public URI getRemoteUri() {
    return uri;
  }

}
