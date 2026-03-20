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
package consulo.util.xml.serializer.internal;

import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.Constants;
import consulo.util.xml.serializer.SerializationFilter;
import consulo.util.xml.serializer.annotation.OptionTag;
import org.jdom.Attribute;
import org.jdom.Element;

import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.Objects;

class OptionTagBinding extends BasePrimitiveBinding {
  private final String myTagName;
  private final String myNameAttribute;
  private final String myValueAttribute;

  public OptionTagBinding(MutableAccessor accessor, @Nullable OptionTag optionTag) {
    super(accessor, optionTag == null ? null : optionTag.value(), optionTag == null ? null : optionTag.converter());

    if (optionTag == null) {
      myTagName = Constants.OPTION;
      myNameAttribute = Constants.NAME;
      myValueAttribute = Constants.VALUE;
    }
    else {
      myNameAttribute = optionTag.nameAttribute();
      myValueAttribute = optionTag.valueAttribute();

      String tagName = optionTag.tag();
      if (StringUtil.isEmpty(myNameAttribute) && Constants.OPTION.equals(tagName)) {
        tagName = accessor.getName();
      }
      myTagName = tagName;
    }
  }

  @Override
  public @Nullable Object serialize(Object o, @Nullable Object context, SerializationFilter filter) {
    Object value = myAccessor.read(o);
    Element targetElement = new Element(myTagName);

    if (!StringUtil.isEmpty(myNameAttribute)) {
      targetElement.setAttribute(myNameAttribute, myName);
    }

    if (value == null) {
      return targetElement;
    }

    if (myConverter == null) {
      if (myBinding == null) {
        targetElement.setAttribute(myValueAttribute, XmlSerializerImpl.convertToString(value));
      }
      else if (myBinding instanceof BeanBinding beanBinding && myValueAttribute.isEmpty()) {
        beanBinding.serializeInto(value, targetElement, filter);
      }
      else {
        Object node = myBinding.serialize(value, targetElement, filter);
        if (node != null && targetElement != node) {
          JDOMUtil.addContent(targetElement, node);
        }
      }
    }
    else {
      targetElement.setAttribute(myValueAttribute, myConverter.toString(value));
    }
    return targetElement;
  }

  @Override
  public Object deserialize(@Nullable Object context, Element element) {
    Objects.requireNonNull(context);
    Attribute valueAttribute = element.getAttribute(myValueAttribute);
    if (valueAttribute == null) {
      if (myValueAttribute.isEmpty()) {
        myAccessor.set(context, Objects.requireNonNull(myBinding).deserialize(context, element));
      }
      else {
        List<Element> children = element.getChildren();
        if (children.isEmpty()) {
          myAccessor.set(context, null);
        }
        else {
          myAccessor.set(context, Binding.deserializeList(Objects.requireNonNull(myBinding), myAccessor.read(context), children));
        }
      }
    }
    else if (myConverter == null) {
      XmlSerializerImpl.doSet(context, valueAttribute.getValue(), myAccessor, XmlSerializerImpl.typeToClass(myAccessor.getGenericType()));
    }
    else {
      myAccessor.set(context, myConverter.fromString(valueAttribute.getValue()));
    }
    return context;
  }

  @Override
  public boolean isBoundTo(Element element) {
    if (!element.getName().equals(myTagName)) {
      return false;
    }

    String name = element.getAttributeValue(myNameAttribute);
    if (StringUtil.isEmpty(myNameAttribute)) {
      return name == null || name.equals(myName);
    }
    else {
      return name != null && name.equals(myName);
    }
  }

  @Override
  public String toString() {
    return "OptionTagBinding[" + myName + ", binding=" + myBinding + "]";
  }
}
