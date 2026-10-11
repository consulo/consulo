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

import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.PackagingElementPath;
import consulo.compiler.artifact.PackagingElementProcessor;
import consulo.compiler.artifact.element.FileCopyPackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.element.PackagingElementOutputKind;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingSourceItem;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems.SourceItemsTree;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
public class ExtractIntoDefaultLocationAction extends PutIntoDefaultLocationActionBase {
    public ExtractIntoDefaultLocationAction(SourceItemsTree sourceItemsTree, ArtifactEditorEx artifactEditor) {
        super(sourceItemsTree, artifactEditor);
    }

    @Override
    public void update(AnActionEvent e) {
        String pathForClasses =
            myArtifactEditor.getArtifact().getArtifactType().getDefaultPathFor(PackagingElementOutputKind.DIRECTORIES_WITH_CLASSES);
        Presentation presentation = e.getPresentation();
        if (onlyJarsSelected() && pathForClasses != null) {
            presentation.setText(ProjectLocalize.artifactActionExtractInto0Text(getTargetLocationText(Set.of(pathForClasses))));
            presentation.setVisible(true);
        }
        else {
            presentation.setVisible(false);
        }
    }

    private boolean onlyJarsSelected() {
        for (PackagingSourceItem item : mySourceItemsTree.getSelectedItems()) {
            PackagingElementOutputKind kind = item.getKindOfProducedElements();
            if (item.isProvideElements() && (!kind.containsJarFiles() || kind.containsDirectoriesWithClasses())) {
                return false;
            }
        }
        return true;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        String pathForClasses =
            myArtifactEditor.getArtifact().getArtifactType().getDefaultPathFor(PackagingElementOutputKind.DIRECTORIES_WITH_CLASSES);
        if (pathForClasses == null) {
            return;
        }

        ArtifactEditorContext context = myArtifactEditor.getContext();
        PackagingElementFactory factory = PackagingElementFactory.getInstance(context.getProject());
        List<PackagingElement<?>> extracted = new ArrayList<>();
        for (PackagingSourceItem item : mySourceItemsTree.getSelectedItems()) {
            ArtifactUtil.processElementsWithSubstitutions(
                item.createElements(context),
                context,
                context.getArtifactType(),
                PackagingElementPath.EMPTY,
                new PackagingElementProcessor<PackagingElement<?>>() {
                    @Override
                    public boolean process(PackagingElement<?> element, PackagingElementPath path) {
                        if (element instanceof FileCopyPackagingElement copyPackagingElement) {
                            VirtualFile file = copyPackagingElement.findFile();
                            VirtualFile archiveRoot = file == null ? null : ArchiveVfsUtil.getVirtualFileForArchive(file);
                            if (archiveRoot != null) {
                                extracted.add(factory.createExtractedDirectory(archiveRoot));
                            }
                        }
                        return true;
                    }
                }
            );
        }
        myArtifactEditor.getLayoutTreeComponent().putElements(pathForClasses, extracted);
    }
}
