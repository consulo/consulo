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

/*
 * User: anna
 * Date: 04-Dec-2008
 */
package consulo.language.copyright.impl.internal.ui;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.language.copyright.UpdateCopyrightsProvider;
import consulo.language.copyright.config.CopyrightFileConfigManager;
import consulo.language.copyright.ui.TemplateCommentPanel;
import consulo.language.file.FileTypeManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CopyrightFormattingConfigurable extends SearchableConfigurable.Parent.Abstract implements Configurable.NoScroll {
  private final Project myProject;
  private TemplateCommentPanel myPanel;

  CopyrightFormattingConfigurable(Project project) {
    myProject = project;
  }

  @Override
  @Nonnull
  public String getId() {
    return "template.copyright.formatting";
  }

  @Override
  @Nls
  public String getDisplayName() {
    return "Formatting";
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent() {
    getOrCreateMainPanel();
    return myPanel.createComponent();
  }

  private TemplateCommentPanel getOrCreateMainPanel() {
    if (myPanel == null) {
      myPanel = new TemplateCommentPanel(CopyrightFileConfigManager.LANG_TEMPLATE, null, null, myProject);
    }
    return myPanel;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return myPanel.isModified();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    myPanel.apply();
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    myPanel.reset();
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    if (myPanel != null) {
      myPanel.disposeUIResources();
    }
    for (Configurable configurable : getConfigurables()) {
      configurable.disposeUIResources();
    }
    myPanel = null;
  }

  @Override
  public boolean hasOwnContent() {
    return true;
  }

  @Override
  protected Configurable[] buildConfigurables() {
    getOrCreateMainPanel();
    FileType[] registeredFileTypes = FileTypeManager.getInstance().getRegisteredFileTypes();
    List<Configurable> list = new ArrayList<>();
    for (FileType fileType : registeredFileTypes) {
      UpdateCopyrightsProvider updateCopyrightsProvider = UpdateCopyrightsProvider.forFileType(fileType);
      if (updateCopyrightsProvider == null) {
        continue;
      }
      list.add(updateCopyrightsProvider.createConfigurable(myProject, myPanel, fileType));
    }
    Collections.sort(list, (o1, o2) -> o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName()));

    return ContainerUtil.toArray(list, Configurable.ARRAY_FACTORY);
  }
}
