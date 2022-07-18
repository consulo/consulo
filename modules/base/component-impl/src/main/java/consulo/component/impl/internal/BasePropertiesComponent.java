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
package consulo.component.impl.internal;

import consulo.component.PropertiesComponent;
import consulo.component.persist.PersistentStateComponent;
import consulo.util.lang.StringUtil;
import org.jdom.Element;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public class BasePropertiesComponent implements PropertiesComponent, PersistentStateComponent<Element> {
  private final Map<String, String> myMap = new LinkedHashMap<>();
  private static final String ELEMENT_PROPERTY = "property";
  private static final String ATTRIBUTE_NAME = "name";
  private static final String ATTRIBUTE_VALUE = "value";

  @TestOnly
  @Deprecated
  public static BasePropertiesComponent create() {
    return new BasePropertiesComponent();
  }

  @Override
  public Element getState() {
    Element parentNode = new Element("state");
    for (final String key : myMap.keySet()) {
      String value = myMap.get(key);
      if (value != null) {
        Element element = new Element(ELEMENT_PROPERTY);
        element.setAttribute(ATTRIBUTE_NAME, key);
        element.setAttribute(ATTRIBUTE_VALUE, value);
        parentNode.addContent(element);
      }
    }
    return parentNode;
  }

  @Override
  public void loadState(final Element parentNode) {
    myMap.clear();
    for (Element e : parentNode.getChildren(ELEMENT_PROPERTY)) {
      String name = e.getAttributeValue(ATTRIBUTE_NAME);
      if (name != null) {
        myMap.put(name, e.getAttributeValue(ATTRIBUTE_VALUE));
      }
    }
  }

  @Override
  public String getValue(String name) {
    return myMap.get(name);
  }

  @Override
  public void setValue(@Nonnull String name, @Nullable String value) {
    if (value == null) {
      myMap.remove(name);
    }
    else {
      myMap.put(name, value);
    }
  }

  @Override
  public void setValue(@Nonnull String name, @Nullable String value, @Nullable String defaultValue) {
    if (value == null || value.equals(defaultValue)) {
      myMap.remove(name);
    }
    else {
      myMap.put(name, value);
    }
  }

  @Override
  public void setValue(@Nonnull String name, float value, float defaultValue) {
    if (value == defaultValue) {
      myMap.remove(name);
    }
    else {
      myMap.put(name, String.valueOf(value));
    }
  }

  @Override
  public void setValue(@Nonnull String name, int value, int defaultValue) {
    if (value == defaultValue) {
      myMap.remove(name);
    }
    else {
      myMap.put(name, String.valueOf(value));
    }
  }

  @Override
  public void setValue(@Nonnull String name, boolean value, boolean defaultValue) {
    if (value == defaultValue) {
      myMap.remove(name);
    }
    else {
      setValue(name, String.valueOf(value));
    }
  }

  @Override
  public void unsetValue(String name) {
    myMap.remove(name);
  }

  @Override
  public boolean isValueSet(String name) {
    return myMap.containsKey(name);
  }

  @Nullable
  @Override
  public String[] getValues(String name) {
    final String value = getValue(name);
    return value != null ? value.split("\n") : null;
  }

  @Override
  public void setValues(String name, String[] values) {
    if (values == null) {
      setValue(name, null);
    }
    else {
      setValue(name, StringUtil.join(values, "\n"));
    }
  }
}