/*
 * Copyright 2013-2018 consulo.io
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
package com.intellij.configurationStore;

import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * from kotlin
 */
public class UnknownElementCollector {
  private final Set<String> myKnownTagNames = new HashSet<>();

  public void addKnownName(String name) {
    myKnownTagNames.add(name);
  }

  @Nonnull
  public UnknownElementWriter createWriter(@Nonnull Element element) {
    Map<String, Element> unknownElements = null;
    Iterator<Element> iterator = element.getChildren().iterator();
    while (iterator.hasNext()) {
      Element child = iterator.next();
      if (!child.getName().equals("option") && !myKnownTagNames.contains(child.getName())) {
        if (unknownElements == null) {
          unknownElements = new HashMap<>();
        }

        unknownElements.put(child.getName(), child);
        iterator.remove();
      }
    }

    return unknownElements == null ? UnknownElementWriter.EMPTY : new UnknownElementWriter(unknownElements);
  }
}
