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
 * Created by IntelliJ IDEA.
 * User: Anna.Kozlova
 * Date: 12-Aug-2006
 * Time: 20:14:02
 */
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.project.ProjectBundle;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CompilerOutputEditor extends ModuleElementsEditor {
  private final BuildElementsEditor myCompilerOutputEditor;

  protected CompilerOutputEditor(final ModuleConfigurationState state) {
    super(state);
    myCompilerOutputEditor = new BuildElementsEditor(state);
  }

  @Nonnull
  @Override
  protected JComponent createComponentImpl() {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(UIUtil.PANEL_SMALL_INSETS));
    panel.add(myCompilerOutputEditor.createComponentImpl(), BorderLayout.NORTH);
    return panel;
  }

  @Override
  public void saveData() {
    myCompilerOutputEditor.saveData();
  }

  @Override
  public String getDisplayName() {
    return ProjectBundle.message("project.roots.path.tab.title");
  }

  @Override
  public boolean isModified() {
    return myCompilerOutputEditor.isModified();
  }

  @Override
  public void moduleStateChanged() {
    super.moduleStateChanged();
    myCompilerOutputEditor.moduleStateChanged();
  }

  @Override
  public void moduleCompileOutputChanged(final String baseUrl, final String moduleName) {
    super.moduleCompileOutputChanged(baseUrl, moduleName);
    myCompilerOutputEditor.moduleCompileOutputChanged(baseUrl, moduleName);
  }

  @Override
  @Nullable
  @NonNls
  public String getHelpTopic() {
    return "projectStructure.modules.paths";
  }
}
