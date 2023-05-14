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
package consulo.language.codeStyle.ui.setting;

import consulo.language.codeStyle.CodeStyleScheme;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 21-Jul-22
 */
public interface CodeStyleSchemesModel {
  void fireCurrentSettingsChanged();

  boolean isProjectScheme(CodeStyleScheme styleScheme);

  boolean isUsePerProjectSettings();

  default void setUsePerProjectSettings(final boolean usePerProjectSettings) {
    setUsePerProjectSettings(usePerProjectSettings, false);
  }

  void setUsePerProjectSettings(final boolean usePerProjectSettings, final boolean commit);

  void selectScheme(final CodeStyleScheme selected, @Nullable Object source);

  void addScheme(final CodeStyleScheme newScheme, boolean changeSelection);

  @Nonnull
  List<CodeStyleScheme> getAllSortedSchemes();

  CodeStyleScheme getProjectScheme();

  @Nullable
  CodeStyleScheme getSelectedGlobalScheme();

  void copyToProject(final CodeStyleScheme selectedScheme);

  void fireSchemeChanged(CodeStyleScheme scheme);

  @Nonnull
  Project getProject();

  @Nonnull
  CodeStyleScheme getSelectedScheme();

  void removeScheme(final CodeStyleScheme scheme);

  void addListener(CodeStyleSchemesModelListener listener);

  CodeStyleScheme createNewScheme(final String preferredName, final CodeStyleScheme parentScheme);

  CodeStyleScheme exportProjectScheme(@Nonnull String name);

  List<CodeStyleScheme> getSchemes();
}
