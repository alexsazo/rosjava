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

package org.ros.node.client;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.ros.node.Response;
import org.ros.topic.Publisher;
import org.ros.topic.Subscriber;
import org.ros.topic.TopicDescription;

import com.google.common.collect.Lists;

public class Master extends Node<org.ros.node.xmlrpc.Master> {

  public Master(URL url) {
    super(url, org.ros.node.xmlrpc.Master.class);
  }

  public Response<Integer> registerService(String callerId, String service, String serviceApi,
      String callerApi) {
    List<Object> response = node.registerService(callerId, service, serviceApi, callerApi);
    return new Response<Integer>((Integer) response.get(0), (String) response.get(1),
        (Integer) response.get(2));
  }

  public Response<Integer> unregisterService(String callerId, String service, String serviceApi) {
    List<Object> response = node.unregisterService(callerId, service, serviceApi);
    return new Response<Integer>((Integer) response.get(0), (String) response.get(1),
        (Integer) response.get(2));
  }

  /**
   * Subscribe the caller to the specified topic. In addition to receiving a
   * list of current publishers, the subscriber will also receive notifications
   * of new publishers via the publisherUpdate API.
   * 
   * @param callerId ROS caller ID
   * @param subscriber
   * @param url API URI of subscriber to register (used for new publisher
   *        notifications)
   * @return Publishers for topic as a list of XML-RPC API URIs for nodes
   *         currently publishing the specified topic.
   * @throws MalformedURLException
   */
  public Response<List<URL>> registerSubscriber(String callerId, Subscriber<?> subscriber, URL url)
      throws MalformedURLException {
    List<Object> response =
        node.registerSubscriber(callerId, subscriber.getTopicName(),
            subscriber.getTopicMessageType(), url.toString());
    List<Object> values = Arrays.asList((Object[]) response.get(2));
    List<URL> urls = Lists.newArrayList();
    for (Object value : values) {
      urls.add(new URL((String) value));
    }
    return new Response<List<URL>>((Integer) response.get(0), (String) response.get(1), urls);
  }

  public Response<Integer> unregisterSubscriber(String callerId, String topic, String callerApi) {
    List<Object> response = node.unregisterSubscriber(callerId, topic, callerApi);
    return new Response<Integer>((Integer) response.get(0), (String) response.get(1),
        (Integer) response.get(2));
  }

  /**
   * Register the caller as a publisher the topic.
   * 
   * @param callerId ROS caller ID
   * @param publisher the publisher to register
   * @param url API URL of publisher to register
   * @return List of current subscribers of topic in the form of XML-RPC URIs
   * @throws MalformedURLException
   */
  public Response<List<URL>> registerPublisher(String callerId, Publisher publisher, URL url)
      throws MalformedURLException {
    String topicName = publisher.getTopicName();
    String messageType = publisher.getTopicMessageType();
    List<Object> response =
        node.registerPublisher(callerId, topicName, messageType, url.toString());
    List<Object> values = Arrays.asList((Object[]) response.get(2));
    List<URL> urls = Lists.newArrayList();
    for (Object value : values) {
      urls.add(new URL((String) value));
    }
    return new Response<List<URL>>((Integer) response.get(0), (String) response.get(1), urls);
  }

  public Response<Integer> unregisterPublisher(String callerId, String topic, String callerApi) {
    List<Object> response = node.unregisterPublisher(callerId, topic, callerApi);
    return new Response<Integer>((Integer) response.get(0), (String) response.get(1),
        (Integer) response.get(2));
  }

  public Response<URI> lookupNode(String callerId, String nodeName) throws URISyntaxException {
    List<Object> response = node.lookupNode(callerId, nodeName);
    return new Response<URI>((Integer) response.get(0), (String) response.get(1), new URI(
        (String) response.get(2)));
  }

  public Response<List<TopicDescription>> getPublishedTopics(String callerId, String subgraph) {
    throw new UnsupportedOperationException();
  }

  public Response<Object> getSystemState(String callerId) {
    throw new UnsupportedOperationException();
  }

  public Response<URI> getUri(String callerId) throws URISyntaxException {
    List<Object> response = node.getUri(callerId);
    return new Response<URI>((Integer) response.get(0), (String) response.get(1), new URI(
        (String) response.get(2)));
  }

  public Response<URI> lookupService(String callerId, String service) throws URISyntaxException {
    List<Object> response = node.lookupService(callerId, service);
    return new Response<URI>((Integer) response.get(0), (String) response.get(1), new URI(
        (String) response.get(2)));
  }

}
