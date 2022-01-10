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

import com.intellij.util.ArrayUtil;
import org.jdom.Element;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * from kotlin
 */
public class UnknownElementWriter {
  public static final UnknownElementWriter EMPTY = new UnknownElementWriter(Collections.emptyMap());

  private final Map<String, Element> myUnknownElements;

  public UnknownElementWriter(Map<String, Element> unknownElements) {
    myUnknownElements = unknownElements;
  }

  public <T> void write(Element outElement, Collection<T> items, Function<T, String> itemToTagName, Consumer<T> writer) {
    Map<String, T> knownNameToWriter = new HashMap<>(items.size());
    for (T item : items) {
      knownNameToWriter.put(itemToTagName.apply(item), item);
    }
    write(outElement, knownNameToWriter, writer);
  }

  public <T> void write(Element outElement, Map<String, T> knownNameToWriter, Consumer<T> writer) {
    Set<String> names;
    if(myUnknownElements.isEmpty()) {
      names = knownNameToWriter.keySet();
    }
    else {
      names = new HashSet<>(myUnknownElements.size());
      names.addAll(knownNameToWriter.keySet());
    }

    String[] sortedNames = ArrayUtil.toStringArray(names);
    Arrays.sort(sortedNames);
    for (String name : sortedNames) {
      T known = knownNameToWriter.get(name);
      if(known == null) {
        outElement.addContent(myUnknownElements.get(name).clone());
      }
      else {
        writer.accept(known);
      }
    }
  }
}
