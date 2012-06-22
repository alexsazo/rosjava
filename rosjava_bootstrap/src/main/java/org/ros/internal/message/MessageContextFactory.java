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

import com.google.common.base.Preconditions;

import org.ros.internal.message.MessageDefinitionParser.MessageDefinitionVisitor;
import org.ros.internal.message.field.Field;
import org.ros.internal.message.field.FieldFactory;
import org.ros.internal.message.field.FieldType;
import org.ros.internal.message.field.MessageFieldType;
import org.ros.internal.message.field.PrimitiveFieldType;
import org.ros.message.MessageDeclaration;
import org.ros.message.MessageFactory;
import org.ros.message.MessageIdentifier;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MessageContextFactory {

  private final MessageFactory messageFactory;

  public MessageContextFactory(MessageFactory messageFactory) {
    Preconditions.checkNotNull(messageFactory);
    this.messageFactory = messageFactory;
  }

  public MessageContext newFromMessageDeclaration(final MessageDeclaration messageDeclaration) {
    final MessageContext context = new MessageContext(messageDeclaration, messageFactory);
    MessageDefinitionVisitor visitor = new MessageDefinitionVisitor() {
      private FieldType getFieldType(String type) {
        Preconditions.checkArgument(!type.equals(messageDeclaration.getType()),
            "Message definitions may not be self-referential: " + messageDeclaration);
        FieldType fieldType;
        if (PrimitiveFieldType.existsFor(type)) {
          fieldType = PrimitiveFieldType.valueOf(type.toUpperCase());
        } else {
          fieldType = new MessageFieldType(MessageIdentifier.of(type), messageFactory);
        }
        return fieldType;
      }

      @Override
      public void variableValue(String type, final String name) {
        final FieldType fieldType = getFieldType(type);
        context.addFieldFactory(name, new FieldFactory() {
          @Override
          public Field create() {
            return fieldType.newVariableValue(name);
          }
        });
      }

      @Override
      public void variableList(String type, final int size, final String name) {
        final FieldType fieldType = getFieldType(type);
        context.addFieldFactory(name, new FieldFactory() {
          @Override
          public Field create() {
            return fieldType.newVariableList(name, size);
          }
        });
      }

      @Override
      public void constantValue(String type, final String name, final String value) {
        final FieldType fieldType = getFieldType(type);
        context.addFieldFactory(name, new FieldFactory() {
          @Override
          public Field create() {
            return fieldType.newConstantValue(name, fieldType.parseFromString(value));
          }
        });
      }
    };
    MessageDefinitionParser messageDefinitionParser = new MessageDefinitionParser(visitor);
    messageDefinitionParser.parse(messageDeclaration.getType(), messageDeclaration.getDefinition());
    return context;
  }

  public MessageContext newFromStrings(String messageType, String messageDefinition) {
    MessageDeclaration messageDeclaration = MessageDeclaration.of(messageType, messageDefinition);
    return newFromMessageDeclaration(messageDeclaration);
  }
}
