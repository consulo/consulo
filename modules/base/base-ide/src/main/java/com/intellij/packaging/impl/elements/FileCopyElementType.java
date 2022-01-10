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
package com.intellij.packaging.impl.elements;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElementType;
import com.intellij.packaging.ui.ArtifactEditorContext;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
* @author nik
*/
public class FileCopyElementType extends PackagingElementType<FileCopyPackagingElement> {
  public static FileCopyElementType getInstance() {
    return getInstance(FileCopyElementType.class);
  }

  public FileCopyElementType() {
    super("file-copy", "File");
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.FileTypes.Text;
  }

  @Nonnull
  public List<? extends FileCopyPackagingElement> chooseAndCreate(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact,
                                                                  @Nonnull CompositePackagingElement<?> parent) {
    final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, true, true, false, true);
    final VirtualFile[] files = FileChooser.chooseFiles(descriptor, context.getProject(), null);
    final List<FileCopyPackagingElement> list = new ArrayList<FileCopyPackagingElement>();
    for (VirtualFile file : files) {
      list.add(new FileCopyPackagingElement(file.getPath()));
    }
    return list;
  }

  @Nonnull
  public FileCopyPackagingElement createEmpty(@Nonnull Project project) {
    return new FileCopyPackagingElement();
  }
}
