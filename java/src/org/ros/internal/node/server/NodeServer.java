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

package org.ros.internal.node.server;

import com.google.common.base.Preconditions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class NodeServer {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(NodeServer.class);

  private final String hostname;
  private final WebServer server;
  
  private boolean running;
  
  public NodeServer(String hostname, int port) {
    this.hostname = hostname;
    server = new WebServer(port);
    running = false;
  }

  public <T extends org.ros.internal.node.xmlrpc.Node> void start(Class<T> instanceClass,
      T instance) throws XmlRpcException, IOException, URISyntaxException {
    Preconditions.checkState(!running);
    XmlRpcServer xmlRpcServer = server.getXmlRpcServer();
    PropertyHandlerMapping phm = new PropertyHandlerMapping();
    phm.setRequestProcessorFactoryFactory(new NodeRequestProcessorFactoryFactory<T>(instance));
    phm.addHandler("", instanceClass);
    xmlRpcServer.setHandlerMapping(phm);
    XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
    serverConfig.setEnabledForExtensions(false);
    serverConfig.setContentLengthOptional(false);
    server.start();
    running = true;
    if (DEBUG) {
      log.info("Slave node bound to: " + getUri());
    }
  }

  public void shutdown() {
    Preconditions.checkState(running);
    server.shutdown();
  }
  
  public URI getUri() throws MalformedURLException, URISyntaxException {
    Preconditions.checkState(running);
    return new URL("http", hostname, server.getPort(), "").toURI();
  }
}
