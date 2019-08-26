/*
 * Copyright 2013-2019 consulo.io
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
package consulo.moduleImport.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBCardLayout;
import consulo.moduleImport.ModuleImportProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-08-26
 */
public class ModuleImportDialog extends DialogWrapper {
  @Nonnull
  private final List<ModuleImportProvider> myModuleImportProviders;

  private JPanel myContentPanel;

  protected ModuleImportDialog(@Nullable Project project, @Nonnull List<ModuleImportProvider> moduleImportProviders) {
    super(project);
    myModuleImportProviders = moduleImportProviders;
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    myContentPanel = new JPanel(new JBCardLayout());
    return myContentPanel;
  }
}
