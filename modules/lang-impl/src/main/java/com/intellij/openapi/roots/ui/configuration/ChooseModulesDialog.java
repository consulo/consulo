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
 * Date: 14-Aug-2006
 * Time: 12:13:18
 */
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.ChooseElementsDialog;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ChooseModulesDialog extends ChooseElementsDialog<Module> {

  public ChooseModulesDialog(Component parent, final List<Module> items, final String title) {
    super(parent, items, title, null, true);
  }

  public ChooseModulesDialog(Component parent, List<Module> items, String title, @Nullable String description) {
    super(parent, items, title, description, true);
  }

  public ChooseModulesDialog(final Project project, final List<? extends Module> items, final String title, final String description) {
    super(project, items, title, description, true);
  }

  public void setSingleSelectionMode() {
    myChooser.setSingleSelectionMode();
  }

  @Override
  protected Icon getItemIcon(final Module item) {
    return AllIcons.Nodes.Module;
  }

  @Override
  protected String getItemText(final Module item) {
    return item.getName();
  }
}
