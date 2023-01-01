/*
 * Copyright 2013-2019 consulo.io
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
package consulo.util.nodep.xml.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2019-07-17
 */
public final class SimpleXmlElement {
  public static final SimpleXmlElement[] EMPTY_ARRAY = new SimpleXmlElement[0];

  private static final String EMPTY_TEXT = "";

  private final String myName;

  private final String myText;
  private final Map<String, String> myAttributes;
  private final List<SimpleXmlElement> myChildren;

  public SimpleXmlElement(String name, String text, List<SimpleXmlElement> children, Map<String, String> attributes) {
    myName = name;
    myText = text == null ? EMPTY_TEXT : text;
    myAttributes = attributes;
    myChildren = Collections.unmodifiableList(children);
  }

  public String getChildText(String tagName) {
    SimpleXmlElement child = getChild(tagName);
    return child == null ? EMPTY_TEXT : child.getText();
  }

  public SimpleXmlElement getChild(String tagName) {
    if (myChildren.isEmpty()) {
      return null;
    }

    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myChildren.size(); i++) {
      SimpleXmlElement element = myChildren.get(i);

      if (tagName.equals(element.getName())) {
        return element;
      }
    }
    return null;
  }

  public List<SimpleXmlElement> getChildren(String tagName) {
    if (myChildren.isEmpty()) {
      return Collections.emptyList();
    }

    List<SimpleXmlElement> list = new ArrayList<SimpleXmlElement>();
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myChildren.size(); i++) {
      SimpleXmlElement element = myChildren.get(i);

      if (tagName.equals(element.getName())) {
        list.add(element);
      }
    }
    return list;
  }

  public String getText() {
    return myText;
  }

  public List<SimpleXmlElement> getChildren() {
    return myChildren;
  }

  public String getAttributeValue(String value) {
    return myAttributes.get(value);
  }

  public String getAttributeValue(String value, String defaultvalue) {
    String attrValue = myAttributes.get(value);
    return attrValue == null ? defaultvalue : attrValue;
  }

  public Map<String, String> getAttributes() {
    return Collections.unmodifiableMap(myAttributes);
  }

  public String getName() {
    return myName;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("SimpleXmlElement{");
    sb.append("myName='").append(myName).append('\'');
    sb.append(", myText='").append(myText).append('\'');
    sb.append(", myAttributes=").append(myAttributes);
    sb.append(", myChildren=").append(myChildren);
    sb.append('}');
    return sb.toString();
  }
}
