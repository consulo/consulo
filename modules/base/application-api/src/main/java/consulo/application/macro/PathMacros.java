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

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;

@ServiceAPI(ComponentScope.APPLICATION)
public interface PathMacros {

  public static PathMacros getInstance() {
    return Application.get().getInstance(PathMacros.class);
  }

  Set<String> getAllMacroNames();

  String getValue(String name);

  void setMacro(String name, String value);

  /**
   * Obsolete macros that are to be removed gently from the project files. They can be read, but not written again. Not persisted.
   *
   * @param name
   * @param value
   */
  void addLegacyMacro(@Nonnull String name, @Nonnull String value);

  void removeMacro(String name);

  Set<String> getUserMacroNames();

  Set<String> getSystemMacroNames();

  Collection<String> getIgnoredMacroNames();

  void setIgnoredMacroNames(@Nonnull final Collection<String> names);

  void addIgnoredMacro(@Nonnull final String name);

  boolean isIgnoredMacroName(@Nonnull final String macro);

  void removeAllMacros();

  Collection<String> getLegacyMacroNames();

  void addMacroReplacements(ReplacePathToMacroMap result);

  void addMacroExpands(ExpandMacroToPathMap result);
}
