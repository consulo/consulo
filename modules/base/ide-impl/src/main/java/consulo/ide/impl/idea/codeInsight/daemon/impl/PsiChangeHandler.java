// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.component.extension.ExtensionPointName;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.psi.impl.PsiDocumentManagerImpl;
import consulo.language.editor.ChangeLocalityDetector;
import consulo.language.editor.impl.internal.daemon.DaemonCodeAnalyzerEx;
import consulo.language.editor.impl.internal.daemon.FileStatusMapImpl;
import consulo.language.editor.impl.internal.highlight.UpdateHighlightersUtilImpl;
import consulo.language.editor.impl.internal.markup.EditorMarkupModel;
import consulo.language.editor.impl.internal.markup.ErrorStripeUpdateManager;
import consulo.language.impl.internal.psi.PsiDocumentManagerBase;
import consulo.language.impl.internal.psi.PsiDocumentTransactionListener;
import consulo.language.impl.internal.psi.PsiTreeChangeEventImpl;
import consulo.language.psi.*;
import consulo.language.psi.event.PsiTreeChangeAdapter;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.ProjectCoreUtil;
import consulo.util.collection.SmartList;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class PsiChangeHandler extends PsiTreeChangeAdapter implements Disposable {
    private static final ExtensionPointName<ChangeLocalityDetector> EP_NAME = ExtensionPointName.create(ChangeLocalityDetector.class);
    private /*NOT STATIC!!!*/ final Key<Boolean> UPDATE_ON_COMMIT_ENGAGED = Key.create("UPDATE_ON_COMMIT_ENGAGED");

    private final Project myProject;
    private final Map<Document, List<Pair<PsiElement, Boolean>>> changedElements = ContainerUtil.createWeakMap();
    private final FileStatusMapImpl myFileStatusMap;

    PsiChangeHandler(@Nonnull Project project, @Nonnull MessageBusConnection connection) {
        myProject = project;
        myFileStatusMap = DaemonCodeAnalyzerEx.getInstanceEx(myProject).getFileStatusMap();
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(
            new DocumentListener() {
                @Override
                public void beforeDocumentChange(@Nonnull DocumentEvent e) {
                    final Document document = e.getDocument();
                    PsiDocumentManagerImpl documentManager = (PsiDocumentManagerImpl)PsiDocumentManager.getInstance(myProject);
                    if (documentManager.getSynchronizer().isInSynchronization(document)) {
                        return;
                    }
                    if (documentManager.getCachedPsiFile(document) == null) {
                        return;
                    }
                    if (document.getUserData(UPDATE_ON_COMMIT_ENGAGED) == null) {
                        document.putUserData(UPDATE_ON_COMMIT_ENGAGED, Boolean.TRUE);
                        PsiDocumentManagerBase.addRunOnCommit(document, () -> {
                            if (document.getUserData(UPDATE_ON_COMMIT_ENGAGED) != null) {
                                updateChangesForDocument(document);
                                document.putUserData(UPDATE_ON_COMMIT_ENGAGED, null);
                            }
                        });
                    }
                }
            },
            this
        );

        connection.subscribe(PsiDocumentTransactionListener.class, new PsiDocumentTransactionListener() {
            @Override
            public void transactionStarted(@Nonnull final Document doc, @Nonnull final PsiFile file) {
            }

            @Override
            public void transactionCompleted(@Nonnull final Document document, @Nonnull final PsiFile file) {
                updateChangesForDocument(document);
                document.putUserData(
                    UPDATE_ON_COMMIT_ENGAGED,
                    null
                ); // ensure we don't call updateChangesForDocument() twice which can lead to whole file re-highlight
            }
        });
    }

    @Override
    public void dispose() {
    }

    private void updateChangesForDocument(@Nonnull final Document document) {
        ApplicationManager.getApplication().assertIsWriteThread();
        if (myProject.isDisposed()) {
            return;
        }
        List<Pair<PsiElement, Boolean>> toUpdate = changedElements.get(document);
        if (toUpdate == null) {
            // The document has been changed, but psi hasn't
            // We may still need to rehighlight the file if there were changes inside highlighted ranges.
            if (UpdateHighlightersUtilImpl.isWhitespaceOptimizationAllowed(document)) {
                return;
            }

            // don't create PSI for files in other projects
            PsiElement file = PsiDocumentManager.getInstance(myProject).getCachedPsiFile(document);
            if (file == null) {
                return;
            }

            toUpdate = Collections.singletonList(Pair.create(file, true));
        }
        Application application = ApplicationManager.getApplication();
        final Editor editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
        if (editor != null && !application.isUnitTestMode()) {
            application.invokeLater(
                () -> {
                    if (!editor.isDisposed()) {
                        EditorMarkupModel markupModel = (EditorMarkupModel)editor.getMarkupModel();
                        PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
                        ErrorStripeUpdateManager.getInstance(myProject).setOrRefreshErrorStripeRenderer(markupModel, file);
                    }
                },
                IdeaModalityState.stateForComponent(editor.getComponent()),
                myProject.getDisposed()
            );
        }

        for (Pair<PsiElement, Boolean> changedElement : toUpdate) {
            PsiElement element = changedElement.getFirst();
            Boolean whiteSpaceOptimizationAllowed = changedElement.getSecond();
            updateByChange(element, document, whiteSpaceOptimizationAllowed);
        }
        changedElements.remove(document);
    }

    @Override
    public void childAdded(@Nonnull PsiTreeChangeEvent event) {
        queueElement(event.getParent(), true, event);
    }

    @Override
    public void childRemoved(@Nonnull PsiTreeChangeEvent event) {
        queueElement(event.getParent(), true, event);
    }

    @Override
    public void childReplaced(@Nonnull PsiTreeChangeEvent event) {
        queueElement(event.getNewChild(), typesEqual(event.getNewChild(), event.getOldChild()), event);
    }

    private static boolean typesEqual(final PsiElement newChild, final PsiElement oldChild) {
        return newChild != null && oldChild != null && newChild.getClass() == oldChild.getClass();
    }

    @Override
    public void childrenChanged(@Nonnull PsiTreeChangeEvent event) {
        if (((PsiTreeChangeEventImpl)event).isGenericChange()) {
            return;
        }
        queueElement(event.getParent(), true, event);
    }

    @Override
    public void beforeChildMovement(@Nonnull PsiTreeChangeEvent event) {
        queueElement(event.getOldParent(), true, event);
        queueElement(event.getNewParent(), true, event);
    }

    @Override
    public void beforeChildrenChange(@Nonnull PsiTreeChangeEvent event) {
        // this event sent always before every PSI change, even not significant one (like after quick typing/backspacing char)
        // mark file dirty just in case
        PsiFile psiFile = event.getFile();
        if (psiFile != null) {
            myFileStatusMap.markFileScopeDirtyDefensively(psiFile, event);
        }
    }

    @Override
    public void propertyChanged(@Nonnull PsiTreeChangeEvent event) {
        String propertyName = event.getPropertyName();
        if (!propertyName.equals(PsiTreeChangeEvent.PROP_WRITABLE)) {
            Object oldValue = event.getOldValue();
            if (oldValue instanceof VirtualFile && shouldBeIgnored((VirtualFile)oldValue)) {
                // ignore workspace.xml
                return;
            }
            myFileStatusMap.markAllFilesDirty(event);
        }
    }

    private void queueElement(@Nonnull PsiElement child, final boolean whitespaceOptimizationAllowed, @Nonnull PsiTreeChangeEvent event) {
        ApplicationManager.getApplication().assertIsWriteThread();
        PsiFile file = event.getFile();
        if (file == null) {
            file = child.getContainingFile();
        }
        if (file == null) {
            myFileStatusMap.markAllFilesDirty(child);
            return;
        }

        if (!child.isValid()) {
            return;
        }

        PsiDocumentManagerImpl pdm = (PsiDocumentManagerImpl)PsiDocumentManager.getInstance(myProject);
        Document document = pdm.getCachedDocument(file);
        if (document != null) {
            if (pdm.getSynchronizer().getTransaction(document) == null) {
                // content reload, language level change or some other big change
                myFileStatusMap.markAllFilesDirty(child);
                return;
            }

            List<Pair<PsiElement, Boolean>> toUpdate = changedElements.get(document);
            if (toUpdate == null) {
                toUpdate = new SmartList<>();
                changedElements.put(document, toUpdate);
            }
            toUpdate.add(Pair.create(child, whitespaceOptimizationAllowed));
        }
    }

    private void updateByChange(@Nonnull PsiElement child, @Nonnull final Document document, final boolean whitespaceOptimizationAllowed) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        final PsiFile file;
        try {
            file = child.getContainingFile();
        }
        catch (PsiInvalidElementAccessException e) {
            myFileStatusMap.markAllFilesDirty(e);
            return;
        }
        if (file == null || file instanceof PsiCompiledElement) {
            myFileStatusMap.markAllFilesDirty(child);
            return;
        }
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile != null && shouldBeIgnored(virtualFile)) {
            // ignore workspace.xml
            return;
        }

        int fileLength = file.getTextLength();
        if (!file.getViewProvider().isPhysical()) {
            myFileStatusMap.markFileScopeDirty(document, new TextRange(0, fileLength), fileLength, "Non-physical file update: " + file);
            return;
        }

        PsiElement element =
            whitespaceOptimizationAllowed && UpdateHighlightersUtilImpl.isWhitespaceOptimizationAllowed(document) ? child : child.getParent();
        while (true) {
            if (element == null || element instanceof PsiFile || element instanceof PsiDirectory) {
                myFileStatusMap.markAllFilesDirty("Top element: " + element);
                return;
            }

            final PsiElement scope = getChangeHighlightingScope(element);
            if (scope != null) {
                myFileStatusMap.markFileScopeDirty(document, scope.getTextRange(), fileLength, "Scope: " + scope);
                return;
            }

            element = element.getParent();
        }
    }

    private boolean shouldBeIgnored(@Nonnull VirtualFile virtualFile) {
        return ProjectCoreUtil.isProjectOrWorkspaceFile(virtualFile) || ProjectRootManager.getInstance(myProject)
            .getFileIndex()
            .isExcluded(virtualFile);
    }

    @Nullable
    private static PsiElement getChangeHighlightingScope(@Nonnull PsiElement element) {
        for (ChangeLocalityDetector detector : element.getProject()
            .getApplication()
            .getExtensionPoint(ChangeLocalityDetector.class)
            .getExtensionList()) {
            final PsiElement scope = detector.getChangeHighlightingDirtyScopeFor(element);
            if (scope != null) {
                return scope;
            }
        }

        return null;
    }
}
