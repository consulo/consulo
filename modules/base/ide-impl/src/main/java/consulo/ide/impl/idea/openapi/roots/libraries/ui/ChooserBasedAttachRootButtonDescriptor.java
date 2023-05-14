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
package consulo.ide.impl.idea.openapi.roots.libraries.ui;

import consulo.language.editor.LangDataKeys;
import consulo.content.library.ui.AttachRootButtonDescriptor;
import consulo.dataContext.DataContext;
import consulo.fileChooser.IdeaFileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.module.Module;
import consulo.content.OrderRootType;
import consulo.content.library.ui.LibraryEditor;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import javax.swing.*;

/**
* @author nik
*/
public abstract class ChooserBasedAttachRootButtonDescriptor extends AttachRootButtonDescriptor {
  public ChooserBasedAttachRootButtonDescriptor(@Nonnull OrderRootType rootType, @Nonnull String buttonText) {
    super(rootType, buttonText);
  }

  public FileChooserDescriptor createChooserDescriptor() {
    return FileChooserDescriptorFactory.createMultipleJavaPathDescriptor();
  }

  public abstract String getChooserTitle(final @Nullable String libraryName);
  public abstract String getChooserDescription();

  @Override
  public VirtualFile[] selectFiles(final @Nonnull JComponent parent, @Nullable VirtualFile initialSelection,
                                   final @Nonnull DataContext dataContext, @Nonnull LibraryEditor libraryEditor) {
    final FileChooserDescriptor chooserDescriptor = createChooserDescriptor();
    chooserDescriptor.setTitle(getChooserTitle(libraryEditor.getName()));
    chooserDescriptor.setDescription(getChooserDescription());
    Module contextModule = dataContext.getData(Module.KEY);
    if (contextModule != null) {
      chooserDescriptor.putUserData(LangDataKeys.MODULE_CONTEXT, contextModule);
    }
    return IdeaFileChooser.chooseFiles(chooserDescriptor, parent, contextModule != null ? contextModule.getProject() : null, initialSelection);
  }
}
