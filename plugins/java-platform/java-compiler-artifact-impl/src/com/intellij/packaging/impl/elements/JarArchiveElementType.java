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
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.impl.ui.properties.JarArchiveElementPropertiesPanel;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPropertiesPanel;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author nik
 */
public class JarArchiveElementType extends CompositePackagingElementType<JarArchivePackagingElement> {
  @NotNull
  public static JarArchiveElementType getInstance() {
    return getInstance(JarArchiveElementType.class);
  }

  public JarArchiveElementType() {
    super("jar-archive", CompilerBundle.message("element.type.name.jar.archive"));
  }

  @Override
  public Icon getCreateElementIcon() {
    return AllIcons.Nodes.PpJar;
  }

  @NotNull
  @Override
  public JarArchivePackagingElement createEmpty(@NotNull Project project) {
    return new JarArchivePackagingElement(this);
  }

  @Override
  public PackagingElementPropertiesPanel createElementPropertiesPanel(@NotNull JarArchivePackagingElement element,
                                                                      @NotNull ArtifactEditorContext context) {
    return new JarArchiveElementPropertiesPanel(element, context);
  }

  @Override
  public CompositePackagingElement<?> createComposite(CompositePackagingElement<?> parent,
                                                      @Nullable String baseName,
                                                      @NotNull ArtifactEditorContext context) {
    final String initialValue = PackagingElementFactoryImpl.suggestFileName(parent, baseName != null ? baseName : "archive", ".jar");
    String path =
      Messages.showInputDialog(context.getProject(), "Enter archive name: ", "New Archive", null, initialValue, new FilePathValidator());
    if (path == null) {
      return null;
    }
    path = FileUtil.toSystemIndependentName(path);
    final String parentPath = PathUtil.getParentPath(path);
    final String fileName = PathUtil.getFileName(path);
    final PackagingElement<?> element = new JarArchivePackagingElement(this,fileName);
    return (CompositePackagingElement<?>)PackagingElementFactory.getInstance().createParentDirectories(parentPath, element);
  }
}
