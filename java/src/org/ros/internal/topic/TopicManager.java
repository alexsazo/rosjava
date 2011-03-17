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
package org.ros.internal.topic;

import com.google.common.collect.Maps;

import org.ros.internal.service.ServiceServer;

import java.util.Map;

/**
 * Stores internal Publisher and Subscriber instances.
 * 
 * @author kwc@willowgarage.com (Ken Conley)
 */
public class TopicManager {

  private final Map<String, Subscriber<?>> subscribers;
  private final Map<String, Publisher<?>> publishers;
  private final Map<String, ServiceServer<?>> services;

  public TopicManager() {
    publishers = Maps.newConcurrentMap();
    subscribers = Maps.newConcurrentMap();
    services = Maps.newConcurrentMap();
  }

  public boolean hasSubscriber(String topicName) {
    return subscribers.containsKey(topicName);
  }

  public boolean hasPublisher(String topicName) {
    return publishers.containsKey(topicName);
  }

  public boolean hasService(String serviceName) {
    return services.containsKey(serviceName);
  }

  public Publisher<?> getPublisher(String topicName) {
    return publishers.get(topicName);
  }

  public void setPublisher(String topicName, Publisher<?> publisher) {
    publishers.put(topicName, publisher);
  }

  public Subscriber<?> getSubscriber(String topicName) {
    return subscribers.get(topicName);
  }

  public void setSubscriber(String topicName, Subscriber<?> subscriber) {
    subscribers.put(topicName, subscriber);
  }

  public void setService(String serviceName, ServiceServer<?> serviceServer) {
    services.put(serviceName, serviceServer);
  }

  public ServiceServer<?> getService(String serviceName) {
    return services.get(serviceName);
  }
}
