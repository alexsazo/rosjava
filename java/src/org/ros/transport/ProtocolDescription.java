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

package org.ros.transport;

import java.net.InetSocketAddress;
import java.util.List;

import com.google.common.collect.Lists;

public class ProtocolDescription {

  private final String name;
  private final InetSocketAddress address;
  
  public ProtocolDescription(String name, InetSocketAddress address) {
    this.name = name;
    this.address = address;
  }

  public String getName() {
    return name;
  }

  public InetSocketAddress getAddress() {
    return address;
  }

  public List<Object> toList() {
    return Lists.newArrayList((Object) name, address.getHostName(), address.getPort());
  }
  
  @Override
  public String toString() {
    return "Protocol<" + name + ", " + getAddress() + ">";    
  }
}
