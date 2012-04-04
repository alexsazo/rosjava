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

package org.ros.internal.message;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.ros.internal.message.topic.TopicDefinitionResourceProvider;
import org.ros.message.MessageFactory;

import java.util.ArrayList;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MessageTest {

  private TopicDefinitionResourceProvider topicDefinitionResourceProvider;
  private MessageFactory messageFactory;

  @Before
  public void setUp() {
    topicDefinitionResourceProvider = new TopicDefinitionResourceProvider();
    messageFactory = new DefaultMessageFactory(topicDefinitionResourceProvider);
  }

  @Test
  public void testCreateEmptyMessage() {
    topicDefinitionResourceProvider.add("foo/foo", "");
    messageFactory.newFromType("foo/foo");
  }

  @Test
  public void testCreateEmptyMessageWithBlankLines() {
    topicDefinitionResourceProvider.add("foo/foo", "\n\n\n\n\n");
    messageFactory.newFromType("foo/foo");
  }

  @Test
  public void testString() {
    String data = "Hello, ROS!";
    RuntimeMessage runtimeMessage = messageFactory.newFromType("std_msgs/String");
    runtimeMessage.setString("data", data);
    assertEquals(data, runtimeMessage.getString("data"));
  }

  @Test
  public void testStringWithComments() {
    topicDefinitionResourceProvider.add("foo/foo",
        "# foo\nstring data\n    # string other data");
    String data = "Hello, ROS!";
    RuntimeMessage runtimeMessage = messageFactory.newFromType("foo/foo");
    runtimeMessage.setString("data", data);
    assertEquals(data, runtimeMessage.getString("data"));
  }

  @Test
  public void testInt8() {
    byte data = 42;
    RuntimeMessage runtimeMessage = messageFactory.newFromType("std_msgs/Int8");
    runtimeMessage.setInt8("data", data);
    assertEquals(data, runtimeMessage.getInt8("data"));
  }

  @Test
  public void testNestedMessage() {
    topicDefinitionResourceProvider.add("foo/foo", "bar data");
    topicDefinitionResourceProvider.add("foo/bar", "int8 data");
    RuntimeMessage fooMessage = messageFactory.newFromType("foo/foo");
    RuntimeMessage barMessage = messageFactory.newFromType("foo/bar");
    fooMessage.setMessage("data", barMessage);
    byte data = 42;
    barMessage.setInt8("data", data);
    assertEquals(data, fooMessage.getMessage("data").getInt8("data"));
  }

  @Test
  public void testConstantInt8() {
    topicDefinitionResourceProvider.add("foo/foo", "int8 data=42");
    RuntimeMessage runtimeMessage = messageFactory.newFromType("foo/foo");
    assertEquals(42, runtimeMessage.getInt8("data"));
  }

  @Test
  public void testConstantString() {
    topicDefinitionResourceProvider.add("foo/foo", "string data=Hello, ROS! # comment ");
    RuntimeMessage runtimeMessage = messageFactory.newFromType("foo/foo");
    assertEquals("Hello, ROS! # comment", runtimeMessage.getString("data"));
  }

  public void testInt8List() {
    topicDefinitionResourceProvider.add("foo/foo", "int8[] data");
    RuntimeMessage runtimeMessage = messageFactory.newFromType("foo/foo");
    ArrayList<Byte> data = Lists.newArrayList((byte) 1, (byte) 2, (byte) 3);
    runtimeMessage.setInt8List("data", data);
    assertEquals(data, runtimeMessage.getInt8List("data"));
  }
}