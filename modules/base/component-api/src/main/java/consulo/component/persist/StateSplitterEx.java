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
package consulo.component.persist;

import consulo.component.util.text.UniqueNameGenerator;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import org.jdom.Attribute;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("deprecation")
public abstract class StateSplitterEx {
  public abstract List<Pair<Element, String>> splitState(@Nonnull Element state);

  public void mergeStateInto(@Nonnull Element target, @Nonnull Element subState) {
    target.addContent(subState);
  }

  @Nonnull
  protected static List<Pair<Element, String>> splitState(@Nonnull Element state, @Nonnull String attributeName) {
    UniqueNameGenerator generator = new UniqueNameGenerator();
    List<Pair<Element, String>> result = new ArrayList<>();
    for (Element subState : state.getChildren()) {
      result.add(createItem(generator, subState, attributeName));
    }
    return result;
  }

  @Nonnull
  protected static Pair<Element, String> createItem(@Nonnull UniqueNameGenerator generator, @Nonnull Element element, @Nonnull String attributeName) {
    return createItem(element.getAttributeValue(attributeName), generator, element);
  }

  @Nonnull
  protected static Pair<Element, String> createItem(@Nonnull String fileName, @Nonnull UniqueNameGenerator generator, @Nonnull Element element) {
    return Pair.create(element, generator.generateUniqueName(FileUtil.sanitizeFileName(fileName)) + ".xml");
  }

  protected static void mergeStateInto(@Nonnull Element target, @Nonnull Element subState, @Nonnull String subStateName) {
    if (subState.getName().equals(subStateName)) {
      target.addContent(subState);
    }
    else {
      for (Iterator<Element> iterator = subState.getChildren().iterator(); iterator.hasNext(); ) {
        Element configuration = iterator.next();
        iterator.remove();
        target.addContent(configuration);
      }
      for (Iterator<Attribute> iterator = subState.getAttributes().iterator(); iterator.hasNext(); ) {
        Attribute attribute = iterator.next();
        iterator.remove();
        target.setAttribute(attribute);
      }
    }
  }
}