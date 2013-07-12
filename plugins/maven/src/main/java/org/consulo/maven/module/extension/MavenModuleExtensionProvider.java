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
package org.consulo.maven.module.extension;

import com.intellij.openapi.module.Module;
import icons.MavenIcons;
import org.consulo.module.extension.ModuleExtensionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 15:19/12.07.13
 */
public class MavenModuleExtensionProvider implements ModuleExtensionProvider<MavenModuleExtension, MavenMutableModuleExtension> {
  @Nullable
  @Override
  public Icon getIcon() {
    return MavenIcons.MavenLogo;
  }

  @NotNull
  @Override
  public String getName() {
    return "Maven";
  }

  @NotNull
  @Override
  public Class<MavenModuleExtension> getImmutableClass() {
    return MavenModuleExtension.class;
  }

  @NotNull
  @Override
  public MavenModuleExtension createImmutable(@NotNull String id, @NotNull Module module) {
    return new MavenModuleExtension(id, module);
  }

  @NotNull
  @Override
  public MavenMutableModuleExtension createMutable(@NotNull String id,
                                                   @NotNull Module module,
                                                   @NotNull MavenModuleExtension mavenModuleExtension) {
    return new MavenMutableModuleExtension(id, module, mavenModuleExtension);
  }
}
