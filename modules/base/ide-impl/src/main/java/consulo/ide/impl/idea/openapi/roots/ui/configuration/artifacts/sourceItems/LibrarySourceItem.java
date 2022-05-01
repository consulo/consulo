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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems;

import consulo.application.AllIcons;
import consulo.virtualFileSystem.VirtualFilePresentation;
import consulo.ui.ex.tree.PresentationData;
import consulo.content.impl.internal.library.LibraryEx;
import consulo.content.library.Library;
import consulo.content.base.BinariesOrderRootType;
import consulo.virtualFileSystem.VirtualFile;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.element.PackagingElementOutputKind;
import consulo.ide.impl.idea.packaging.impl.elements.LibraryPackagingElement;
import consulo.ide.impl.idea.packaging.impl.ui.LibraryElementPresentation;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingSourceItem;
import consulo.compiler.artifact.ui.SourceItemPresentation;
import consulo.ide.impl.idea.packaging.ui.SourceItemWeights;
import consulo.ui.ex.SimpleTextAttributes;
import javax.annotation.Nonnull;

import java.util.List;

/**
 * @author nik
 */
public class LibrarySourceItem extends PackagingSourceItem {
  private final Library myLibrary;

  public LibrarySourceItem(@Nonnull Library library) {
    myLibrary = library;
  }

  @Override
  public SourceItemPresentation createPresentation(@Nonnull ArtifactEditorContext context) {
    return new LibrarySourceItemPresentation(myLibrary, context);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof LibrarySourceItem && myLibrary.equals(((LibrarySourceItem)obj).myLibrary);
  }

  @Override
  public int hashCode() {
    return myLibrary.hashCode();
  }

  @Nonnull
  public Library getLibrary() {
    return myLibrary;
  }

  @Nonnull
  @Override
  public PackagingElementOutputKind getKindOfProducedElements() {
    return LibraryPackagingElement.getKindForLibrary(myLibrary);
  }

  @Override
  @Nonnull
  public List<? extends PackagingElement<?>> createElements(@Nonnull ArtifactEditorContext context) {
    return PackagingElementFactory.getInstance(context.getProject()).createLibraryElements(myLibrary);
  }

  private static class LibrarySourceItemPresentation extends SourceItemPresentation {
    private final Library myLibrary;
    private final ArtifactEditorContext myContext;

    public LibrarySourceItemPresentation(Library library, ArtifactEditorContext context) {
      myLibrary = library;
      myContext = context;
    }

    @Override
    public boolean canNavigateToSource() {
      return myLibrary != null;
    }

    @Override
    public void navigateToSource() {
      myContext.selectLibrary(myLibrary);
    }

    @Override
    public String getPresentableName() {
      final String name = myLibrary.getName();
      if (name != null) {
        return name;
      }
      final VirtualFile[] files = myLibrary.getFiles(BinariesOrderRootType.getInstance());
      return files.length > 0 ? files[0].getName() : "Empty Library";
    }

    @Override
    public void render(@Nonnull PresentationData presentationData, SimpleTextAttributes mainAttributes,
                       SimpleTextAttributes commentAttributes) {
      final String name = myLibrary.getName();
      if (name != null) {
        presentationData.setIcon(AllIcons.Nodes.PpLib);
        presentationData.addText(name, mainAttributes);
        presentationData.addText(LibraryElementPresentation.getLibraryTableComment(myLibrary), commentAttributes);
      }
      else {
        if (((LibraryEx)myLibrary).isDisposed()) {
          //todo[nik] disposed library should not be shown in the tree
          presentationData.addText("Invalid Library", SimpleTextAttributes.ERROR_ATTRIBUTES);
          return;
        }
        final VirtualFile[] files = myLibrary.getFiles(BinariesOrderRootType.getInstance());
        if (files.length > 0) {
          final VirtualFile file = files[0];
          presentationData.setIcon(VirtualFilePresentation.getIcon(file));
          presentationData.addText(file.getName(), mainAttributes);
        }
        else {
          presentationData.addText("Empty Library", SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
      }
    }

    @Override
    public int getWeight() {
      return SourceItemWeights.LIBRARY_WEIGHT;
    }
  }
}
