/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util.xmlb;

import com.intellij.util.xmlb.annotations.AbstractCollection;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.Text;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.*;

abstract class AbstractCollectionBinding extends Binding implements MultiNodeBinding, MainBinding {
  private Map<Class<?>, Binding> itemBindings;

  protected final Class<?> itemType;
  private final AbstractCollection annotation;

  public AbstractCollectionBinding(@Nonnull Class elementType, @Nullable MutableAccessor accessor) {
    super(accessor);

    itemType = elementType;
    annotation = accessor == null ? null : accessor.getAnnotation(AbstractCollection.class);
  }

  @Override
  public boolean isMulti() {
    return true;
  }

  @Override
  public void init(@Nonnull Type originalType) {
    if (annotation == null || annotation.surroundWithTag()) {
      return;
    }

    if (StringUtil.isEmpty(annotation.elementTag()) ||
        (annotation.elementTag().equals(Constants.OPTION) && XmlSerializerImpl.getBinding(itemType) == null)) {
      throw new XmlSerializationException("If surround with tag is turned off, element tag must be specified for: " + myAccessor);
    }
  }

  @Nonnull
  private synchronized Map<Class<?>, Binding> getElementBindings() {
    if (itemBindings == null) {
      Binding binding = XmlSerializerImpl.getBinding(itemType);
      if (annotation == null || annotation.elementTypes().length == 0) {
        itemBindings = binding == null ? Collections.<Class<?>, Binding>emptyMap() : Collections.<Class<?>, Binding>singletonMap(itemType, binding);
      }
      else {
        itemBindings = new HashMap<Class<?>, Binding>();
        if (binding != null) {
          itemBindings.put(itemType, binding);
        }
        for (Class aClass : annotation.elementTypes()) {
          Binding b = XmlSerializerImpl.getBinding(aClass);
          if (b != null) {
            itemBindings.put(aClass, b);
          }
        }
        if (itemBindings.isEmpty()) {
          itemBindings = Collections.emptyMap();
        }
      }
    }
    return itemBindings;
  }

  @Nullable
  private Binding getElementBinding(@Nonnull Element element) {
    for (Binding binding : getElementBindings().values()) {
      if (binding.isBoundTo(element)) {
        return binding;
      }
    }
    return null;
  }

  abstract Object processResult(Collection result, Object target);

  @Nonnull
  abstract Collection<Object> getIterable(@Nonnull Object o);

  @Nullable
  @Override
  public Object serialize(@Nonnull Object o, @Nullable Object context, @Nonnull SerializationFilter filter) {
    Collection<Object> collection = getIterable(o);

    String tagName = getTagName(o);
    if (tagName == null) {
      List<Object> result = new ArrayList<>();
      if (!ContainerUtil.isEmpty(collection)) {
        for (Object item : collection) {
          ContainerUtil.addIfNotNull(result, serializeItem(item, result, filter));
        }
      }
      return result;
    }
    else {
      Element result = new Element(tagName);
      if (!ContainerUtil.isEmpty(collection)) {
        for (Object item : collection) {
          Content child = (Content)serializeItem(item, result, filter);
          if (child != null) {
            result.addContent(child);
          }
        }
      }
      return result;
    }
  }

  @Nullable
  @Override
  public Object deserializeList(Object context, @Nonnull List<Element> elements) {
    Collection result;
    if (getTagName(context) == null) {
      if (context instanceof Collection) {
        result = (Collection)context;
        result.clear();
      }
      else {
        result = new ArrayList();
      }
      for (Element node : elements) {
        //noinspection unchecked
        result.add(deserializeItem(node, context));
      }

      if (result == context) {
        return result;
      }
    }
    else {
      assert elements.size() == 1;
      result = deserializeSingle(context, elements.get(0));
    }
    return processResult(result, context);
  }


  @Nullable
  private Object serializeItem(@Nullable Object value, Object context, @Nonnull SerializationFilter filter) {
    if (value == null) {
      LOG.warn("Collection " + myAccessor + " contains 'null' object");
      return null;
    }

    Binding binding = XmlSerializerImpl.getBinding(value.getClass());
    if (binding == null) {
      Element serializedItem = new Element(annotation == null ? Constants.OPTION : annotation.elementTag());
      String attributeName = annotation == null ? Constants.VALUE : annotation.elementValueAttribute();
      String serialized = XmlSerializerImpl.convertToString(value);
      if (attributeName.isEmpty()) {
        if (!serialized.isEmpty()) {
          serializedItem.addContent(new Text(serialized));
        }
      }
      else {
        serializedItem.setAttribute(attributeName, serialized);
      }
      return serializedItem;
    }
    else {
      return binding.serialize(value, context, filter);
    }
  }

  private Object deserializeItem(@Nonnull Element node, Object context) {
    Binding binding = getElementBinding(node);
    if (binding == null) {
      String attributeName = annotation == null ? Constants.VALUE : annotation.elementValueAttribute();
      String value;
      if (attributeName.isEmpty()) {
        value = XmlSerializerImpl.getTextValue(node, "");
      }
      else {
        value = node.getAttributeValue(attributeName);
      }
      return XmlSerializerImpl.convert(value, itemType);
    }
    else {
      return binding.deserialize(context, node);
    }
  }

  @Override
  public Object deserialize(Object context, @Nonnull Element element) {
    Collection result;
    if (getTagName(context) == null) {
      if (context instanceof Collection) {
        result = (Collection)context;
        result.clear();
      }
      else {
        result = new ArrayList();
      }

      //noinspection unchecked
      result.add(deserializeItem(element, context));

      if (result == context) {
        return result;
      }
    }
    else {
      result = deserializeSingle(context, element);
    }
    return processResult(result, context);
  }

  @Nonnull
  private Collection deserializeSingle(Object context, @Nonnull Element node) {
    Collection result = createCollection(node.getName());
    for (Element child : node.getChildren()) {
      //noinspection unchecked
      result.add(deserializeItem(child, context));
    }
    return result;
  }

  protected Collection createCollection(@Nonnull String tagName) {
    return new ArrayList();
  }

  @Override
  public boolean isBoundTo(@Nonnull Element element) {
    String tagName = getTagName(element);
    if (tagName == null) {
      if (element.getName().equals(annotation == null ? Constants.OPTION : annotation.elementTag())) {
        return true;
      }

      if (getElementBinding(element) != null) {
        return true;
      }
    }

    return element.getName().equals(tagName);
  }

  @Nullable
  private String getTagName(@Nullable Object target) {
    return annotation == null || annotation.surroundWithTag() ? getCollectionTagName(target) : null;
  }

  protected abstract String getCollectionTagName(@Nullable Object target);
}
