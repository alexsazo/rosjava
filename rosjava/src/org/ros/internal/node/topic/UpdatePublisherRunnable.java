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

package org.ros.internal.node.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.exception.RemoteException;
import org.ros.internal.node.client.SlaveClient;
import org.ros.internal.node.response.Response;
import org.ros.internal.node.server.SlaveIdentifier;
import org.ros.internal.node.xmlrpc.XmlRpcTimeoutException;
import org.ros.internal.transport.ProtocolDescription;
import org.ros.internal.transport.ProtocolNames;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
class UpdatePublisherRunnable<MessageType> implements Runnable {

  private static final Log log = LogFactory.getLog(UpdatePublisherRunnable.class);

  private final Subscriber<MessageType> subscriber;
  private final PublisherDefinition publisherDefinition;
  private final SlaveIdentifier slaveIdentifier;

  /**
   * @param subscriber
   * @param slaveIdentifier
   *          Identifier of the subscriber's slave.
   * @param publisherDefinition
   */
  public UpdatePublisherRunnable(Subscriber<MessageType> subscriber,
      SlaveIdentifier slaveIdentifier, PublisherDefinition publisherDefinition) {
    this.subscriber = subscriber;
    this.slaveIdentifier = slaveIdentifier;
    this.publisherDefinition = publisherDefinition;
  }

  @Override
  public void run() {
    SlaveClient slaveClient;
    try {
      slaveClient = new SlaveClient(slaveIdentifier.getName(), publisherDefinition.getUri());
      Response<ProtocolDescription> response = slaveClient.requestTopic(this.subscriber
          .getTopicName().toString(), ProtocolNames.SUPPORTED);
      // TODO(kwc): all of this logic really belongs in a protocol handler
      // registry.
      ProtocolDescription selected = response.getResult();
      if (ProtocolNames.SUPPORTED.contains(selected.getName())) {
        subscriber.addPublisher(publisherDefinition, selected.getAddress());
      } else {
        log.error("Publisher returned unsupported protocol selection: " + response);
      }
    } catch (RemoteException e) {
      // TODO(damonkohler): Retry logic is needed at the XML-RPC layer.
      log.error(e);
    } catch (XmlRpcTimeoutException e) {
      // TODO see above note re: retry
      log.error(e);
    } catch (RuntimeException e) {
      // TODO(kwc):
      // org.apache.xmlrpc.XmlRpcException/java.net.ConnectException's are
      // leaking through as java.lang.reflect.UndeclaredThrowableExceptions.
      // This is happening whenever the node attempts to connect to a stale
      // publisher (i.e. a publisher that is no longer online).
      log.error(e);
    }
  }
}
