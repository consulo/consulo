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
package com.intellij.packaging.impl.elements;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.CompositePackagingElementType;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.util.PathUtil;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author nik
 */
public class DirectoryElementType extends CompositePackagingElementType<DirectoryPackagingElement> {
  public static DirectoryElementType getInstance() {
    return getInstance(DirectoryElementType.class);
  }

  public DirectoryElementType() {
    super("directory", CompilerBundle.message("element.type.name.directory"));
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Actions.NewFolder;
  }

  @Override
  @Nonnull
  public DirectoryPackagingElement createEmpty(@Nonnull Project project) {
    return new DirectoryPackagingElement();
  }

  /*@Override
  public PackagingElementPropertiesPanel createElementPropertiesPanel(@NotNull DirectoryPackagingElement element,
                                                                      @NotNull ArtifactEditorContext context) {
    if (JpsArtifactUtil.isArchiveName(element.getDirectoryName())) {
      return new DirectoryElementPropertiesPanel(element, context);
    }
    return null;
  }    */

  @Override
  public CompositePackagingElement<?> createComposite(CompositePackagingElement<?> parent,
                                                      String baseName,
                                                      @Nonnull ArtifactEditorContext context) {
    final String initialValue = PackagingElementFactoryImpl.suggestFileName(parent, baseName != null ? baseName : "folder", "");
    String path = Messages
      .showInputDialog(context.getProject(), "Enter directory name: ", "New Directory", null, initialValue, new FilePathValidator());
    if (path == null) {
      return null;
    }
    path = FileUtil.toSystemIndependentName(path);
    final String parentPath = PathUtil.getParentPath(path);
    final String fileName = PathUtil.getFileName(path);
    final PackagingElement<?> element = new DirectoryPackagingElement(fileName);
    return (CompositePackagingElement<?>)PackagingElementFactoryImpl.getInstance(context.getProject()).createParentDirectories(parentPath, element);

  }

}
