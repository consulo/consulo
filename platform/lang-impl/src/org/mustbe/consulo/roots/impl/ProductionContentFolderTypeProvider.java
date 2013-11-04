/*
 * Copyright 2013 must-be.org
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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.ProjectBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 22:37/31.10.13
 */
public class ProductionContentFolderTypeProvider extends BaseContentFolderTypeProvider {
  public static ProductionContentFolderTypeProvider getInstance() {
    return EP_NAME.findExtension(ProductionContentFolderTypeProvider.class);
  }

  public ProductionContentFolderTypeProvider() {
    super("PRODUCTION");
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AllIcons.Modules.SourceRoot;
  }

  @Override
  public Icon getChildDirectoryIcon() {
    return AllIcons.Nodes.Package;
  }

  @NotNull
  @Override
  public String getName() {
    return ProjectBundle.message("module.toggle.sources.action");
  }
}
