/*
 * Copyright 2013 Consulo.org
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
package org.jetbrains.idea.devkit.module.extension;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import org.consulo.module.extension.ModuleExtensionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2:00/23.05.13
 */
public class PluginModuleExtensionProvider implements ModuleExtensionProvider<PluginModuleExtension, PluginMutableModuleExtension> {
  @Nullable
  @Override
  public Icon getIcon() {
    return AllIcons.Nodes.Plugin;
  }

  @NotNull
  @Override
  public String getName() {
    return "Consulo Develop Kit";
  }

  @NotNull
  @Override
  public Class<PluginModuleExtension> getImmutableClass() {
    return PluginModuleExtension.class;
  }

  @NotNull
  @Override
  public PluginModuleExtension createImmutable(@NotNull String id, @NotNull Module module) {
    return new PluginModuleExtension(id, module);
  }

  @NotNull
  @Override
  public PluginMutableModuleExtension createMutable(@NotNull String id,
                                                    @NotNull Module module,
                                                    @NotNull PluginModuleExtension pluginModuleExtension) {
    return new PluginMutableModuleExtension(id, module, pluginModuleExtension);
  }
}
