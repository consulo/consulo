// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.impl.internal.daemon;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
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
import consulo.language.editor.ChangeLocalityDetector;
import consulo.language.editor.FileStatusMap;
import consulo.language.editor.impl.internal.highlight.UpdateHighlightersUtilImpl;
import consulo.language.editor.impl.internal.markup.EditorMarkupModel;
import consulo.language.editor.impl.internal.markup.ErrorStripeUpdateManager;
import consulo.language.editor.internal.DaemonCodeAnalyzerInternal;
import consulo.language.impl.internal.psi.PsiDocumentManagerBase;
import consulo.language.impl.internal.psi.PsiDocumentTransactionListener;
import consulo.language.impl.internal.psi.PsiTreeChangeEventImpl;
import consulo.language.psi.*;
import consulo.language.psi.event.PsiTreeChangeAdapter;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.ProjectCoreUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.collection.SmartList;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Records PSI/document changes and defers the daemon-dirty-scope computation off the write/commit path.
 * <p>
 * Matching the JetBrains design, PSI tree change events and the on-commit callback only <b>record</b>
 * changed elements (guarded by {@link #changedElements}) and <b>queue</b> a background pooled-thread alarm.
 * The alarm's {@link #run()} then processes the recorded changes under a read action on a background thread,
 * so no write lock is required on the commit finalization path (which runs outside the write action).
 */
final class PsiChangeHandler extends PsiTreeChangeAdapter implements Disposable, Runnable {
    private /*NOT STATIC!!!*/ final Key<Boolean> UPDATE_ON_COMMIT_ENGAGED = Key.create("UPDATE_ON_COMMIT_ENGAGED");

    private final Project myProject;
    private final Map<Document, List<Change>> changedElements = new WeakHashMap<>(); // guarded by changedElements
    private final FileStatusMap myFileStatusMap;
    private final Alarm myUpdateFileStatusAlarm;

    private record Change(PsiElement psiElement, boolean whiteSpaceOptimizationAllowed) {
    }

    PsiChangeHandler(Project project, MessageBusConnection connection) {
        myProject = project;
        myFileStatusMap = DaemonCodeAnalyzerInternal.getInstanceEx(myProject).getFileStatusMap();
        myUpdateFileStatusAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);

        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(
            new DocumentListener() {
                @Override
                @RequiredReadAction
                public void beforeDocumentChange(DocumentEvent e) {
                    Document document = e.getDocument();
                    PsiDocumentManagerBase documentManager = (PsiDocumentManagerBase)PsiDocumentManager.getInstance(myProject);
                    if (documentManager.getSynchronizer().isInSynchronization(document)) {
                        return;
                    }
                    if (documentManager.getCachedPsiFile(document) == null) {
                        return;
                    }
                    if (document.getUserData(UPDATE_ON_COMMIT_ENGAGED) == null) {
                        document.putUserData(UPDATE_ON_COMMIT_ENGAGED, Boolean.TRUE);
                        PsiDocumentManagerBase.addRunOnCommit(
                            document,
                            () -> {
                                if (document.getUserData(UPDATE_ON_COMMIT_ENGAGED) != null) {
                                    updateChangesForDocumentOnCommit(document);
                                    document.putUserData(UPDATE_ON_COMMIT_ENGAGED, null);
                                }
                            }
                        );
                    }
                }
            },
            this
        );

        connection.subscribe(PsiDocumentTransactionListener.class, new PsiDocumentTransactionListener() {
            @Override
            public void transactionStarted(Document doc, PsiFile file) {
            }

            @Override
            @RequiredUIAccess
            public void transactionCompleted(Document document, PsiFile file) {
                updateChangesForDocumentOnCommit(document);
                // ensure we don't call updateChangesForDocumentOnCommit() twice which can lead to whole file re-highlight
                document.putUserData(UPDATE_ON_COMMIT_ENGAGED, null);
            }
        });
    }

    @Override
    public void dispose() {
    }

    @RequiredUIAccess
    private void updateChangesForDocumentOnCommit(Document document) {
        if (myProject.isDisposed()) {
            return;
        }
        Application application = myProject.getApplication();
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(myProject);

        // don't create PSI for files in other projects
        PsiFile psiFile = psiDocumentManager.getCachedPsiFile(document);
        if (psiFile != null) {
            synchronized (changedElements) {
                // The document has been changed, but psi hasn't.
                // We may still need to rehighlight the file if there were changes inside highlighted ranges.
                if (!UpdateHighlightersUtilImpl.isWhitespaceOptimizationAllowed(document) && !changedElements.containsKey(document)) {
                    storeChangedElement(psiFile, document, true);
                }
            }
        }

        Editor selectedEditor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
        PsiFile selectedPsiFile = selectedEditor == null ? null : psiDocumentManager.getCachedPsiFile(selectedEditor.getDocument());
        if (selectedPsiFile != null && !application.isUnitTestMode()) {
            application.invokeLater(
                () -> {
                    if (!selectedEditor.isDisposed() && selectedEditor.getMarkupModel() instanceof EditorMarkupModel markupModel) {
                        ErrorStripeUpdateManager.getInstance(myProject).setOrRefreshErrorStripeRenderer(markupModel, selectedPsiFile);
                    }
                },
                application.getModalityStateForComponent(selectedEditor.getComponent()),
                myProject.getDisposed()
            );
        }

        queueUpdateFileStatusQueue();
    }

    @Override
    public void childAdded(PsiTreeChangeEvent event) {
        queueElement(event.getParent(), true, event);
    }

    @Override
    public void childRemoved(PsiTreeChangeEvent event) {
        queueElement(event.getParent(), true, event);
    }

    @Override
    public void childReplaced(PsiTreeChangeEvent event) {
        queueElement(event.getNewChild(), typesEqual(event.getNewChild(), event.getOldChild()), event);
    }

    private static boolean typesEqual(PsiElement newChild, PsiElement oldChild) {
        return newChild != null && oldChild != null && newChild.getClass() == oldChild.getClass();
    }

    @Override
    public void childrenChanged(PsiTreeChangeEvent event) {
        if (((PsiTreeChangeEventImpl)event).isGenericChange()) {
            return;
        }
        queueElement(event.getParent(), true, event);
    }

    @Override
    public void beforeChildMovement(PsiTreeChangeEvent event) {
        queueElement(event.getOldParent(), true, event);
        queueElement(event.getNewParent(), true, event);
    }

    @Override
    public void beforeChildrenChange(PsiTreeChangeEvent event) {
        // this event sent always before every PSI change, even not significant one (like after quick typing/backspacing char)
        // mark file dirty just in case
        PsiFile psiFile = event.getFile();
        if (psiFile != null) {
            myFileStatusMap.markFileScopeDirtyDefensively(psiFile, event);
        }
    }

    @Override
    public void propertyChanged(PsiTreeChangeEvent event) {
        String propertyName = event.getPropertyName();
        if (!propertyName.equals(PsiTreeChangeEvent.PROP_WRITABLE)) {
            Object oldValue = event.getOldValue();
            if (oldValue instanceof VirtualFile virtualFile && shouldBeIgnored(virtualFile)) {
                // ignore workspace.xml
                return;
            }
            myFileStatusMap.markAllFilesDirty(event);
        }
    }

    private void queueElement(PsiElement child, boolean whitespaceOptimizationAllowed, PsiTreeChangeEvent event) {
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

        PsiDocumentManagerBase pdm = (PsiDocumentManagerBase)PsiDocumentManager.getInstance(myProject);
        Document document = pdm.getCachedDocument(file);
        if (document != null) {
            if (pdm.getSynchronizer().getTransaction(document) == null) {
                // content reload, language level change or some other big change
                myFileStatusMap.markAllFilesDirty(child);
                return;
            }

            storeChangedElement(child, document, whitespaceOptimizationAllowed);
        }
    }

    private void storeChangedElement(PsiElement child, Document document, boolean whitespaceOptimizationAllowed) {
        synchronized (changedElements) {
            List<Change> toUpdate = changedElements.computeIfAbsent(document, __ -> new SmartList<>());
            toUpdate.add(new Change(child, whitespaceOptimizationAllowed));
        }
    }

    private void queueUpdateFileStatusQueue() {
        synchronized (myUpdateFileStatusAlarm) {
            myUpdateFileStatusAlarm.cancelRequest(this);
            myUpdateFileStatusAlarm.addRequest(this, 0);
        }
    }

    // handle queued elements on a background thread under a read action
    @Override
    public void run() {
        if (myProject.isDisposed()) {
            return;
        }
        myProject.getApplication().runReadAction((Runnable)() -> {
            // assume changedElements won't change under read action
            boolean needRestart = handleChangedElements();
            boolean hasUncommittedDocuments = PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments();
            // when hasUncommittedDocuments=true, daemon will restart again on commit anyway
            if (needRestart && !hasUncommittedDocuments) {
                DaemonCodeAnalyzerInternal.getInstanceEx(myProject).stopProcess(true, "flushUpdateFileStatusQueue");
            }
        });
    }

    // return true if myFileStatusMap has changed and we need restart
    @RequiredReadAction
    private boolean handleChangedElements() {
        List<Map.Entry<Document, List<Change>>> entries;
        synchronized (changedElements) {
            entries = new ArrayList<>(changedElements.entrySet());
            changedElements.clear();
        }
        boolean changed = false;
        for (Map.Entry<Document, List<Change>> entry : entries) {
            Document document = entry.getKey();
            for (Change change : entry.getValue()) {
                updateByChange(document, change.psiElement(), change.whiteSpaceOptimizationAllowed());
                changed = true;
            }
        }
        return changed;
    }

    @RequiredReadAction
    private void updateByChange(Document document, PsiElement child, boolean whitespaceOptimizationAllowed) {
        if (myProject.isDisposed()) {
            return;
        }
        PsiFile file;
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

            PsiElement scope = getChangeHighlightingScope(element);
            if (scope != null) {
                myFileStatusMap.markFileScopeDirty(document, scope.getTextRange(), fileLength, "Scope: " + scope);
                return;
            }

            element = element.getParent();
        }
    }

    private boolean shouldBeIgnored(VirtualFile virtualFile) {
        return ProjectCoreUtil.isProjectOrWorkspaceFile(virtualFile) || ProjectRootManager.getInstance(myProject)
            .getFileIndex()
            .isExcluded(virtualFile);
    }

    @RequiredReadAction
    private static @Nullable PsiElement getChangeHighlightingScope(PsiElement element) {
        for (ChangeLocalityDetector detector : element.getProject()
            .getApplication()
            .getExtensionPoint(ChangeLocalityDetector.class)
            .getExtensionList()) {
            PsiElement scope = detector.getChangeHighlightingDirtyScopeFor(element);
            if (scope != null) {
                return scope;
            }
        }

        return null;
    }
}
