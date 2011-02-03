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

package org.ros.node.server;

import java.io.IOException;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.ros.node.xmlrpc.MasterImpl;

public class Master extends Node {
  
  public void serveForever(int port) throws XmlRpcException, IOException {
    super.start(port, org.ros.node.xmlrpc.MasterImpl.class, new MasterImpl(this));
  }

  public List<Object> registerService(String callerId, String service, String serviceApi,
      String callerApi) {
    return null;
  }

  public List<Object> unregisterService(String callerId, String service, String serviceApi) {
    return null;
  }

  public List<Object> registerSubscriber(String callerId, String topic, String topicType,
      String callerApi) {
    return null;
  }

  public List<Object> unregisterSubscriber(String callerId, String topic, String callerApi) {
    return null;
  }

  public List<Object> registerPublisher(String callerId, String topic, String topicType,
      String callerApi) {
    return null;
  }

  public List<Object> unregisterPublisher(String callerId, String topic, String callerApi) {
    return null;
  }

  public List<Object> lookupNode(String callerId, String nodeName) {
    return null;
  }

  public List<Object> getPublishedTopics(String callerId, String subgraph) {
    return null;
  }

  public List<Object> getSystemState(String callerId) {
    return null;
  }

  public List<Object> getUri(String callerId) {
    return null;
  }

  public List<Object> lookupService(String callerId, String service) {
    return null;
  }
}