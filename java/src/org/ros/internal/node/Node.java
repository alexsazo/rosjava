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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.MessageDeserializer;
import org.ros.MessageSerializer;
import org.ros.internal.namespace.GraphName;
import org.ros.internal.node.address.AdvertiseAddress;
import org.ros.internal.node.address.BindAddress;
import org.ros.internal.node.client.MasterClient;
import org.ros.internal.node.response.Response;
import org.ros.internal.node.response.StatusCode;
import org.ros.internal.node.server.MasterServer;
import org.ros.internal.node.server.NodeServer;
import org.ros.internal.node.server.SlaveServer;
import org.ros.internal.node.service.ServiceClient;
import org.ros.internal.node.service.ServiceDefinition;
import org.ros.internal.node.service.ServiceIdentifier;
import org.ros.internal.node.service.ServiceManager;
import org.ros.internal.node.service.ServiceResponseBuilder;
import org.ros.internal.node.service.ServiceServer;
import org.ros.internal.node.topic.Publisher;
import org.ros.internal.node.topic.Subscriber;
import org.ros.internal.node.topic.TopicDefinition;
import org.ros.internal.node.topic.TopicManager;
import org.ros.internal.node.xmlrpc.XmlRpcTimeoutException;
import org.ros.message.Service;

import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Implementation of a ROS node. A {@link Node} is responsible for managing
 * various resources including XML-RPC, TCPROS servers, and topic/service
 * instances.
 * 
 * @author kwc@willowgarage.com (Ken Conley)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Node {

  private static final Log log = LogFactory.getLog(Node.class);

  private final GraphName nodeName;
  private final MasterClient masterClient;
  private final SlaveServer slaveServer;
  private final TopicManager topicManager;
  private final ServiceManager serviceManager;
  private final MasterRegistration masterRegistration;
  private final SubscriberFactory subscriberFactory;
  private final ServiceFactory serviceFactory;
  private final PublisherFactory publisherFactory;

  private boolean started;

  public static Node createPublic(GraphName nodeName, URI masterUri, String advertiseHostname,
      int xmlRpcBindPort, int tcpRosBindPort) {
    Node node =
        new Node(nodeName, masterUri, BindAddress.createPublic(tcpRosBindPort),
            new AdvertiseAddress(advertiseHostname), BindAddress.createPublic(xmlRpcBindPort),
            new AdvertiseAddress(advertiseHostname));
    node.start();
    return node;
  }

  public static Node createPrivate(GraphName nodeName, URI masterUri, int xmlRpcBindPort,
      int tcpRosBindPort) {
    Node node =
        new Node(nodeName, masterUri, BindAddress.createPrivate(tcpRosBindPort),
            AdvertiseAddress.createPrivate(), BindAddress.createPrivate(xmlRpcBindPort),
            AdvertiseAddress.createPrivate());
    node.start();
    return node;
  }

  Node(GraphName nodeName, URI masterUri, BindAddress tcpRosBindAddress,
      AdvertiseAddress tcpRosAdvertiseAddress, BindAddress xmlRpcBindAddress,
      AdvertiseAddress xmlRpcAdvertiseAddress) {
    this.nodeName = nodeName;
    started = false;
    masterClient = new MasterClient(masterUri);
    topicManager = new TopicManager();
    serviceManager = new ServiceManager();
    slaveServer =
        new SlaveServer(nodeName, tcpRosBindAddress, tcpRosAdvertiseAddress, xmlRpcBindAddress,
            xmlRpcAdvertiseAddress, masterClient, topicManager, serviceManager);
    masterRegistration = new MasterRegistration(masterClient);
    topicManager.setListener(masterRegistration);
    publisherFactory = new PublisherFactory(topicManager);
    subscriberFactory = new SubscriberFactory(slaveServer, topicManager);
    serviceFactory = new ServiceFactory(nodeName, slaveServer, serviceManager);
  }

  public <MessageType> Subscriber<MessageType> createSubscriber(TopicDefinition topicDefinition,
      Class<MessageType> messageClass, MessageDeserializer<MessageType> deserializer) {
    return subscriberFactory.create(topicDefinition, messageClass, deserializer);
  }

  public <MessageType> Publisher<MessageType> createPublisher(TopicDefinition topicDefinition,
      MessageSerializer<MessageType> serializer) {
    return publisherFactory.create(topicDefinition, serializer);
  }

  public <RequestType, ResponseType> ServiceServer createServiceServer(
      ServiceDefinition serviceDefinition,
      ServiceResponseBuilder<RequestType, ResponseType> responseBuilder) throws Exception {
    return serviceFactory.createServiceServer(serviceDefinition, responseBuilder);
  }

  public <ResponseMessageType> ServiceClient<ResponseMessageType> createServiceClient(
      ServiceIdentifier serviceIdentifier, MessageDeserializer<ResponseMessageType> deserializer) {
    return serviceFactory.createServiceClient(serviceIdentifier, deserializer);
  }

  void start() {
    if (started) {
      throw new IllegalStateException("Already started.");
    }
    started = true;
    slaveServer.start();
    masterRegistration.start(slaveServer.toSlaveIdentifier());
  }

  /**
   * Shutdown the node and make a best effort attempt to unregister all
   * {@link Publisher}s, {@link Subscriber}s, and {@link ServiceServer}s.
   */
  public void shutdown() {
    for (Publisher<?> publisher : topicManager.getPublishers()) {
      publisher.shutdown();
      // NOTE(damonkohler): We don't want to raise potentially spurious
      // exceptions during shutdown that would interrupt the process. This is
      // simply best effort cleanup.
      try {
        masterClient.unregisterPublisher(slaveServer.toSlaveIdentifier(), publisher);
      } catch (XmlRpcTimeoutException e) {
        log.error(e);
      } catch (RemoteException e) {
        log.error(e);
      }
    }
    for (Subscriber<?> subscriber : topicManager.getSubscribers()) {
      subscriber.shutdown();
      // NOTE(damonkohler): We don't want to raise potentially spurious
      // exceptions during shutdown that would interrupt the process. This is
      // simply best effort cleanup.
      try {
        masterClient.unregisterSubscriber(slaveServer.toSlaveIdentifier(), subscriber);
      } catch (XmlRpcTimeoutException e) {
        log.error(e);
      } catch (RemoteException e) {
        log.error(e);
      }
    }
    for (ServiceServer serviceServer : serviceManager.getServiceServers()) {
      // NOTE(damonkohler): We don't want to raise potentially spurious
      // exceptions during shutdown that would interrupt the process. This is
      // simply best effort cleanup.
      try {
        masterClient.unregisterService(slaveServer.toSlaveIdentifier(), serviceServer);
      } catch (XmlRpcTimeoutException e) {
        log.error(e);
      } catch (RemoteException e) {
        log.error(e);
      }
    }
    slaveServer.shutdown();
    masterRegistration.shutdown();
  }

  /**
   * @return the {@link URI} of the {@link NodeServer}
   */
  public URI getUri() {
    return slaveServer.getUri();
  }

  public InetSocketAddress getAddress() {
    return slaveServer.getAddress();
  }

  public ServiceIdentifier lookupService(GraphName serviceName, Service<?, ?> serviceType)
      throws RemoteException, XmlRpcTimeoutException {
    Response<URI> response =
        masterClient.lookupService(slaveServer.toSlaveIdentifier(), serviceName.toString());
    if (response.getStatusCode() == StatusCode.SUCCESS) {
      ServiceDefinition serviceDefinition =
          new ServiceDefinition(serviceName, serviceType.getDataType(), serviceType.getMD5Sum());
      return new ServiceIdentifier(response.getResult(), serviceDefinition);
    } else {
      return null;
    }
  }

  /**
   * @return true if Node is fully registered with {@link MasterServer}.
   *         {@code isRegistered()} can become {@code false} if new
   *         {@link Publisher}s or {@link Subscriber}s are created.
   */
  public boolean isRegistered() {
    return masterRegistration.getPendingSize() == 0;
  }

  public boolean isRegistrationOk() {
    return masterRegistration.isMasterRegistrationOk();
  }

}
