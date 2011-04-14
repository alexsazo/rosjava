/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of Willow Garage, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.ros.app_manager;

import org.ros.Node;
import org.ros.internal.node.service.ServiceClient;
import org.ros.internal.node.service.ServiceIdentifier;
import org.ros.namespace.NameResolver;
import org.ros.service.app_manager.ListApps;
import org.ros.service.app_manager.StartApp;
import org.ros.service.app_manager.StopApp;

//TODO(kwc) this class is not meant to be part of rosjava and is only being developed here until rosjava matures
/**
 * Interact with a remote ROS App Manager.
 * 
 * @author kwc@willowgarage.com (Ken Conley)
 */
public class AppManagerCb {

  private final Node node;
  private NameResolver resolver;

  public AppManagerCb(Node node, String robotName) {
    this.node = node;
    resolver = node.getResolver().createResolver(robotName);
  }

  public void listApps(final AppManagerCallback<ListApps.Response> callback) {

    Thread callThread = new Thread(new Runnable() {

      @Override
      public void run() {
// add "try" here.  node.lookupService() throws if ros master is down.
        ServiceIdentifier serviceIdentifier = node.lookupService(resolver.resolveName("list_apps"),
            new ListApps());
        if (serviceIdentifier == null) {
          callback.callFailed(new AppManagerException());
        } else {
          ServiceClient<ListApps.Response> listAppsClient = node
              .createServiceClient(serviceIdentifier, ListApps.Response.class);
          listAppsClient.call(new ListApps.Request(), callback);
        }
      }
    });
    callThread.start();

  }

  public void startApp(final String appName, final AppManagerCallback<StartApp.Response> callback) {
    Thread callThread = new Thread(new Runnable() {

      @Override
      public void run() {
        ServiceIdentifier serviceIdentifier = node.lookupService(resolver.resolveName("start_app"),
            new StartApp());
        if (serviceIdentifier == null) {
          callback.callFailed(new AppManagerException());
        }
        ServiceClient<StartApp.Response> startAppClient = node.createServiceClient(
            serviceIdentifier, StartApp.Response.class);
        StartApp.Request request = new StartApp.Request();
        request.name = appName;
        startAppClient.call(request, callback);
      }
    });
    callThread.start();
  }

  public void stopApp(final String appName, final AppManagerCallback<StopApp.Response> callback) {
    Thread callThread = new Thread(new Runnable() {

      @Override
      public void run() {
        ServiceIdentifier serviceIdentifier = node.lookupService(resolver.resolveName("stop_app"),
            new StopApp());
        if (serviceIdentifier == null) {
          callback.callFailed(new AppManagerException());
        } else {
          ServiceClient<StopApp.Response> stopAppClient = node
              .createServiceClient(serviceIdentifier, StopApp.Response.class);
          StopApp.Request request = new StopApp.Request();
          request.name = appName;
          stopAppClient.call(request, callback);
        }
      }
    });
    callThread.start();
  }
}
