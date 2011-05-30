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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import org.ros.internal.namespace.GraphName;
import org.ros.internal.node.RemoteException;
import org.ros.internal.node.address.AdvertiseAddress;
import org.ros.internal.node.address.BindAddress;
import org.ros.internal.node.client.SlaveClient;
import org.ros.internal.node.service.ServiceIdentifier;
import org.ros.internal.node.topic.PublisherIdentifier;
import org.ros.internal.node.topic.SubscriberIdentifier;
import org.ros.internal.node.xmlrpc.MasterImpl;
import org.ros.internal.node.xmlrpc.XmlRpcTimeoutException;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MasterServer extends NodeServer {

  private final Map<String, SlaveIdentifier> slaves;
  private final Map<String, ServiceIdentifier> services;
  private final Multimap<String, PublisherIdentifier> publishers;
  private final Multimap<String, SubscriberIdentifier> subscribers;

  public MasterServer(BindAddress bindAddress, AdvertiseAddress advertiseAddress) {
    super(bindAddress, advertiseAddress);
    slaves = Maps.newConcurrentMap();
    services = Maps.newConcurrentMap();
    publishers =
        Multimaps.synchronizedMultimap(ArrayListMultimap.<String, PublisherIdentifier>create());
    subscribers =
        Multimaps.synchronizedMultimap(ArrayListMultimap.<String, SubscriberIdentifier>create());
  }

  public void start() {
    super.start(org.ros.internal.node.xmlrpc.MasterImpl.class, new MasterImpl(this));
  }

  public void registerService(ServiceIdentifier description) {
    services.put(description.getName().toString(), description);
  }

  public List<Object> unregisterService(String callerId, String service, String serviceApi) {
    return null;
  }

  private void addSlave(SlaveIdentifier description) {
    String name = description.getName().toString();
    Preconditions.checkState(slaves.get(name) == null || slaves.get(name).equals(description));
    slaves.put(name, description);
  }

  private void publisherUpdate(String topicName) throws XmlRpcTimeoutException, RemoteException {
    for (SlaveIdentifier slaveIdentifier : slaves.values()) {
      // TODO(damonkohler): Should the master server know its node name here?
      SlaveClient client;
      try {
        client = new SlaveClient(GraphName.createUnknown(), slaveIdentifier.getUri());
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
      List<URI> publisherUris = Lists.newArrayList();
      for (PublisherIdentifier identifier : publishers.get(topicName)) {
        publisherUris.add(identifier.getSlaveUri());
      }
      client.publisherUpdate(topicName, publisherUris);
    }
  }

  /**
   * Subscribe the caller to the specified topic. In addition to receiving a
   * list of current publishers, the subscriber will also receive notifications
   * of new publishers via the publisherUpdate API.
   * 
   * @param description
   * @return Publishers is a list of XMLRPC API URIs for nodes currently
   *         publishing the specified topic.
   */
  public List<PublisherIdentifier> registerSubscriber(SubscriberIdentifier description) {
    subscribers.put(description.getTopicName().toString(), description);
    addSlave(description.getSlaveIdentifier());
    return ImmutableList.copyOf(publishers.get(description.getTopicName().toString()));
  }

  public List<Object> unregisterSubscriber(String callerId, String topic, String callerApi) {
    return null;
  }

  /**
   * Register the caller as a publisher the topic.
   * 
   * @param callerId
   *          ROS caller ID
   * @return List of current subscribers of topic in the form of XML-RPC URIs.
   * @throws RemoteException
   * @throws XmlRpcTimeoutException
   * @throws MalformedURLException
   */
  public List<SubscriberIdentifier> registerPublisher(String callerId,
      PublisherIdentifier description) throws MalformedURLException, XmlRpcTimeoutException,
      RemoteException {
    publishers.put(description.getTopicName().toString(), description);
    addSlave(description.getSlaveIdentifier());
    publisherUpdate(description.getTopicName().toString());
    return ImmutableList.copyOf(subscribers.get(description.getTopicName().toString()));
  }

  public List<Object> unregisterPublisher(String callerId, String topic, String callerApi) {
    return null;
  }

  /**
   * Returns a {@link SlaveIdentifier} for the node with the given name. This
   * API is for looking information about publishers and subscribers. Use
   * lookupService instead to lookup ROS-RPC URIs.
   * 
   * @param callerId
   *          ROS caller ID
   * @param nodeName
   *          name of node to lookup
   * @return a {@link SlaveIdentifier} for the node with the given name
   */
  public SlaveIdentifier lookupNode(String callerId, String nodeName) {
    return slaves.get(nodeName);
  }

  public List<PublisherIdentifier> getPublishedTopics(String callerId, String subgraph) {
    // TODO(damonkohler): Add support for subgraph filtering.
    Preconditions.checkArgument(subgraph.length() == 0);
    return ImmutableList.copyOf(publishers.values());
  }

  public List<Object> getSystemState(String callerId) {
    return null;
  }

  /**
   * Lookup the provider of a particular service.
   * 
   * @param callerId
   *          ROS caller ID
   * @param service
   *          Fully-qualified name of service
   * @return service URI that provides address and port of the service. Fails if
   *         there is no provider.
   */
  public ServiceIdentifier lookupService(String callerId, String service) {
    return services.get(service);
  }

}
