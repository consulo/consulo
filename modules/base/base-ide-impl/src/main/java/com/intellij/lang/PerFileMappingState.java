/*
 * Copyright 2013-2020 consulo.io
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
package com.intellij.lang;

import consulo.util.lang.StringUtil;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * from kotlin
 */
final class PerFileMappingState {
  @Nonnull
  public static List<PerFileMappingState> read(Element element, String valueAttributeName) {
    List<Element> entries = element.getChildren("file");
    if (entries.isEmpty()) {
      return List.of();
    }

    List<PerFileMappingState> result = new ArrayList<>();
    for (Element child : entries) {
      String url = child.getAttributeValue("url");
      String value = child.getAttributeValue(valueAttributeName);
      if (StringUtil.isEmpty(url) || value == null) {
        continue;
      }

      result.add(new PerFileMappingState(url, value));
    }
    return result;
  }

  public static Element write(List<PerFileMappingState> list, String valueAttributeName) {
    Element element = new Element("state");
    for (PerFileMappingState entry : list) {
      String value = entry.value;
      if (value == null) {
        continue;
      }

      Element entryElement = new Element("file");
      entryElement.setAttribute("url", entry.url);
      entryElement.setAttribute(valueAttributeName, value);
      element.addContent(entryElement);
    }
    return element;
  }

  @Nonnull
  private final String url;
  @Nullable
  private final String value;

  public PerFileMappingState(@Nonnull String url) {
    this.url = url;
    this.value = null;
  }

  public PerFileMappingState(@Nonnull String url, @Nullable String value) {
    this.url = url;
    this.value = value;
  }

  @Nonnull
  public String getUrl() {
    return url;
  }

  @Nullable
  public String getValue() {
    return value;
  }
}
