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

package consulo.ide.impl.idea.ui.tabs;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.project.Project;
import consulo.disposer.Disposer;
import consulo.language.editor.FileColorManager;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author spleaner
 */
@ExtensionImpl
public class FileColorsConfigurable implements SearchableConfigurable, Configurable.NoScroll, ProjectConfigurable {
  private final Project myProject;
  private FileColorsConfigurablePanel myPanel;

  @Inject
  public FileColorsConfigurable(@Nonnull final Project project) {
    myProject = project;
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.EDITOR_GROUP;
  }

  @Override
  @Nls
  public String getDisplayName() {
    return "File Colors";
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent() {
    if (myPanel == null) {
      myPanel = new FileColorsConfigurablePanel((FileColorManagerImpl) FileColorManager.getInstance(myProject));
    }

    return myPanel;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return myPanel != null && myPanel.isModified();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    if (myPanel != null) myPanel.apply();
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    if (myPanel != null) myPanel.reset();
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    if (myPanel !=  null) {
      Disposer.dispose(myPanel);
      myPanel = null;
    }
  }

  @Nonnull
  @Override
  public String getId() {
    return "fileColors";
  }
}
