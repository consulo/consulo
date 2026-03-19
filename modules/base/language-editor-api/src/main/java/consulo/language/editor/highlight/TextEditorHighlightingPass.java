// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.highlight;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.colorScheme.EditorColorsScheme;
import consulo.document.Document;
import consulo.fileEditor.highlight.HighlightingPass;
import consulo.language.editor.internal.DaemonCodeAnalyzerInternal;
import consulo.language.editor.internal.DaemonProgressIndicator;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.util.IncorrectOperationException;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class TextEditorHighlightingPass implements HighlightingPass {
    public static final TextEditorHighlightingPass[] EMPTY_ARRAY = new TextEditorHighlightingPass[0];
    protected final @Nullable Document myDocument;
    
    protected final Project myProject;
    private final boolean myRunIntentionPassAfter;
    private final long myInitialDocStamp;
    private final long myInitialPsiStamp;
    private volatile int[] myCompletionPredecessorIds = ArrayUtil.EMPTY_INT_ARRAY;
    private volatile int[] myStartingPredecessorIds = ArrayUtil.EMPTY_INT_ARRAY;
    private volatile int myId;
    private volatile boolean myDumb;
    private EditorColorsScheme myColorsScheme;

    protected TextEditorHighlightingPass(Project project, @Nullable Document document, boolean runIntentionPassAfter) {
        myDocument = document;
        myProject = project;
        myRunIntentionPassAfter = runIntentionPassAfter;
        myInitialDocStamp = document == null ? 0 : document.getModificationStamp();
        myInitialPsiStamp = PsiModificationTracker.getInstance(myProject).getModificationCount();
    }

    protected TextEditorHighlightingPass(Project project, @Nullable Document document) {
        this(project, document, true);
    }

    @RequiredReadAction
    @Override
    public final void collectInformation(ProgressIndicator progress) {
        if (!isValid()) {
            return; //Document has changed.
        }
        if (!(progress instanceof DaemonProgressIndicator)) {
            throw new IncorrectOperationException("Highlighting must be run under DaemonProgressIndicator, but got: " + progress);
        }
        myDumb = DumbService.getInstance(myProject).isDumb();
        doCollectInformation(progress);
    }

    public @Nullable EditorColorsScheme getColorsScheme() {
        return myColorsScheme;
    }

    public void setColorsScheme(@Nullable EditorColorsScheme colorsScheme) {
        myColorsScheme = colorsScheme;
    }

    protected boolean isDumbMode() {
        return myDumb;
    }

    @RequiredReadAction
    protected boolean isValid() {
        if (isDumbMode() && !DumbService.isDumbAware(this)) {
            return false;
        }

        if (PsiModificationTracker.getInstance(myProject).getModificationCount() != myInitialPsiStamp) {
            return false;
        }

        if (myDocument != null) {
            if (myDocument.getModificationStamp() != myInitialDocStamp) {
                return false;
            }
            PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(myDocument);
            return file != null && file.isValid();
        }

        return true;
    }

    @Override
    @RequiredReadAction
    public final void applyInformationToEditor() {
        if (!isValid()) {
            return; // Document has changed.
        }
        if (DumbService.getInstance(myProject).isDumb() && !DumbService.isDumbAware(this)) {
            Document document = getDocument();
            PsiFile file = document == null ? null : PsiDocumentManager.getInstance(myProject).getPsiFile(document);
            if (file != null) {
                DaemonCodeAnalyzerInternal.getInstanceEx(myProject).getFileStatusMap().markFileUpToDate(getDocument(), getId());
            }
            return;
        }
        doApplyInformationToEditor();
    }

    public abstract void doCollectInformation(ProgressIndicator progress);

    public abstract void doApplyInformationToEditor();

    public final int getId() {
        return myId;
    }

    public final void setId(int id) {
        myId = id;
    }

    
    public List<HighlightInfo> getInfos() {
        return Collections.emptyList();
    }

    
    public final int[] getCompletionPredecessorIds() {
        return myCompletionPredecessorIds;
    }

    public final void setCompletionPredecessorIds(int[] completionPredecessorIds) {
        myCompletionPredecessorIds = completionPredecessorIds;
    }

    public @Nullable Document getDocument() {
        return myDocument;
    }

    
    public final int[] getStartingPredecessorIds() {
        return myStartingPredecessorIds;
    }

    public final void setStartingPredecessorIds(int[] startingPredecessorIds) {
        myStartingPredecessorIds = startingPredecessorIds;
    }

    @Override
    public String toString() {
        return (getClass().isAnonymousClass() ? getClass().getSuperclass() : getClass()).getSimpleName() + "; id=" + getId();
    }

    public boolean isRunIntentionPassAfter() {
        return myRunIntentionPassAfter;
    }
}
