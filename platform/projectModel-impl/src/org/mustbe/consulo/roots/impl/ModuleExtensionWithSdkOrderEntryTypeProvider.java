/*
 * Copyright 2013-2014 must-be.org
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
package org.mustbe.consulo.roots.impl;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootLayer;
import com.intellij.openapi.roots.impl.ModuleExtensionWithSdkOrderEntryImpl;
import com.intellij.openapi.roots.impl.ModuleRootLayerImpl;
import com.intellij.openapi.roots.ui.CellAppearanceEx;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.util.SimpleTextCellAppearance;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.ui.SimpleTextAttributes;
import org.consulo.lombok.annotations.LazyInstance;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.roots.OrderEntryTypeProvider;
import org.mustbe.consulo.sdk.SdkUtil;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public class ModuleExtensionWithSdkOrderEntryTypeProvider implements OrderEntryTypeProvider<ModuleExtensionWithSdkOrderEntryImpl> {
  @NotNull
  @LazyInstance
  public static ModuleExtensionWithSdkOrderEntryTypeProvider getInstance() {
    return EP_NAME.findExtension(ModuleExtensionWithSdkOrderEntryTypeProvider.class);
  }

  @NonNls
  public static final String EXTENSION_ID_ATTRIBUTE = "extension-id";

  @NotNull
  @Override
  public String getId() {
    return "module-extension-sdk";
  }

  @NotNull
  @Override
  public ModuleExtensionWithSdkOrderEntryImpl loadOrderEntry(@NotNull Element element, @NotNull ModuleRootLayer moduleRootLayer) throws InvalidDataException {
    String moduleExtensionId = element.getAttributeValue(EXTENSION_ID_ATTRIBUTE);
    if (moduleExtensionId == null) {
      throw new InvalidDataException();
    }
    return new ModuleExtensionWithSdkOrderEntryImpl(moduleExtensionId, (ModuleRootLayerImpl)moduleRootLayer, false);
  }

  @Override
  public void storeOrderEntry(@NotNull Element element, @NotNull ModuleExtensionWithSdkOrderEntryImpl orderEntry) {
    element.setAttribute(EXTENSION_ID_ATTRIBUTE, orderEntry.getModuleExtensionId());
  }

  @Override
  public void navigate(@NotNull final ModuleExtensionWithSdkOrderEntryImpl orderEntry) {
    final Sdk sdk = orderEntry.getSdk();
    if (sdk == null) {
      return;
    }
    Project project = orderEntry.getModuleRootLayer().getProject();
    final ProjectStructureConfigurable config = ProjectStructureConfigurable.getInstance(project);
    ShowSettingsUtil.getInstance().editConfigurable(project, config, new Runnable() {
      @Override
      public void run() {
        config.select(sdk, true);
      }
    });
  }

  @NotNull
  @Override
  public CellAppearanceEx getCellAppearance(@NotNull ModuleExtensionWithSdkOrderEntryImpl orderEntry) {
    Sdk sdk = orderEntry.getSdk();
    return new SimpleTextCellAppearance(orderEntry.getPresentableName(), SdkUtil.getIcon(sdk),
                                        sdk == null ? SimpleTextAttributes.ERROR_ATTRIBUTES : SimpleTextAttributes.SYNTHETIC_ATTRIBUTES);
  }
}
