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
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.Pair;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class MainConfigurationStateSplitter extends StateSplitterEx {
  @Override
  public final List<Pair<Element, String>> splitState(@Nonnull Element state) {
    UniqueNameGenerator generator = new UniqueNameGenerator();
    List<Pair<Element, String>> result = new ArrayList<>();
    for (Iterator<Element> iterator = state.getChildren(getSubStateTagName()).iterator(); iterator.hasNext(); ) {
      Element element = iterator.next();
      iterator.remove();
      result.add(createItem(getSubStateFileName(element), generator, element));
    }
    if (!JDOMUtil.isEmpty(state)) {
      result.add(createItem(getComponentStateFileName(), generator, state));
    }
    return result;
  }

  @Override
  public final void mergeStateInto(@Nonnull Element target, @Nonnull Element subState) {
    mergeStateInto(target, subState, getSubStateTagName());
  }

  @Nonnull
  protected abstract String getSubStateFileName(@Nonnull Element element);

  @Nonnull
  protected abstract String getComponentStateFileName();

  @Nonnull
  protected abstract String getSubStateTagName();
}