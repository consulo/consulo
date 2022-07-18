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

import consulo.application.util.SystemInfo;
import consulo.component.impl.internal.macro.BasePathMacroManager;
import consulo.util.collection.FactoryMap;
import org.jdom.Element;

import java.util.*;

/**
 * @author VISTALL
 * @since 23-Mar-22
 */
public class TrackingPathMacroSubstitutorImpl implements TrackingPathMacroSubstitutor {
  private final Map<String, Set<String>> myMacroToComponentNames = FactoryMap.create(k -> new HashSet<>());

  private final Map<String, Set<String>> myComponentNameToMacros = FactoryMap.create(k -> new HashSet<>());

  private final BasePathMacroManager myPathMacroManager;

  public TrackingPathMacroSubstitutorImpl(BasePathMacroManager pathMacroManager) {
    myPathMacroManager = pathMacroManager;
  }

  @Override
  public void reset() {
    myMacroToComponentNames.clear();
    myComponentNameToMacros.clear();
  }

  @Override
  public String expandPath(final String path) {
    return myPathMacroManager.getExpandMacroMap().substitute(path, SystemInfo.isFileSystemCaseSensitive);
  }

  @Override
  public String collapsePath(final String path) {
    return myPathMacroManager.getReplacePathMap().substitute(path, SystemInfo.isFileSystemCaseSensitive);
  }

  @Override
  public void expandPaths(final Element element) {
    myPathMacroManager.getExpandMacroMap().substitute(element, SystemInfo.isFileSystemCaseSensitive);
  }

  @Override
  public void collapsePaths(final Element element) {
    myPathMacroManager.getReplacePathMap().substitute(element, SystemInfo.isFileSystemCaseSensitive);
  }

  public int hashCode() {
    return myPathMacroManager.getExpandMacroMap().hashCode();
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
    final Set<String> result = new HashSet<String>();
    for (String macro : myMacroToComponentNames.keySet()) {
      if (macros.contains(macro)) {
        result.addAll(myMacroToComponentNames.get(macro));
      }
    }

    return result;
  }

  @Override
  public Set<String> getUnknownMacros(final String componentName) {
    final Set<String> result = new HashSet<String>();
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

