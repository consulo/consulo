/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.util.xml.serializer;

import consulo.util.collection.ContainerUtil;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.Comparing;
import org.jdom.Element;

import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"HardCodedStringLiteral"})
public class JDOMExternalizerUtil {
  private static final String VALUE_ATTR_NAME = "value";

  public static void writeField(Element root, String fieldName, String value) {
    Element element = new Element("option");
    element.setAttribute("name", fieldName);
    element.setAttribute("value", value == null ? "" : value);
    root.addContent(element);
  }

  public static String readField(Element parent, String fieldName, String defaultValue) {
    String val = readField(parent, fieldName);
    return val == null ? defaultValue : val;
  }

  @Nullable
  public static String readField(Element parent, String fieldName) {
    for (Element element : JDOMUtil.getChildren(parent, "option")) {
      String childName = element.getAttributeValue("name");
      if (Comparing.strEqual(childName, fieldName)) {
        return element.getAttributeValue("value");
      }
    }
    return null;
  }

  public static Element getOption(Element parent, String fieldName) {
    for (Element element : JDOMUtil.getChildren(parent, "option")) {
      String childName = element.getAttributeValue("name");
      if (Comparing.strEqual(childName, fieldName)) {
        return element;
      }
    }
    return null;
  }

  public static Element writeOption(Element root, String fieldName) {
    Element element = new Element("option");
    element.setAttribute("name", fieldName);
    root.addContent(element);
    return element;
  }

  public static Element addElementWithValueAttribute(Element parent, String childTagName, @Nullable String attrValue) {
    Element element = new Element(childTagName);
    if (attrValue != null) {
      element.setAttribute(VALUE_ATTR_NAME, attrValue);
    }
    parent.addContent(element);
    return element;
  }

  @Nullable
  public static String getFirstChildValueAttribute(Element parent, String childTagName) {
    Element first = parent.getChild(childTagName);
    if (first != null) {
      return first.getAttributeValue(VALUE_ATTR_NAME);
    }
    return null;
  }

  public static List<String> getChildrenValueAttributes(Element parent, String childTagName) {
    List<Element> children = parent.getChildren(childTagName);
    if (children.isEmpty()) {
      return Collections.emptyList();
    }
    if (children.size() == 1) {
      String value = children.iterator().next().getAttributeValue(VALUE_ATTR_NAME);
      return value == null ? Collections.<String>emptyList() : Collections.singletonList(value);
    }
    List<String> values = ContainerUtil.newArrayListWithCapacity(children.size());
    for (Element child : children) {
      String value = child.getAttributeValue(VALUE_ATTR_NAME);
      if (value != null) {
        values.add(value);
      }
    }
    return values;
  }

  @SuppressWarnings("Duplicates")
  public static void addChildrenWithValueAttribute(Element parent, String childTagName, List<String> attrValues) {
    for (String value : attrValues) {
      if (value != null) {
        Element child = new Element(childTagName);
        child.setAttribute(VALUE_ATTR_NAME, value);
        parent.addContent(child);
      }
    }
  }

  @SuppressWarnings({"deprecation", "Duplicates"})
  public static void addChildren(Element parent, String childElementName, Collection<? extends JDOMExternalizable> children)
          throws WriteExternalException {
    for (JDOMExternalizable child : children) {
      if (child != null) {
        Element element = new Element(childElementName);
        child.writeExternal(element);
        parent.addContent(element);
      }
    }
  }
}
