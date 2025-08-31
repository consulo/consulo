/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.util.collection.ArrayUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JDOMExternalizer {
  private JDOMExternalizer() {
  }

  public static void write(Element root, @NonNls String name, String value) {
    @NonNls Element element = new Element("setting");
    element.setAttribute("name", name);
    element.setAttribute("value", value == null ? "" : value);
    root.addContent(element);
  }

  public static void write(Element root, @NonNls String name, boolean value) {
    write(root, name, Boolean.toString(value));
  }
  public static void write(Element root, String name, int value) {
    write(root, name, Integer.toString(value));
  }

  public static boolean readBoolean(Element root, @NonNls String name) {
    return Boolean.valueOf(readString(root, name)).booleanValue();
  }

  public static int readInteger(Element root, String name, int defaultValue) {
    return StringUtil.parseInt(readString(root, name), defaultValue);
  }

  @Nullable
  public static String readString(@NonNls Element root, @NonNls String name) {
    for (Element element : root.getChildren("setting")) {
      if (Comparing.strEqual(element.getAttributeValue("name"), name)) {
        return element.getAttributeValue("value");
      }
    }
    return null;
  }

  public static void writeMap(Element root, Map<String, String> map, @NonNls @Nullable String rootName, @NonNls String entryName) {
    Element mapRoot;
    if (StringUtil.isNotEmpty(rootName)) {
      mapRoot = new Element(rootName);
      root.addContent(mapRoot);
    }
    else {
      mapRoot = root;
    }
    String[] names = ArrayUtil.toStringArray(map.keySet());
    Arrays.sort(names);
    for (String name : names) {
      @NonNls Element element = new Element(entryName);
      element.setAttribute("name", name);
      String value = map.get(name);
      if (value != null) {
        element.setAttribute("value", value);
      }
      mapRoot.addContent(element);
    }
  }

  public static void readMap(Element root, Map<String, String> map, @NonNls @Nullable String rootName, @NonNls String entryName) {
    Element mapRoot;
    if (StringUtil.isNotEmpty(rootName)) {
      mapRoot = root.getChild(rootName);
    }
    else {
      mapRoot = root;
    }
    if (mapRoot == null) {
      return;
    }

    for (@NonNls Element element : mapRoot.getChildren(entryName)) {
      String name = element.getAttributeValue("name");
      if (name != null) {
        map.put(name, element.getAttributeValue("value"));
      }
    }
  }

  /**
   * Saves a pack of strings to some attribute. I.e: [tag attr="value"]
   * @param parent parent element (where to add newly created tags)
   * @param nodeName node name (tag, in our example)
   * @param attrName attribute name (attr, in our example)
   * @param values a pack of values to add
   * @see #loadStringsList(org.jdom.Element, String, String)
   */
  public static void saveStringsList(@Nonnull Element parent,
                                     @Nonnull String nodeName,
                                     @Nonnull String attrName,
                                     @Nonnull String... values) {
    for (String value : values) {
      Element node = new Element(nodeName);
      node.setAttribute(attrName, value);
      parent.addContent(node);
    }
  }

  @Nonnull
  public static List<String> loadStringsList(Element element, String rootName, String attrName) {
    List<String> paths = new LinkedList<String>();
    if (element != null) {
      @Nonnull List list = element.getChildren(rootName);
      for (Object o : list) {
        paths.add(((Element)o).getAttribute(attrName).getValue());
      }
    }
    return paths;
  }
}