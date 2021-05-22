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

import com.intellij.util.xmlb.annotations.*;
import consulo.util.collection.ContainerUtil;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.*;
import consulo.util.lang.reflect.ReflectionUtil;
import org.jdom.Comment;
import org.jdom.Content;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

class BeanBinding extends Binding implements MainBinding {
  private static final Map<Class, List<MutableAccessor>> ourAccessorCache = ContainerUtil.createConcurrentSoftValueMap();

  private final String myTagName;
  @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
  private Binding[] myBindings;

  final Class<?> myBeanClass;

  ThreeState hasEqualMethod = ThreeState.UNSURE;

  public BeanBinding(@Nonnull Class<?> beanClass, @Nullable MutableAccessor accessor) {
    super(accessor);

    assert !beanClass.isArray() : "Bean is an array: " + beanClass;
    assert !beanClass.isPrimitive() : "Bean is primitive type: " + beanClass;
    myBeanClass = beanClass;
    myTagName = getTagName(beanClass);
    assert !StringUtil.isEmptyOrSpaces(myTagName) : "Bean name is empty: " + beanClass;
  }

  @Override
  public synchronized void init(@Nonnull Type originalType) {
    assert myBindings == null;

    List<MutableAccessor> accessors = getAccessors(myBeanClass);
    myBindings = new Binding[accessors.size()];
    for (int i = 0, size = accessors.size(); i < size; i++) {
      myBindings[i] = createBinding(accessors.get(i));
    }
  }

  @Override
  @Nullable
  public Object serialize(@Nonnull Object o, @Nullable Object context, @Nonnull SerializationFilter filter) {
    return serializeInto(o, context == null ? null : new Element(myTagName), filter);
  }

  public Element serialize(@Nonnull Object object, boolean createElementIfEmpty, @Nonnull SerializationFilter filter) {
    return serializeInto(object, createElementIfEmpty ? new Element(myTagName) : null, filter);
  }

  @Nullable
  public Element serializeInto(@Nonnull Object o, @Nullable Element element, @Nonnull SerializationFilter filter) {
    for (Binding binding : myBindings) {
      Accessor accessor = binding.getAccessor();

      if (filter instanceof SkipDefaultsSerializationFilter) {
        if (((SkipDefaultsSerializationFilter)filter).equal(binding, o)) {
          continue;
        }
      }
      else if (!filter.accepts(accessor, o)) {
        continue;
      }

      //todo: optimize. Cache it.
      Property property = accessor.getAnnotation(Property.class);
      if (property != null && property.filter() != SerializationFilter.class && !ReflectionUtil.newInstance(property.filter()).accepts(accessor, o)) {
        continue;
      }

      if (element == null) {
        element = new Element(myTagName);
      }

      Object node = binding.serialize(o, element, filter);
      if (node != null) {
        if (node instanceof org.jdom.Attribute) {
          element.setAttribute((org.jdom.Attribute)node);
        }
        else {
          JDOMUtil.addContent(element, node);
        }
      }
    }
    return element;
  }

  @Override
  public Object deserialize(Object context, @Nonnull Element element) {
    Object instance = ReflectionUtil.newInstance(myBeanClass);
    deserializeInto(instance, element, null);
    return instance;
  }

  boolean equalByFields(@Nonnull Object currentValue, @Nonnull Object defaultValue, @Nonnull SkipDefaultsSerializationFilter filter) {
    for (Binding binding : myBindings) {
      Accessor accessor = binding.getAccessor();
      if (!filter.equal(binding, accessor.read(currentValue), accessor.read(defaultValue))) {
        return false;
      }
    }
    return true;
  }

  @Nonnull
  public Map<String, Float> computeBindingWeights(@Nonnull LinkedHashSet<String> accessorNameTracker) {
    Map<String, Float> weights = new HashMap<>(accessorNameTracker.size());
    float weight = 0;
    float step = (float)myBindings.length / (float)accessorNameTracker.size();
    for (String name : accessorNameTracker) {
      weights.put(name, weight);
      weight += step;
    }

    weight = 0;
    for (Binding binding : myBindings) {
      String name = binding.getAccessor().getName();
      if (!weights.containsKey(name)) {
        weights.put(name, weight);
      }

      weight++;
    }
    return weights;
  }

  public void sortBindings(@Nonnull final Map<String, Float> weights) {
    Arrays.sort(myBindings, (o1, o2) -> {
      String n1 = o1.getAccessor().getName();
      String n2 = o2.getAccessor().getName();
      Float w1 = ObjectUtil.notNull(weights.get(n1), 0f);
      Float w2 = ObjectUtil.notNull(weights.get(n2), 0f);
      return (int)(w1 - w2);
    });
  }

  public void deserializeInto(@Nonnull Object result, @Nonnull Element element, @Nullable Set<String> accessorNameTracker) {
    nextAttribute:
    for (org.jdom.Attribute attribute : element.getAttributes()) {
      if (StringUtil.isEmpty(attribute.getNamespaceURI())) {
        for (Binding binding : myBindings) {
          if (binding instanceof AttributeBinding && ((AttributeBinding)binding).myName.equals(attribute.getName())) {
            if (accessorNameTracker != null) {
              accessorNameTracker.add(binding.getAccessor().getName());
            }
            ((AttributeBinding)binding).set(result, attribute.getValue());
            continue nextAttribute;
          }
        }
      }
    }

    Map<Binding, List<Element>> data = null;
    nextNode:
    for (Content content : element.getContent()) {
      if (content instanceof Comment) {
        continue;
      }

      for (Binding binding : myBindings) {
        if (content instanceof org.jdom.Text) {
          if (binding instanceof TextBinding) {
            ((TextBinding)binding).set(result, content.getValue());
          }
          continue;
        }

        Element child = (Element)content;
        if (binding.isBoundTo(child)) {
          if (binding instanceof MultiNodeBinding && ((MultiNodeBinding)binding).isMulti()) {
            if (data == null) {
              data = new LinkedHashMap<>();
            }

            data.computeIfAbsent(binding, it -> new ArrayList<>()).add(child);
          }
          else {
            if (accessorNameTracker != null) {
              accessorNameTracker.add(binding.getAccessor().getName());
            }
            binding.deserialize(result, child);
          }
          continue nextNode;
        }
      }
    }

    if (data != null) {
      for (Map.Entry<Binding, List<Element>> entry : data.entrySet()) {
        Binding binding = entry.getKey();
        List<Element> elements = entry.getValue();

        if (accessorNameTracker != null) {
          accessorNameTracker.add(binding.getAccessor().getName());
        }
        ((MultiNodeBinding)binding).deserializeList(result, elements);
      }
    }
  }

  @Override
  public boolean isBoundTo(@Nonnull Element element) {
    return element.getName().equals(myTagName);
  }

  private static String getTagName(Class<?> aClass) {
    for (Class<?> c = aClass; c != null; c = c.getSuperclass()) {
      String name = getTagNameFromAnnotation(c);
      if (name != null) {
        return name;
      }
    }
    String name = aClass.getSimpleName();
    return name.isEmpty() ? aClass.getSuperclass().getSimpleName() : name;
  }

  private static String getTagNameFromAnnotation(Class<?> aClass) {
    Tag tag = aClass.getAnnotation(Tag.class);
    return tag != null && !tag.value().isEmpty() ? tag.value() : null;
  }

  @Nonnull
  static List<MutableAccessor> getAccessors(@Nonnull Class<?> aClass) {
    List<MutableAccessor> accessors = ourAccessorCache.get(aClass);
    if (accessors != null) {
      return accessors;
    }

    accessors = ContainerUtil.newArrayList();

    if (!AWTHacks.isRectangle(aClass)) {   // special case for Rectangle.class to avoid infinite recursion during serialization due to bounds() method
      collectPropertyAccessors(aClass, accessors);
    }
    collectFieldAccessors(aClass, accessors);

    ourAccessorCache.put(aClass, accessors);

    return accessors;
  }

  private static void collectPropertyAccessors(@Nonnull Class<?> aClass, @Nonnull List<MutableAccessor> accessors) {
    final Map<String, Couple<Method>> candidates = new TreeMap<>(); // (name,(getter,setter))
    for (Method method : aClass.getMethods()) {
      if (!Modifier.isPublic(method.getModifiers())) {
        continue;
      }

      Pair<String, Boolean> propertyData = getPropertyData(method.getName()); // (name,isSetter)
      if (propertyData == null || propertyData.first.equals("class") || method.getParameterTypes().length != (propertyData.second ? 1 : 0)) {
        continue;
      }

      Couple<Method> candidate = candidates.get(propertyData.first);
      if (candidate == null) {
        candidate = Couple.of();
      }
      if ((propertyData.second ? candidate.second : candidate.first) != null) {
        continue;
      }
      candidate = Couple.of(propertyData.second ? candidate.first : method, propertyData.second ? method : candidate.second);
      candidates.put(propertyData.first, candidate);
    }
    for (Map.Entry<String, Couple<Method>> candidate : candidates.entrySet()) {
      Couple<Method> methods = candidate.getValue(); // (getter,setter)
      if (methods.first != null &&
          methods.second != null &&
          methods.first.getReturnType().equals(methods.second.getParameterTypes()[0]) &&
          methods.first.getAnnotation(Transient.class) == null &&
          methods.second.getAnnotation(Transient.class) == null) {
        accessors.add(new PropertyAccessor(candidate.getKey(), methods.first.getReturnType(), methods.first, methods.second));
      }
    }
  }

  private static void collectFieldAccessors(@Nonnull Class<?> aClass, @Nonnull List<MutableAccessor> accessors) {
    Class<?> currentClass = aClass;
    do {
      for (Field field : currentClass.getDeclaredFields()) {
        int modifiers = field.getModifiers();
        //noinspection deprecation
        if (!Modifier.isStatic(modifiers) &&
            (field.getAnnotation(OptionTag.class) != null ||
             field.getAnnotation(Tag.class) != null ||
             field.getAnnotation(Attribute.class) != null ||
             field.getAnnotation(Property.class) != null ||
             field.getAnnotation(Text.class) != null ||
             field.getAnnotation(CollectionBean.class) != null ||
             (Modifier.isPublic(modifiers) &&
              // we don't want to allow final fields of all types, but only supported
              (!Modifier.isFinal(modifiers) || Collection.class.isAssignableFrom(field.getType())) && !Modifier.isTransient(modifiers) && field.getAnnotation(Transient.class) == null))) {
          accessors.add(new FieldAccessor(field));
        }
      }
    }
    while ((currentClass = currentClass.getSuperclass()) != null && currentClass.getAnnotation(Transient.class) == null);
  }

  @Nullable
  private static Pair<String, Boolean> getPropertyData(@Nonnull String methodName) {
    String part = "";
    boolean isSetter = false;
    if (methodName.startsWith("get")) {
      part = methodName.substring(3, methodName.length());
    }
    else if (methodName.startsWith("is")) {
      part = methodName.substring(2, methodName.length());
    }
    else if (methodName.startsWith("set")) {
      part = methodName.substring(3, methodName.length());
      isSetter = true;
    }
    return part.isEmpty() ? null : Pair.create(StringUtil.decapitalize(part), isSetter);
  }

  public String toString() {
    return "BeanBinding[" + myBeanClass.getName() + ", tagName=" + myTagName + "]";
  }

  @Nonnull
  private static Binding createBinding(@Nonnull MutableAccessor accessor) {
    Binding binding = XmlSerializerImpl.getBinding(accessor);
    if (binding instanceof JDOMElementBinding) {
      return binding;
    }

    Attribute attribute = accessor.getAnnotation(Attribute.class);
    if (attribute != null) {
      return new AttributeBinding(accessor, attribute);
    }

    Tag tag = accessor.getAnnotation(Tag.class);
    if (tag != null) {
      return new TagBinding(accessor, tag);
    }

    Text text = accessor.getAnnotation(Text.class);
    if (text != null) {
      return new TextBinding(accessor);
    }

    if (binding instanceof CompactCollectionBinding) {
      return new AccessorBindingWrapper(accessor, binding);
    }

    boolean surroundWithTag = true;
    Property property = accessor.getAnnotation(Property.class);
    if (property != null) {
      surroundWithTag = property.surroundWithTag();
    }

    if (!surroundWithTag) {
      if (binding == null || binding instanceof TextBinding) {
        throw new XmlSerializationException("Text-serializable properties can't be serialized without surrounding tags: " + accessor);
      }
      return new AccessorBindingWrapper(accessor, binding);
    }

    return new OptionTagBinding(accessor, accessor.getAnnotation(OptionTag.class));
  }
}
