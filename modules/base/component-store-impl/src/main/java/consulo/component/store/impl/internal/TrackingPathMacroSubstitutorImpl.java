/*
 * Copyright 2013-2022 consulo.io
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
package consulo.component.store.impl.internal;

import consulo.component.impl.internal.macro.BasePathMacroManager;
import consulo.component.macro.PathMacroManager;
import consulo.platform.Platform;
import consulo.util.collection.FactoryMap;
import jakarta.inject.Provider;
import org.jdom.Element;

import java.util.*;

/**
 * @author VISTALL
 * @since 23-Mar-22
 */
public class TrackingPathMacroSubstitutorImpl implements TrackingPathMacroSubstitutor {
  private final Map<String, Set<String>> myMacroToComponentNames = FactoryMap.create(k -> new HashSet<>());

  private final Map<String, Set<String>> myComponentNameToMacros = FactoryMap.create(k -> new HashSet<>());

  private final Provider<? extends PathMacroManager> myPathMacroManager;

  public TrackingPathMacroSubstitutorImpl(Provider<? extends PathMacroManager> pathMacroManager) {
    myPathMacroManager = pathMacroManager;
  }

  @Override
  public void reset() {
    myMacroToComponentNames.clear();
    myComponentNameToMacros.clear();
  }

  @Override
  public String expandPath(final String path) {
    return ((BasePathMacroManager)myPathMacroManager.get()).getExpandMacroMap().substitute(path, Platform.current().fs().isCaseSensitive());
  }

  @Override
  public String collapsePath(final String path) {
    return ((BasePathMacroManager)myPathMacroManager.get()).getReplacePathMap().substitute(path, Platform.current().fs().isCaseSensitive());
  }

  @Override
  public void expandPaths(final Element element) {
    ((BasePathMacroManager)myPathMacroManager.get()).getExpandMacroMap().substitute(element, Platform.current().fs().isCaseSensitive());
  }

  @Override
  public void collapsePaths(final Element element) {
    ((BasePathMacroManager)myPathMacroManager.get()).getReplacePathMap().substitute(element, Platform.current().fs().isCaseSensitive());
  }

  @Override
  public int hashCode() {
    return ((BasePathMacroManager)myPathMacroManager.get()).getExpandMacroMap().hashCode();
  }

  @Override
  public void invalidateUnknownMacros(final Set<String> macros) {
    for (final String macro : macros) {
      final Set<String> components = myMacroToComponentNames.get(macro);
      for (final String component : components) {
        myComponentNameToMacros.remove(component);
      }

      myMacroToComponentNames.remove(macro);
    }
  }

  @Override
  public Set<String> getComponents(final Collection<String> macros) {
    final Set<String> result = new HashSet<>();
    for (String macro : myMacroToComponentNames.keySet()) {
      if (macros.contains(macro)) {
        result.addAll(myMacroToComponentNames.get(macro));
      }
    }

    return result;
  }

  @Override
  public Set<String> getUnknownMacros(final String componentName) {
    final Set<String> result = new HashSet<>();
    result.addAll(componentName == null ? myMacroToComponentNames.keySet() : myComponentNameToMacros.get(componentName));
    return Collections.unmodifiableSet(result);
  }

  @Override
  public void addUnknownMacros(final String componentName, final Collection<String> unknownMacros) {
    if (unknownMacros.isEmpty()) return;

    for (String unknownMacro : unknownMacros) {
      final Set<String> stringList = myMacroToComponentNames.get(unknownMacro);
      stringList.add(componentName);
    }

    myComponentNameToMacros.get(componentName).addAll(unknownMacros);
  }
}

