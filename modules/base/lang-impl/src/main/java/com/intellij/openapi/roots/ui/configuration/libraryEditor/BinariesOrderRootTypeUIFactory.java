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
 * User: anna
 * Date: 26-Dec-2007
 */
package com.intellij.openapi.roots.ui.configuration.libraryEditor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.ui.SdkPathEditor;
import com.intellij.openapi.roots.ui.OrderRootTypeUIFactory;
import consulo.roots.types.BinariesOrderRootType;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

public class BinariesOrderRootTypeUIFactory implements OrderRootTypeUIFactory {

  @Nonnull
  @Override
  public SdkPathEditor createPathEditor(Sdk sdk) {
    return new SdkPathEditor(ProjectBundle.message("library.binaries.node"), BinariesOrderRootType.getInstance(),
                             new FileChooserDescriptor(true, true, true, false, true, true), sdk);
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Nodes.CompiledClassesFolder;
  }

  @Nonnull
  @Override
  public String getNodeText() {
    return ProjectBundle.message("library.binaries.node");
  }
}
