/*
 * Copyright 2013-2016 consulo.io
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
package consulo.roots.orderEntry;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.impl.ModuleExtensionWithSdkOrderEntryImpl;
import com.intellij.openapi.roots.ui.CellAppearanceEx;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.util.SimpleTextCellAppearance;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.ui.SimpleTextAttributes;
import consulo.bundle.SdkUtil;
import consulo.roots.ui.configuration.ProjectStructureDialog;
import consulo.ui.RequiredUIAccess;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 06-Jun-16
 */
public class ModuleExtensionWithSdkOrderEntryTypeEditor implements OrderEntryTypeEditor<ModuleExtensionWithSdkOrderEntryImpl> {
  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<Void> navigateAsync(@Nonnull ModuleExtensionWithSdkOrderEntryImpl orderEntry) {
    final Sdk sdk = orderEntry.getSdk();
    if (sdk == null) {
      return AsyncResult.resolved();
    }
    Project project = orderEntry.getModuleRootLayer().getProject();
    final ProjectStructureConfigurable config = ProjectStructureConfigurable.getInstance(project);
    return ProjectStructureDialog.show(project, configurable -> config.select(sdk, true));
  }

  @Nonnull
  @Override
  public CellAppearanceEx getCellAppearance(@Nonnull ModuleExtensionWithSdkOrderEntryImpl orderEntry) {
    Sdk sdk = orderEntry.getSdk();
    return new SimpleTextCellAppearance(orderEntry.getPresentableName(), SdkUtil.getIcon(sdk), sdk == null ? SimpleTextAttributes.ERROR_ATTRIBUTES : SimpleTextAttributes.SYNTHETIC_ATTRIBUTES);
  }
}
