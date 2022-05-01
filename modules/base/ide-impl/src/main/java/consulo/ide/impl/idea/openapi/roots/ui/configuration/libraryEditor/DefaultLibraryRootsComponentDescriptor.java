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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor;

import consulo.ide.impl.idea.openapi.projectRoots.ui.Util;
import consulo.ide.impl.idea.openapi.roots.ui.OrderRootTypeUIFactory;
import consulo.component.extension.ExtensionPointName;
import consulo.content.OrderRootType;
import consulo.content.base.DocumentationOrderRootType;
import consulo.content.library.ui.*;
import consulo.dataContext.DataContext;
import consulo.project.ProjectBundle;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author nik
 */
public class DefaultLibraryRootsComponentDescriptor extends LibraryRootsComponentDescriptor {
  public static final ExtensionPointName<RootDetector> EP_NAME = ExtensionPointName.create("consulo.defaultLibraryRootDetector");

  @Override
  public OrderRootTypePresentation getRootTypePresentation(@Nonnull OrderRootType type) {
    return getDefaultPresentation(type);
  }

  @Nonnull
  @Override
  public List<? extends AttachRootButtonDescriptor> createAttachButtons() {
    return Arrays.asList(new AttachUrlJavadocDescriptor());
  }

  @Nonnull
  @Override
  public List<? extends RootDetector> getRootDetectors() {
    return EP_NAME.getExtensionList();
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
    public VirtualFile[] selectFiles(@Nonnull JComponent parent,
                                     @Nullable VirtualFile initialSelection,
                                     @Nullable DataContext dataContext,
                                     @Nonnull LibraryEditor libraryEditor) {
      final VirtualFile vFile = Util.showSpecifyJavadocUrlDialog(parent);
      if (vFile != null) {
        return new VirtualFile[]{vFile};
      }
      return VirtualFile.EMPTY_ARRAY;
    }
  }
}
