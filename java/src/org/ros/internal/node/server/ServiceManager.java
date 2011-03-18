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

import com.google.common.collect.Maps;

import org.ros.internal.service.ServiceServer;
import org.ros.message.Message;

import java.util.Map;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class ServiceManager {

  private final Map<String, ServiceServer<? extends Message>> services;
  
  public ServiceManager() {
    services = Maps.newConcurrentMap();
  }
  
  public boolean hasService(String serviceName) {
    return services.containsKey(serviceName);
  }

  public void putService(String serviceName, ServiceServer<? extends Message> serviceServer) {
    services.put(serviceName, serviceServer);
  }

  public ServiceServer<? extends Message> getService(String serviceName) {
    return services.get(serviceName);
  }
  
}
