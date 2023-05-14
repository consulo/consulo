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
package consulo.ide.impl.idea.application.options.pathMacros;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationBundle;
import consulo.configurable.*;
import consulo.project.impl.internal.ProjectStorageUtil;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.internal.ProjectEx;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

/**
 * @author dsl
 */
@ExtensionImpl
public class PathMacroConfigurable implements SearchableConfigurable, Configurable.NoScroll, ApplicationConfigurable {
  public static final String ID = "preferences.pathVariables";
  private PathMacroListEditor myEditor;

  @Override
  public JComponent createComponent() {
    myEditor = new PathMacroListEditor();
    return myEditor.getPanel();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    myEditor.commit();

    final Project[] projects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : projects) {
      ProjectStorageUtil.checkUnknownMacros((ProjectEx)project, false);
    }
  }

  @Override
  public void reset() {
    myEditor.reset();
  }

  @Override
  public void disposeUIResources() {
    myEditor = null;
  }

  @Override
  public String getDisplayName() {
    return ApplicationBundle.message("title.path.variables");
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.GENERAL_GROUP;
  }

  @Override
  public boolean isModified() {
    return myEditor != null && myEditor.isModified();
  }

  @Override
  @Nonnull
  public String getId() {
    return ID;
  }
}
