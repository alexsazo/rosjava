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

package org.ros.internal.node.address;

import com.google.common.base.Preconditions;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;

/**
 * A wrapper for {@link InetSocketAddress} that emphasizes the difference
 * between an address that should be used for binding a server port and one that
 * should be advertised to external entities.
 * 
 * An {@link AdvertiseAddress} enforces lazy lookups of port information to
 * prevent accidentally storing a bind port (e.g. 0 for OS picked) instead of
 * the advertised port.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public class AdvertiseAddress implements Address {

  private final String host;
  
  private Callable<Integer> portCallable;

  public static AdvertiseAddress createPrivate() {
    return new AdvertiseAddress(LOOPBACK);
  }
  
  public static AdvertiseAddress createPublic() throws UnknownHostException {
    return new AdvertiseAddress(InetAddress.getLocalHost().getCanonicalHostName());
  }
  
  public AdvertiseAddress(String host) {
    this.host = host;
  }
  
  public void setPortCallable(Callable<Integer> portCallable) {
    this.portCallable = portCallable;
  }

  public InetSocketAddress toInetSocketAddress() throws Exception {
    Preconditions.checkNotNull(portCallable);
    return InetSocketAddress.createUnresolved(host, portCallable.call());
  }

  public URI toUri(String scheme) throws Exception {
    Preconditions.checkNotNull(portCallable);
    return new URI(scheme, null, host, portCallable.call(), null, null, null);
  }

  @Override
  public String toString() {
    Preconditions.checkNotNull(portCallable);
    try {
      return "AdvertiseAddress<" + host + ", " + portCallable.call() + ">";
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int hashCode() {
    Preconditions.checkNotNull(portCallable);
    final int prime = 31;
    int result = 1;
    result = prime * result + ((host == null) ? 0 : host.hashCode());
    try {
      result = prime * result + portCallable.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    Preconditions.checkNotNull(portCallable);
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    AdvertiseAddress other = (AdvertiseAddress) obj;
    if (host == null) {
      if (other.host != null) return false;
    } else if (!host.equals(other.host)) return false;
    try {
      if (portCallable.call() != other.portCallable.call()) return false;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return true;
  }

}
