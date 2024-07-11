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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems.actions;

import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems.SourceItemsTree;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.element.PackagingElementOutputKind;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.PackagingElementPath;
import consulo.compiler.artifact.PackagingElementProcessor;
import consulo.compiler.artifact.element.FileCopyPackagingElement;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingSourceItem;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class ExtractIntoDefaultLocationAction extends PutIntoDefaultLocationActionBase {
  public ExtractIntoDefaultLocationAction(SourceItemsTree sourceItemsTree, ArtifactEditorEx artifactEditor) {
    super(sourceItemsTree, artifactEditor);
  }

  @Override
  public void update(AnActionEvent e) {
    final String pathForClasses = myArtifactEditor.getArtifact().getArtifactType().getDefaultPathFor(PackagingElementOutputKind.DIRECTORIES_WITH_CLASSES);
    final Presentation presentation = e.getPresentation();
    if (onlyJarsSelected() && pathForClasses != null) {
      presentation.setText("Extract Into " + getTargetLocationText(Collections.singleton(pathForClasses)));
      presentation.setVisible(true);
    }
    else {
      presentation.setVisible(false);
    }
  }

  private boolean onlyJarsSelected() {
    for (PackagingSourceItem item : mySourceItemsTree.getSelectedItems()) {
      if (item.isProvideElements() && (!item.getKindOfProducedElements().containsJarFiles() || item.getKindOfProducedElements().containsDirectoriesWithClasses())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final String pathForClasses = myArtifactEditor.getArtifact().getArtifactType().getDefaultPathFor(PackagingElementOutputKind.DIRECTORIES_WITH_CLASSES);
    if (pathForClasses != null) {
      final List<PackagingElement<?>> extracted = new ArrayList<PackagingElement<?>>();
      for (PackagingSourceItem item : mySourceItemsTree.getSelectedItems()) {
        final ArtifactEditorContext context = myArtifactEditor.getContext();
        final List<? extends PackagingElement<?>> elements = item.createElements(context);
        ArtifactUtil.processElementsWithSubstitutions(elements, context, context.getArtifactType(), PackagingElementPath.EMPTY, new PackagingElementProcessor<PackagingElement<?>>() {
          @Override
          public boolean process(@Nonnull PackagingElement<?> element, @Nonnull PackagingElementPath path) {
            if (element instanceof FileCopyPackagingElement copyPackagingElement) {
              final VirtualFile file = copyPackagingElement.findFile();
              if (file != null) {
                final VirtualFile archiveRoot = ArchiveVfsUtil.getVirtualFileForArchive(file);
                if (archiveRoot != null) {
                  extracted.add(PackagingElementFactory.getInstance(e.getData(Project.KEY)).createExtractedDirectory(archiveRoot));
                }
              }
            }
            return true;
          }
        });
      }
      myArtifactEditor.getLayoutTreeComponent().putElements(pathForClasses, extracted);
    }
  }
}
