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
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.ComplexPackagingElementType;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
* @author nik
*/
public class LibraryElementType extends ComplexPackagingElementType<LibraryPackagingElement> {
  public static LibraryElementType getInstance() {
    return getInstance(LibraryElementType.class);
  }

  public LibraryElementType() {
    super("library", CompilerBundle.message("element.type.name.library.files"));
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Nodes.PpLib;
  }

  @Override
  public boolean isAvailableForAdd(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact) {
    return !getAllLibraries(context).isEmpty();
  }

  @Nonnull
  public List<? extends LibraryPackagingElement> chooseAndCreate(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact,
                                                                 @Nonnull CompositePackagingElement<?> parent) {
    final List<Library> selected = context.chooseLibraries(ProjectBundle.message("dialog.title.packaging.choose.library"));
    final List<LibraryPackagingElement> elements = new ArrayList<LibraryPackagingElement>();
    for (Library library : selected) {
      elements.add(new LibraryPackagingElement(library.getTable().getTableLevel(), library.getName(), null));
    }
    return elements;
  }

  private static List<Library> getAllLibraries(ArtifactEditorContext context) {
    List<Library> libraries = new ArrayList<Library>();
    ContainerUtil.addAll(libraries, LibraryTablesRegistrar.getInstance().getLibraryTable().getLibraries());
    ContainerUtil.addAll(libraries, LibraryTablesRegistrar.getInstance().getLibraryTable(context.getProject()).getLibraries());
    return libraries;
  }

  @Nonnull
  public LibraryPackagingElement createEmpty(@Nonnull Project project) {
    return new LibraryPackagingElement();
  }

  @Override
  public String getShowContentActionText() {
    return "Show Library Files";
  }
}
