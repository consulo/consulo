/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.profile.codeInspection.ui;

import consulo.configurable.Configurable;
import consulo.language.Language;
import consulo.language.editor.inspection.scheme.Profile;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Marker interface for the configurable which is used to configure the current inspection profile. 
 *
 * @author yole
 */
public interface ErrorsConfigurable extends Configurable {
  void selectProfile(final Profile profile);

  void selectInspectionTool(final String selectedToolShortName);

  void setFilterLanguages(@Nonnull Collection<Language> languages);

  @Nullable
  Object getSelectedObject();

  @Deprecated
  class SERVICE {
    private SERVICE() {
    }

    @Nullable
    @Deprecated
    public static ErrorsConfigurable createConfigurable(@Nonnull Project project) {
      throw new UnsupportedOperationException("ShowSettingsUtil.getInstance().showAndSelect(project, ErrorsConfigurable.class) use");
    }
  }
}
