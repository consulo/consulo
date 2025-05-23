// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.stickyLine;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ReadAction;
import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.StickyLinesCollector;
import consulo.codeEditor.internal.stickyLine.StickyLineInfo;
import consulo.document.util.TextRange;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.fileEditor.structureView.tree.TreeElement;
import consulo.language.editor.impl.highlight.EditorBoundHighlightingPass;
import consulo.language.editor.structureView.PsiStructureViewFactory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;

public class StickyLinesPass extends EditorBoundHighlightingPass implements DumbAware {
    private final StickyLinesCollector myCollector;
    private Collection<StickyLineInfo> myCollectedLines;

    public StickyLinesPass(Editor editor, PsiFile file) {
        super(editor, file, false);
        myCollector = new StickyLinesCollector(file.getProject(), editor.getDocument());
    }

    @Override
    public void doCollectInformation(@Nonnull ProgressIndicator progress) {
        if (myDocument == null) {
            return;
        }

        StructureViewBuilder builder = ReadAction.compute(() -> PsiStructureViewFactory.createBuilderForFile(myFile));
        if (!(builder instanceof TreeBasedStructureViewBuilder treeBasedStructureViewBuilder)) {
            return;
        }

        myCollectedLines = new ArrayList<>();

        StructureViewModel model = treeBasedStructureViewBuilder.createStructureViewModel(myEditor);

        acceptItems(model.getRoot(), progress);
    }

    private void acceptItems(TreeElement treeElement, @Nonnull ProgressIndicator progress) {
        if (treeElement instanceof StructureViewTreeElement structureViewTreeElement) {
            Object value = structureViewTreeElement.getValue();

            if (value instanceof PsiFile) {
                // skip root as file
            } else if (value instanceof PsiElement psiElement) {
                StickyLineInfo info = ReadAction.compute(() -> convertToStickyLine(psiElement));
                if (info != null) {
                    myCollectedLines.add(info);
                }
            }
        }

        for (TreeElement element : treeElement.getChildren()) {
            acceptItems(element, progress);
        }
    }

    @RequiredReadAction
    private StickyLineInfo convertToStickyLine(@Nonnull PsiElement element) {
        TextRange elementTextRange = element.getTextRange();
        if (element instanceof PsiNameIdentifierOwner nameOwner) {
            PsiElement nameIdentifier = nameOwner.getNameIdentifier();

            if (nameIdentifier != null) {
                TextRange nameTextRange = nameIdentifier.getTextRange();
                if (elementTextRange.contains(nameTextRange)) {
                    return new StickyLineInfo(nameTextRange.getStartOffset(), elementTextRange.getEndOffset(), null);
                }
            }
        }

        return new StickyLineInfo(elementTextRange);
    }

    @Override
    @RequiredUIAccess
    public void doApplyInformationToEditor() {
        if (myCollectedLines != null) {
            myCollector.applyLines(myFile, myCollectedLines);
            myCollectedLines = null;
        }
    }
}
