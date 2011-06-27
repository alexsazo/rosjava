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

package org.ros;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.ros.internal.node.address.AdvertiseAddress;
import org.ros.internal.node.address.BindAddress;
import org.ros.internal.node.server.MasterServer;
import org.ros.internal.node.service.ServiceClient;
import org.ros.internal.node.service.ServiceException;
import org.ros.internal.node.service.ServiceResponseBuilder;
import org.ros.message.srv.beginner_tutorials.AddTwoInts;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class ServiceIntegrationTest {

  private static final String SERVICE_NAME = "/add_two_ints";
  private static final String SERVICE_TYPE = "beginner_tutorials/AddTwoInts";

  private MasterServer masterServer;
  private NodeConfiguration configuration;

  @Before
  public void setUp() {
    masterServer = new MasterServer(BindAddress.createPublic(0), AdvertiseAddress.createPublic());
    masterServer.start();
    configuration = NodeConfiguration.createDefault();
    configuration.setMasterUri(masterServer.getUri());
  }

  @Test
  public void PesistentServiceConnectionTest() throws Exception {
    Node serverNode = new DefaultNode("/server", configuration);
    serverNode.createServiceServer(SERVICE_NAME, SERVICE_TYPE,
        new ServiceResponseBuilder<AddTwoInts.Request, AddTwoInts.Response>() {
          @Override
          public AddTwoInts.Response build(AddTwoInts.Request request) {
            AddTwoInts.Response response = new AddTwoInts.Response();
            response.sum = request.a + request.b;
            return response;
          }
        });

    Node clientNode = new DefaultNode("/client", configuration);
    ServiceClient<AddTwoInts.Request, AddTwoInts.Response> client =
        clientNode.createServiceClient(SERVICE_NAME, SERVICE_TYPE);

    // TODO(damonkohler): This is a hack that we should remove once it's
    // possible to block on a connection being established.
    Thread.sleep(100);

    AddTwoInts.Request request = new AddTwoInts.Request();
    request.a = 2;
    request.b = 2;
    final CountDownLatch latch = new CountDownLatch(1);
    client.call(request, new ServiceResponseListener<AddTwoInts.Response>() {
      @Override
      public void onSuccess(AddTwoInts.Response message) {
        assertEquals(message.sum, 4);
        latch.countDown();
      }

      @Override
      public void onFailure(Exception e) {
        throw new RuntimeException(e);
      }
    });
    assertTrue(latch.await(1, TimeUnit.SECONDS));
  }

  @Test
  public void RequestFailureTest() throws Exception {
    final String errorMessage = "Error!";
    Node serverNode = new DefaultNode("/server", configuration);
    serverNode.createServiceServer(SERVICE_NAME, SERVICE_TYPE,
        new ServiceResponseBuilder<AddTwoInts.Request, AddTwoInts.Response>() {
          @Override
          public AddTwoInts.Response build(AddTwoInts.Request request) throws ServiceException {
            throw new ServiceException(errorMessage);
          }
        });

    Node clientNode = new DefaultNode("/client", configuration);
    ServiceClient<AddTwoInts.Request, AddTwoInts.Response> client =
        clientNode.createServiceClient(SERVICE_NAME, SERVICE_TYPE);

    // TODO(damonkohler): This is a hack that we should remove once it's
    // possible to block on a connection being established.
    Thread.sleep(100);

    AddTwoInts.Request request = new AddTwoInts.Request();
    final CountDownLatch latch = new CountDownLatch(1);
    client.call(request, new ServiceResponseListener<AddTwoInts.Response>() {
      @Override
      public void onSuccess(AddTwoInts.Response message) {
        fail();
      }

      @Override
      public void onFailure(Exception e) {
        assertEquals(e.getMessage(), errorMessage);
        latch.countDown();
      }
    });
    assertTrue(latch.await(1, TimeUnit.SECONDS));
  }

}
