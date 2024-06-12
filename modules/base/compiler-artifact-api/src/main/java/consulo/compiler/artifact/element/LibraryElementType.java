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
package consulo.compiler.artifact.element;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.localize.CompilerLocalize;
import consulo.content.library.Library;
import consulo.project.Project;
import consulo.project.content.library.ProjectLibraryTable;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
* @author nik
*/
@ExtensionImpl
public class LibraryElementType extends ComplexPackagingElementType<LibraryPackagingElement> {
  public static LibraryElementType getInstance() {
    return getInstance(LibraryElementType.class);
  }

  public LibraryElementType() {
    super("library", CompilerLocalize.elementTypeNameLibraryFiles().get());
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

  @Override
  @Nonnull
  public List<? extends LibraryPackagingElement> chooseAndCreate(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact,
                                                                 @Nonnull CompositePackagingElement<?> parent) {
    final List<Library> selected = context.chooseLibraries(ProjectLocalize.dialogTitlePackagingChooseLibrary().get());
    final List<LibraryPackagingElement> elements = new ArrayList<>();
    for (Library library : selected) {
      elements.add(new LibraryPackagingElement(library.getTable().getTableLevel(), library.getName(), null));
    }
    return elements;
  }

  private static List<Library> getAllLibraries(ArtifactEditorContext context) {
    List<Library> libraries = new ArrayList<>();
    ContainerUtil.addAll(libraries, ProjectLibraryTable.getInstance(context.getProject()).getLibraries());
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
