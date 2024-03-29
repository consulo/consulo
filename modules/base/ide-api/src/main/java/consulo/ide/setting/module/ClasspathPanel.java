/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.setting.module;

import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.List;

/**
 * @author nik
 */
public interface ClasspathPanel {
  void addItems(List<ClasspathTableItem<?>> toAdd);

  ModifiableRootModel getRootModel();

  Project getProject();

  JComponent getComponent();

  ModuleConfigurationState getModuleConfigurationState();

  void navigate(AnActionEvent fromInputEvent, boolean openLibraryEditor);

  @Nullable
  OrderEntry getSelectedEntry();

  @Nonnull
  LibraryTableModifiableModelProvider getModifiableModelProvider(@Nonnull String tableLevel);
}
