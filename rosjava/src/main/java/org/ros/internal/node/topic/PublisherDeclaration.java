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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.ros.internal.node.server.NodeIdentifier;
import org.ros.namespace.GraphName;

import java.net.URI;
import java.util.Map;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class PublisherDeclaration {

  private final PublisherIdentifier publisherIdentifier;
  private final TopicDeclaration topicDeclaration;

  public static PublisherDeclaration newFromSlaveIdentifier(NodeIdentifier nodeIdentifier,
      TopicDeclaration topicDeclaration) {
    Preconditions.checkNotNull(nodeIdentifier);
    Preconditions.checkNotNull(topicDeclaration);
    return new PublisherDeclaration(new PublisherIdentifier(nodeIdentifier,
        topicDeclaration.toIdentifier()), topicDeclaration);
  }

  public PublisherDeclaration(PublisherIdentifier publisherIdentifier,
      TopicDeclaration topicDeclaration) {
    Preconditions.checkNotNull(publisherIdentifier);
    Preconditions.checkNotNull(topicDeclaration);
    Preconditions.checkArgument(publisherIdentifier.getTopicIdentifier().equals(
        topicDeclaration.toIdentifier()));
    this.publisherIdentifier = publisherIdentifier;
    this.topicDeclaration = topicDeclaration;
  }
  
  public Map<String, String> toHeader() {
    // NOTE(damonkohler): ImmutableMap.Builder does not allow duplicate fields
    // while building.
    Map<String, String> header = Maps.newHashMap();
    header.putAll(publisherIdentifier.toHeader());
    header.putAll(topicDeclaration.toHeader());
    return ImmutableMap.copyOf(header);
  }

  public NodeIdentifier getSlaveIdentifier() {
    return publisherIdentifier.getNodeSlaveIdentifier();
  }

  public GraphName getSlaveName() {
    return publisherIdentifier.getNodeSlaveIdentifier().getNodeName();
  }

  public URI getSlaveUri() {
    return publisherIdentifier.getNodeUri();
  }

  public GraphName getTopicName() {
    return topicDeclaration.getName();
  }

  public String getTopicMessageType() {
    return topicDeclaration.getMessageType();
  }

  @Override
  public String toString() {
    return "PublisherDefinition<" + publisherIdentifier + ", " + topicDeclaration + ">";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((publisherIdentifier == null) ? 0 : publisherIdentifier.hashCode());
    result = prime * result + ((topicDeclaration == null) ? 0 : topicDeclaration.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    PublisherDeclaration other = (PublisherDeclaration) obj;
    if (publisherIdentifier == null) {
      if (other.publisherIdentifier != null)
        return false;
    } else if (!publisherIdentifier.equals(other.publisherIdentifier))
      return false;
    if (topicDeclaration == null) {
      if (other.topicDeclaration != null)
        return false;
    } else if (!topicDeclaration.equals(other.topicDeclaration))
      return false;
    return true;
  }
}
