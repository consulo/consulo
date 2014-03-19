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
package org.mustbe.consulo.sandLanguage.ide.artifact;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElementOutputKind;
import com.intellij.packaging.impl.artifacts.PlainArtifactType;
import com.intellij.packaging.impl.elements.ArtifactRootElementImpl;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.sandLanguage.ide.module.extension.SandModuleExtension;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 19.03.14
 */
public class SandArtifactType extends ArtifactType {
  @NonNls
  public static final String ID = "sand";

  public static PlainArtifactType getInstance() {
    return EP_NAME.findExtension(PlainArtifactType.class);
  }

  public SandArtifactType() {
    super(ID, "Sand");
  }

  @Override
  public boolean isAvailableForAdd(@NotNull ModulesProvider modulesProvider) {
    return ModuleUtil.hasModuleExtension(modulesProvider, SandModuleExtension.class);
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AllIcons.Nodes.Artifact;
  }

  @Override
  public String getDefaultPathFor(@NotNull PackagingElementOutputKind kind) {
    return "/";
  }

  @Override
  @NotNull
  public CompositePackagingElement<?> createRootElement(@NotNull String artifactName) {
    return new ArtifactRootElementImpl();
  }
}
