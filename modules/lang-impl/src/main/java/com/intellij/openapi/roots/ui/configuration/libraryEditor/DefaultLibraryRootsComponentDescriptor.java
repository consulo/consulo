/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.roots.ui.configuration.libraryEditor;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.ui.Util;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.ui.AttachRootButtonDescriptor;
import com.intellij.openapi.roots.libraries.ui.LibraryRootsComponentDescriptor;
import com.intellij.openapi.roots.libraries.ui.OrderRootTypePresentation;
import com.intellij.openapi.roots.libraries.ui.RootDetector;
import consulo.roots.types.DocumentationOrderRootType;
import com.intellij.openapi.roots.ui.OrderRootTypeUIFactory;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author nik
 */
public class DefaultLibraryRootsComponentDescriptor extends LibraryRootsComponentDescriptor {
  public static final ExtensionPointName<RootDetector> EP_NAME = ExtensionPointName.create("com.intellij.defaultLibraryRootDetector");

  @Override
  public OrderRootTypePresentation getRootTypePresentation(@NotNull OrderRootType type) {
    return getDefaultPresentation(type);
  }

  @NotNull
  @Override
  public List<? extends AttachRootButtonDescriptor> createAttachButtons() {
    return Arrays.asList(new AttachUrlJavadocDescriptor());
  }

  @NotNull
  @Override
  public List<? extends RootDetector> getRootDetectors() {
    return Arrays.asList(EP_NAME.getExtensions());
  }

  public static OrderRootTypePresentation getDefaultPresentation(OrderRootType type) {
    final OrderRootTypeUIFactory factory = OrderRootTypeUIFactory.FACTORY.getByKey(type);
    return new OrderRootTypePresentation(factory.getNodeText(), factory.getIcon());
  }
  private static class AttachUrlJavadocDescriptor extends AttachRootButtonDescriptor {
    private AttachUrlJavadocDescriptor() {
      super(DocumentationOrderRootType.getInstance(), ProjectBundle.message("module.libraries.javadoc.url.button"));
    }

    @Override
    public VirtualFile[] selectFiles(@NotNull JComponent parent,
                                     @Nullable VirtualFile initialSelection,
                                     @Nullable Module contextModule,
                                     @NotNull LibraryEditor libraryEditor) {
      final VirtualFile vFile = Util.showSpecifyJavadocUrlDialog(parent);
      if (vFile != null) {
        return new VirtualFile[]{vFile};
      }
      return VirtualFile.EMPTY_ARRAY;
    }
  }
}
