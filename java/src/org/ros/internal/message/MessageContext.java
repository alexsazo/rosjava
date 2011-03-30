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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
class MessageContext {

  private final String name;
  private final Map<String, Field> fields;
  private final List<Field> orderedFields;
  private final Map<String, Object> constantFieldValues;

  MessageContext(String name) {
    this.name = name;
    this.fields = Maps.newConcurrentMap();
    this.orderedFields = Lists.newArrayList();
    this.constantFieldValues = Maps.newConcurrentMap();
  }
  
  String getName() {
    return name;
  }

  void addConstantField(String name, FieldType type, Object value) {
    Field field = Field.createConstant(name, type);
    fields.put(name, field);
    orderedFields.add(field);
    constantFieldValues.put(name, value);
  }

  void addConstantArrayField(String name, FieldType type, Object value) {
    Field field = Field.createConstantArray(name, type);
    fields.put(name, field);
    orderedFields.add(field);
    constantFieldValues.put(name, value);
  }

  void addValueField(String name, FieldType type) {
    Field field = Field.createValue(name, type);
    fields.put(name, field);
    orderedFields.add(field);
  }

  void addValueArrayField(String name, FieldType type) {
    Field field = Field.createValueArray(name, type);
    fields.put(name, field);
    orderedFields.add(field);
  }

  boolean hasConstantField(String name, FieldType type) {
    return fields.containsKey(name) && fields.get(name).getType() == type
        && fields.get(name).isConstant();
  }

  Object getConstant(String name) {
    return constantFieldValues.get(name);
  }

  boolean hasValueField(String name, FieldType type) {
    return fields.containsKey(name) && fields.get(name).getType() == type
        && !fields.get(name).isConstant();
  }

  boolean hasMessageValueField(String name) {
    return fields.containsKey(name) && fields.get(name).getType() instanceof MessageFieldType
        && !fields.get(name).isConstant();
  }

  /**
   * @return the {@link List} of {@link Field}s in the order they were added
   */
  List<Field> getFields() {
    return Collections.unmodifiableList(orderedFields);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((constantFieldValues == null) ? 0 : constantFieldValues.hashCode());
    result = prime * result + ((fields == null) ? 0 : fields.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((orderedFields == null) ? 0 : orderedFields.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    MessageContext other = (MessageContext) obj;
    if (constantFieldValues == null) {
      if (other.constantFieldValues != null) return false;
    } else if (!constantFieldValues.equals(other.constantFieldValues)) return false;
    if (fields == null) {
      if (other.fields != null) return false;
    } else if (!fields.equals(other.fields)) return false;
    if (name == null) {
      if (other.name != null) return false;
    } else if (!name.equals(other.name)) return false;
    if (orderedFields == null) {
      if (other.orderedFields != null) return false;
    } else if (!orderedFields.equals(other.orderedFields)) return false;
    return true;
  }

}
