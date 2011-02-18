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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.node.Response;
import org.ros.node.StatusCode;
import org.ros.node.server.SlaveServer;
import org.ros.topic.Publisher;
import org.ros.transport.ProtocolDescription;

import java.net.URL;
import java.util.List;
import java.util.Set;

public class SlaveImpl implements Slave {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(SlaveImpl.class);

  private final SlaveServer slave;

  public SlaveImpl(SlaveServer slave) {
    this.slave = slave;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ros.node.xmlrpc.Slave#getBusStats(java.lang.String)
   */
  @Override
  public List<Object> getBusStats(String callerId) {
    return slave.getBusStats(callerId);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ros.node.xmlrpc.Slave#getBusInfo(java.lang.String)
   */
  @Override
  public List<Object> getBusInfo(String callerId) {
    return slave.getBusInfo(callerId);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ros.node.xmlrpc.Slave#getMasterUri(java.lang.String)
   */
  @Override
  public List<Object> getMasterUri(String callerId) {
    URL url = slave.getMasterUri(callerId);
    return new Response<String>(StatusCode.SUCCESS, "", url.toString()).toList();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ros.node.xmlrpc.Slave#shutdown(java.lang.String, java.lang.String)
   */
  @Override
  public List<Object> shutdown(String callerId, String message) {
    slave.shutdown(callerId, message);
    return Response.createSuccess("Shutdown successful.", null).toList();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ros.node.xmlrpc.Slave#getPid(java.lang.String)
   */
  @Override
  public List<Object> getPid(String callerId) {
    return slave.getPid(callerId);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ros.node.xmlrpc.Slave#getSubscriptions(java.lang.String)
   */
  @Override
  public List<Object> getSubscriptions(String callerId) {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ros.node.xmlrpc.Slave#getPublications(java.lang.String)
   */
  @Override
  public List<Object> getPublications(String callerId) {
    List<Publisher> publishers = slave.getPublications();
    List<List<String>> publications = Lists.newArrayList();
    for (Publisher publisher : publishers) {
      publications.add(publisher.getTopicDescriptionAsList());
    }
    return Response.createSuccess("Success", publications).toList();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ros.node.xmlrpc.Slave#paramUpdate(java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @Override
  public List<Object> paramUpdate(String callerId, String parameterKey, String parameterValue) {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ros.node.xmlrpc.Slave#publisherUpdate(java.lang.String,
   * java.lang.String, java.lang.Object[])
   */
  @Override
  public List<Object> publisherUpdate(String callerId, String topic, Object[] publishers) {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ros.node.xmlrpc.Slave#requestTopic(java.lang.String,
   * java.lang.String, java.lang.Object[])
   */
  @Override
  public List<Object> requestTopic(String callerId, String topic, Object[] protocols) {
    Set<String> requestedProtocols = Sets.newHashSet();
    for (int i = 0; i < protocols.length; i++) {
      requestedProtocols.add((String) ((Object[]) protocols[i])[0]);
    }
    ProtocolDescription protocol = slave.requestTopic(topic, requestedProtocols);
    List<Object> response = Response.createSuccess(protocol.toString(), protocol.toList()).toList();
    if (DEBUG) {
      log.info("requestTopic(" + topic + ", " + requestedProtocols + ") response: "
          + response.toString());
    }
    return response;
  }

}
