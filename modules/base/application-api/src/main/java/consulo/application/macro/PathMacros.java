/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.application.macro;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.component.macro.ExpandMacroToPathMap;
import consulo.component.macro.ReplacePathToMacroMap;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

@ServiceAPI(ComponentScope.APPLICATION)
public interface PathMacros {

  public static PathMacros getInstance() {
    return Application.get().getInstance(PathMacros.class);
  }

  Set<String> getAllMacroNames();

  @Nullable String getValue(String name);

  void setMacro(String name, String value);

  void removeMacro(String name);

  Set<String> getUserMacroNames();

  Set<String> getSystemMacroNames();

  Collection<String> getIgnoredMacroNames();

  void setIgnoredMacroNames(Collection<String> names);

  void addIgnoredMacro(String name);

  boolean isIgnoredMacroName(String macro);

  void removeAllMacros();

  void addMacroReplacements(ReplacePathToMacroMap result);

  void addMacroExpands(ExpandMacroToPathMap result);
}
