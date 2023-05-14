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
package consulo.ide.impl.roots.orderEntry;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkUtil;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.module.OrderEntryTypeEditor;
import consulo.module.impl.internal.layer.orderEntry.ModuleExtensionWithSdkOrderEntryImpl;
import consulo.module.impl.internal.layer.orderEntry.ModuleExtensionWithSdkOrderEntryType;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;

import jakarta.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 06-Jun-16
 */
@ExtensionImpl
public class ModuleExtensionWithSdkOrderEntryTypeEditor implements OrderEntryTypeEditor<ModuleExtensionWithSdkOrderEntryImpl> {
  @RequiredUIAccess
  @Override
  public void navigate(@Nonnull final ModuleExtensionWithSdkOrderEntryImpl orderEntry) {
    final Sdk sdk = orderEntry.getSdk();
    if (sdk == null) {
      return;
    }
    Project project = orderEntry.getModuleRootLayer().getProject();
    ShowSettingsUtil.getInstance().showProjectStructureDialog(project, config -> config.select(sdk, true));
  }

  @Nonnull
  @Override
  public String getOrderTypeId() {
    return ModuleExtensionWithSdkOrderEntryType.ID;
  }

  @Nonnull
  @Override
  public Consumer<ColoredTextContainer> getRender(@Nonnull ModuleExtensionWithSdkOrderEntryImpl orderEntry) {
    return it -> {
      Sdk sdk = orderEntry.getSdk();

      it.setIcon(SdkUtil.getIcon(sdk));
      it.append(orderEntry.getPresentableName(), sdk == null ? SimpleTextAttributes.ERROR_ATTRIBUTES : SimpleTextAttributes.SYNTHETIC_ATTRIBUTES);
    };
  }
}
