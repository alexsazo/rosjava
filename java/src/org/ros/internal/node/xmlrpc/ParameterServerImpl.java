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

package org.ros.internal.node.xmlrpc;

import java.util.List;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class ParameterServerImpl implements ParameterServer {

  @Override
  public List<Object> deleteParam(String callerId, String key) {
    return null;
  }

  @Override
  public List<Object> setParam(String callerId, String key, String value) {
    return null;
  }

  @Override
  public List<Object> getParam(String callerId, String key) {
    return null;
  }

  @Override
  public List<Object> searchParam(String callerId, String key) {
    return null;
  }
  
  @Override
  public List<Object> subscribeParam(String callerId, String callerApi, String key) {
    return null;
  }
  
  @Override
  public List<Object> unsubscribeParam(String callerId, String callerApi, String key) {
    return null;
  }
  
  @Override
  public List<Object> hasParam(String callerId, String key) {
    return null;
  }
  
  @Override
  public List<Object> getParamNames(String callerId) {
    return null;
  }
}
