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
package org.jetbrains.plugins.groovy.griffon.module.extension;

import com.intellij.openapi.module.Module;
import icons.JetgroovyIcons;
import org.consulo.module.extension.ModuleExtensionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 14:35/30.06.13
 */
public class GriffonModuleExtensionProvider implements ModuleExtensionProvider<GriffonModuleExtension,GriffonMutableModuleExtension> {
  @Nullable
  @Override
  public Icon getIcon() {
    return JetgroovyIcons.Griffon.Griffon;
  }

  @NotNull
  @Override
  public String getName() {
    return "Griffon";
  }

  @NotNull
  @Override
  public Class<GriffonModuleExtension> getImmutableClass() {
    return GriffonModuleExtension.class;
  }

  @NotNull
  @Override
  public GriffonModuleExtension createImmutable(@NotNull String id, @NotNull Module module) {
    return new GriffonModuleExtension(id, module);
  }

  @NotNull
  @Override
  public GriffonMutableModuleExtension createMutable(@NotNull String id,
                                                     @NotNull Module module,
                                                     @NotNull GriffonModuleExtension griffonModuleExtension) {
    return new GriffonMutableModuleExtension(id, module, griffonModuleExtension);
  }
}
