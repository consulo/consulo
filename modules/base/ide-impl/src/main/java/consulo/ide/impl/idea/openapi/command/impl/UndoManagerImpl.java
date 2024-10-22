/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.command.impl;

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.document.*;
import consulo.fileEditor.*;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.internal.ExternalChangeAction;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.ActionLocalize;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.undoRedo.*;
import consulo.undoRedo.event.CommandEvent;
import consulo.undoRedo.event.CommandListener;
import consulo.util.lang.*;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jetbrains.annotations.TestOnly;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

public abstract class UndoManagerImpl implements UndoManager, Disposable {
    private static final Logger LOG = Logger.getInstance(UndoManagerImpl.class);

    private static final int COMMANDS_TO_KEEP_LIVE_QUEUES = 100;
    private static final int COMMAND_TO_RUN_COMPACT = 20;
    private static final int FREE_QUEUES_LIMIT = 30;

    @Nonnull
    protected final Application myApplication;
    @Nullable
    protected final Project myProject;
    private final CommandProcessor myCommandProcessor;

    private CurrentEditorProvider myEditorProvider;

    private final UndoRedoStacksHolder myUndoStacksHolder = new UndoRedoStacksHolder(true);
    private final UndoRedoStacksHolder myRedoStacksHolder = new UndoRedoStacksHolder(false);

    private final CommandMerger myMerger;

    private CommandMerger myCurrentMerger;
    private Project myCurrentActionProject = DummyProject.getInstance();

    private int myCommandTimestamp = 1;

    private int myCommandLevel;

    private enum OperationState {
        NONE,
        UNDO,
        REDO
    }

    private OperationState myCurrentOperationState = OperationState.NONE;

    private DocumentReference myOriginatorReference;

    @RequiredWriteAction
    public static boolean isRefresh() {
        return Application.get().hasWriteAction(ExternalChangeAction.class);
    }

    public static int getGlobalUndoLimit() {
        return Registry.intValue("undo.globalUndoLimit");
    }

    public static int getDocumentUndoLimit() {
        return Registry.intValue("undo.documentUndoLimit");
    }

    @Inject
    public UndoManagerImpl(@Nonnull Application application, @Nullable Project project, CommandProcessor commandProcessor) {
        myApplication = application;
        myProject = project;
        myCommandProcessor = commandProcessor;

        if (myProject == null || !myProject.isDefault()) {
            runStartupActivity();
        }

        myMerger = new CommandMerger(this);
    }

    @Nullable
    public Project getProject() {
        return myProject;
    }

    @Override
    public void dispose() {
    }

    private void runStartupActivity() {
        myEditorProvider = CurrentEditorProvider.getInstance();
        myCommandProcessor.addCommandListener(
            new CommandListener() {
                private boolean myStarted;

                @Override
                public void commandStarted(CommandEvent event) {
                    if (myProject != null && myProject.isDisposed()) {
                        return;
                    }
                    onCommandStarted(event.getProject(), event.getUndoConfirmationPolicy(), event.shouldRecordActionForOriginalDocument());
                }

                @Override
                public void commandFinished(CommandEvent event) {
                    if (myProject != null && myProject.isDisposed()) {
                        return;
                    }
                    onCommandFinished(event.getProject(), event.getCommandNameValue(), event.getCommandGroupId());
                }

                @Override
                public void undoTransparentActionStarted() {
                    if (myProject != null && myProject.isDisposed()) {
                        return;
                    }
                    if (!isInsideCommand()) {
                        myStarted = true;
                        onCommandStarted(myProject, UndoConfirmationPolicy.DEFAULT, true);
                    }
                }

                @Override
                public void undoTransparentActionFinished() {
                    if (myProject != null && myProject.isDisposed()) {
                        return;
                    }
                    if (myStarted) {
                        myStarted = false;
                        onCommandFinished(myProject, LocalizeValue.empty(), null);
                    }
                }
            },
            this
        );
    }

    protected abstract void forEachProvider(@Nonnull Consumer<? super UndoProvider> consumer);

    public boolean isActive() {
        return Comparing.equal(myProject, myCurrentActionProject);
    }

    private boolean isInsideCommand() {
        return myCommandLevel > 0;
    }

    private void onCommandStarted(final Project project, UndoConfirmationPolicy undoConfirmationPolicy, boolean recordOriginalReference) {
        if (myCommandLevel == 0) {
            forEachProvider(undoProvider -> undoProvider.commandStarted(project));
            myCurrentActionProject = project;
        }

        commandStarted(undoConfirmationPolicy, myProject == project && recordOriginalReference);

        LOG.assertTrue(myCommandLevel == 0 || !(myCurrentActionProject instanceof DummyProject));
    }

    private void onCommandFinished(final Project project, @Nonnull LocalizeValue commandName, Object commandGroupId) {
        commandFinished(commandName, commandGroupId);
        if (myCommandLevel == 0) {
            forEachProvider(undoProvider -> undoProvider.commandFinished(project));
            myCurrentActionProject = DummyProject.getInstance();
        }
        LOG.assertTrue(myCommandLevel == 0 || !(myCurrentActionProject instanceof DummyProject));
    }

    private void commandStarted(UndoConfirmationPolicy undoConfirmationPolicy, boolean recordOriginalReference) {
        if (myCommandLevel == 0) {
            myCurrentMerger = new CommandMerger(this, CommandProcessor.getInstance().isUndoTransparentActionInProgress());

            if (recordOriginalReference && myProject != null) {
                Editor editor = null;
                final Application application = myProject.getApplication();
                if (application.isUnitTestMode() || application.isHeadlessEnvironment()) {
                    editor = DataManager.getInstance().getDataContext().getData(Editor.KEY);
                }
                else {
                    Component component = WindowManagerEx.getInstanceEx().getFocusedComponent(myProject);
                    if (component != null) {
                        editor = DataManager.getInstance().getDataContext(component).getData(Editor.KEY);
                    }
                }

                if (editor != null) {
                    Document document = editor.getDocument();
                    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
                    if (file != null && file.isValid()) {
                        myOriginatorReference = DocumentReferenceManager.getInstance().create(file);
                    }
                }
            }
        }
        LOG.assertTrue(myCurrentMerger != null, String.valueOf(myCommandLevel));
        myCurrentMerger.setBeforeState(getCurrentState());
        myCurrentMerger.mergeUndoConfirmationPolicy(undoConfirmationPolicy);

        myCommandLevel++;
    }

    private void commandFinished(@Nonnull LocalizeValue commandName, Object groupId) {
        if (myCommandLevel == 0) {
            return; // possible if command listener was added within command
        }
        myCommandLevel--;
        if (myCommandLevel > 0) {
            return;
        }

        if (myProject != null && myCurrentMerger.hasActions() && !myCurrentMerger.isTransparent() && myCurrentMerger.isPhysical()) {
            addFocusedDocumentAsAffected();
        }
        myOriginatorReference = null;

        myCurrentMerger.setAfterState(getCurrentState());
        myMerger.commandFinished(commandName.get(), groupId, myCurrentMerger);

        disposeCurrentMerger();
    }

    private void addFocusedDocumentAsAffected() {
        if (myOriginatorReference == null || myCurrentMerger.hasChangesOf(myOriginatorReference, true)) {
            return;
        }

        final DocumentReference[] refs = {myOriginatorReference};
        myCurrentMerger.addAction(new MentionOnlyUndoableAction(refs));
    }

    private EditorAndState getCurrentState() {
        FileEditor editor = myEditorProvider.getCurrentEditor();
        if (editor == null) {
            return null;
        }
        if (!editor.isValid()) {
            return null;
        }
        return new EditorAndState(editor, editor.getState(FileEditorStateLevel.UNDO));
    }

    private void disposeCurrentMerger() {
        LOG.assertTrue(myCommandLevel == 0);
        if (myCurrentMerger != null) {
            myCurrentMerger = null;
        }
    }

    @Override
    @RequiredUIAccess
    @RequiredWriteAction
    public void nonundoableActionPerformed(@Nonnull final DocumentReference ref, final boolean isGlobal) {
        Application.get().assertIsDispatchThread();
        if (myProject != null && myProject.isDisposed()) {
            return;
        }
        undoableActionPerformed(new NonUndoableAction(ref, isGlobal));
    }

    @Override
    @RequiredUIAccess
    @RequiredWriteAction
    public void undoableActionPerformed(@Nonnull UndoableAction action) {
        Application.get().assertIsWriteThread();
        if (myProject != null && myProject.isDisposed()) {
            return;
        }

        if (myCurrentOperationState != OperationState.NONE) {
            return;
        }

        if (myCommandLevel == 0) {
            LOG.assertTrue(
                action instanceof NonUndoableAction,
                "Undoable actions allowed inside commands only (see consulo.ide.impl.idea.openapi.command.CommandProcessor.executeCommand())"
            );
            commandStarted(UndoConfirmationPolicy.DEFAULT, false);
            myCurrentMerger.addAction(action);
            commandFinished(LocalizeValue.empty(), null);
            return;
        }

        if (isRefresh()) {
            myOriginatorReference = null;
        }

        myCurrentMerger.addAction(action);
    }

    public void markCurrentCommandAsGlobal() {
        myCurrentMerger.markAsGlobal();
    }

    void addAffectedDocuments(@Nonnull Document... docs) {
        if (!isInsideCommand()) {
            LOG.error("Must be called inside command");
            return;
        }
        List<DocumentReference> refs = new ArrayList<>(docs.length);
        for (Document each : docs) {
            // is document's file still valid
            VirtualFile file = FileDocumentManager.getInstance().getFile(each);
            if (file != null && !file.isValid()) {
                continue;
            }

            refs.add(DocumentReferenceManager.getInstance().create(each));
        }
        myCurrentMerger.addAdditionalAffectedDocuments(refs);
    }

    public void addAffectedFiles(@Nonnull VirtualFile... files) {
        if (!isInsideCommand()) {
            LOG.error("Must be called inside command");
            return;
        }
        List<DocumentReference> refs = new ArrayList<>(files.length);
        for (VirtualFile each : files) {
            refs.add(DocumentReferenceManager.getInstance().create(each));
        }
        myCurrentMerger.addAdditionalAffectedDocuments(refs);
    }

    @Override
    public boolean canMergeGroup(Object groupId, Object lastGroupId) {
        return CommandMerger.canMergeGroup(groupId, lastGroupId);
    }

    @Override
    @RequiredUIAccess
    public void invalidateActionsFor(@Nonnull DocumentReference ref) {
        Application.get().assertIsDispatchThread();
        myMerger.invalidateActionsFor(ref);
        if (myCurrentMerger != null) {
            myCurrentMerger.invalidateActionsFor(ref);
        }
        myUndoStacksHolder.invalidateActionsFor(ref);
        myRedoStacksHolder.invalidateActionsFor(ref);
    }

    @Override
    @RequiredUIAccess
    public void undo(@Nullable Object editor) {
        Application.get().assertIsDispatchThread();
        LOG.assertTrue(isUndoAvailable(editor));
        undoOrRedo(editor, true);
    }

    @Override
    @RequiredUIAccess
    public void redo(@Nullable Object editor) {
        Application.get().assertIsDispatchThread();
        LOG.assertTrue(isRedoAvailable(editor));
        undoOrRedo(editor, false);
    }

    @RequiredUIAccess
    private void undoOrRedo(final Object editor, final boolean isUndo) {
        myCurrentOperationState = isUndo ? OperationState.UNDO : OperationState.REDO;

        final RuntimeException[] exception = new RuntimeException[1];
        Runnable executeUndoOrRedoAction = () -> {
            try {
                if (myProject != null) {
                    PsiDocumentManager.getInstance(myProject).commitAllDocuments();
                }
                CopyPasteManager.getInstance().stopKillRings();
                myMerger.undoOrRedo((FileEditor)editor, isUndo);
            }
            catch (RuntimeException ex) {
                exception[0] = ex;
            }
            finally {
                myCurrentOperationState = OperationState.NONE;
            }
        };

        LocalizeValue name = getUndoOrRedoActionNameAndDescription(editor, isUndoInProgress()).second;
        CommandProcessor.getInstance().newCommand(executeUndoOrRedoAction)
            .withProject(myProject)
            .withName(name)
            .withUndoConfirmationPolicy(myMerger.getUndoConfirmationPolicy())
            .execute();
        if (exception[0] != null) {
            throw exception[0];
        }
    }

    @Override
    public boolean isUndoInProgress() {
        return myCurrentOperationState == OperationState.UNDO;
    }

    @Override
    public boolean isRedoInProgress() {
        return myCurrentOperationState == OperationState.REDO;
    }

    @Override
    @RequiredUIAccess
    public boolean isUndoAvailable(@Nullable Object editor) {
        return isUndoOrRedoAvailable(editor, true);
    }

    @Override
    @RequiredUIAccess
    public boolean isRedoAvailable(@Nullable Object editor) {
        return isUndoOrRedoAvailable(editor, false);
    }

    @RequiredUIAccess
    boolean isUndoOrRedoAvailable(@Nullable Object editor, boolean undo) {
        Application.get().assertIsDispatchThread();

        Collection<DocumentReference> refs = getDocRefs(editor);
        return refs != null && isUndoOrRedoAvailable(refs, undo);
    }

    boolean isUndoOrRedoAvailable(@Nonnull DocumentReference ref) {
        Set<DocumentReference> refs = Collections.singleton(ref);
        return isUndoOrRedoAvailable(refs, true) || isUndoOrRedoAvailable(refs, false);
    }

    private boolean isUndoOrRedoAvailable(@Nonnull Collection<DocumentReference> refs, boolean isUndo) {
        if (isUndo && myMerger.isUndoAvailable(refs)) {
            return true;
        }
        UndoRedoStacksHolder stackHolder = getStackHolder(isUndo);
        return stackHolder.canBeUndoneOrRedone(refs);
    }

    private static Collection<DocumentReference> getDocRefs(@Nullable Object editor) {
        if (editor instanceof TextEditor textEditor && textEditor.getEditor().isViewer()) {
            return null;
        }
        if (editor == null) {
            return Collections.emptyList();
        }
        return getDocumentReferences((FileEditor)editor);
    }

    @Nonnull
    static Set<DocumentReference> getDocumentReferences(@Nonnull FileEditor editor) {
        Set<DocumentReference> result = new HashSet<>();

        if (editor instanceof DocumentReferenceProvider documentReferenceProvider) {
            result.addAll(documentReferenceProvider.getDocumentReferences());
            return result;
        }

        Document[] documents = TextEditorProvider.getDocuments(editor);
        if (documents != null) {
            for (Document each : documents) {
                Document original = getOriginal(each);
                // KirillK : in AnAction.update we may have an editor with an invalid file
                VirtualFile f = FileDocumentManager.getInstance().getFile(each);
                if (f != null && !f.isValid()) {
                    continue;
                }
                result.add(DocumentReferenceManager.getInstance().create(original));
            }
        }
        return result;
    }

    @Nonnull
    private UndoRedoStacksHolder getStackHolder(boolean isUndo) {
        return isUndo ? myUndoStacksHolder : myRedoStacksHolder;
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public Couple<String> getUndoActionNameAndDescription(Object editor) {
        Couple<LocalizeValue> nameAndDescription = getUndoOrRedoActionNameAndDescription(editor, true);
        return Couple.of(nameAndDescription.getFirst().get(), nameAndDescription.getSecond().get());
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public Couple<String> getRedoActionNameAndDescription(Object editor) {
        Couple<LocalizeValue> nameAndDescription = getUndoOrRedoActionNameAndDescription(editor, false);
        return Couple.of(nameAndDescription.getFirst().get(), nameAndDescription.getSecond().get());
    }

    @Nonnull
    @RequiredUIAccess
    private Couple<LocalizeValue> getUndoOrRedoActionNameAndDescription(Object editor, boolean undo) {
        LocalizeValue desc = isUndoOrRedoAvailable(editor, undo) ? doFormatAvailableUndoRedoAction(editor, undo) : LocalizeValue.empty();
        LocalizeValue shortActionName = desc.map((localizeManager, value) -> StringUtil.first(value, 30, true));

        if (desc == LocalizeValue.empty()) {
            desc = undo
                ? ActionLocalize.actionUndoDescriptionEmpty()
                : ActionLocalize.actionRedoDescriptionEmpty();
        }

        return Couple.of(
            (undo ? ActionLocalize.actionUndoText(shortActionName) : ActionLocalize.actionRedoText(shortActionName)),
            (undo ? ActionLocalize.actionUndoDescription(desc) : ActionLocalize.actionRedoDescription(desc))
        );
    }

    @Nonnull
    private LocalizeValue doFormatAvailableUndoRedoAction(Object editor, boolean isUndo) {
        Collection<DocumentReference> refs = getDocRefs(editor);
        if (refs == null) {
            return LocalizeValue.empty();
        }
        if (isUndo && myMerger.isUndoAvailable(refs)) {
            return LocalizeValue.ofNullable(myMerger.getCommandName());
        }
        return LocalizeValue.ofNullable(getStackHolder(isUndo).getLastAction(refs).getCommandName());
    }

    @Nonnull
    UndoRedoStacksHolder getUndoStacksHolder() {
        return myUndoStacksHolder;
    }

    @Nonnull
    UndoRedoStacksHolder getRedoStacksHolder() {
        return myRedoStacksHolder;
    }

    int nextCommandTimestamp() {
        return ++myCommandTimestamp;
    }

    @Nonnull
    private static Document getOriginal(@Nonnull Document document) {
        Document result = document.getUserData(ORIGINAL_DOCUMENT);
        return result == null ? document : result;
    }

    static boolean isCopy(@Nonnull Document d) {
        return d.getUserData(ORIGINAL_DOCUMENT) != null;
    }

    protected void compact() {
        if (myCurrentOperationState == OperationState.NONE && myCommandTimestamp % COMMAND_TO_RUN_COMPACT == 0) {
            doCompact();
        }
    }

    private void doCompact() {
        Collection<DocumentReference> refs = collectReferencesWithoutMergers();

        Collection<DocumentReference> openDocs = new HashSet<>();
        for (DocumentReference each : refs) {
            VirtualFile file = each.getFile();
            if (file == null) {
                Document document = each.getDocument();
                if (document != null && EditorFactory.getInstance().getEditors(document, myProject).length > 0) {
                    openDocs.add(each);
                }
            }
            else {
                if (myProject != null && FileEditorManager.getInstance(myProject).isFileOpen(file)) {
                    openDocs.add(each);
                }
            }
        }
        refs.removeAll(openDocs);

        if (refs.size() <= FREE_QUEUES_LIMIT) {
            return;
        }

        DocumentReference[] backSorted = refs.toArray(new DocumentReference[refs.size()]);
        Arrays.sort(backSorted, Comparator.comparingInt(this::getLastCommandTimestamp));

        for (int i = 0; i < backSorted.length - FREE_QUEUES_LIMIT; i++) {
            DocumentReference each = backSorted[i];
            if (getLastCommandTimestamp(each) + COMMANDS_TO_KEEP_LIVE_QUEUES > myCommandTimestamp) {
                break;
            }
            clearUndoRedoQueue(each);
        }
    }

    private int getLastCommandTimestamp(@Nonnull DocumentReference ref) {
        return Math.max(myUndoStacksHolder.getLastCommandTimestamp(ref), myRedoStacksHolder.getLastCommandTimestamp(ref));
    }

    @Nonnull
    private Collection<DocumentReference> collectReferencesWithoutMergers() {
        Set<DocumentReference> result = new HashSet<>();
        myUndoStacksHolder.collectAllAffectedDocuments(result);
        myRedoStacksHolder.collectAllAffectedDocuments(result);
        return result;
    }

    private void clearUndoRedoQueue(@Nonnull DocumentReference docRef) {
        myMerger.flushCurrentCommand();
        disposeCurrentMerger();

        myUndoStacksHolder.clearStacks(false, Collections.singleton(docRef));
        myRedoStacksHolder.clearStacks(false, Collections.singleton(docRef));
    }

    @TestOnly
    public void setEditorProvider(@Nonnull CurrentEditorProvider p) {
        myEditorProvider = p;
    }

    @TestOnly
    @Nonnull
    public CurrentEditorProvider getEditorProvider() {
        return myEditorProvider;
    }

    @TestOnly
    @RequiredUIAccess
    public void dropHistoryInTests() {
        flushMergers();
        LOG.assertTrue(myCommandLevel == 0, myCommandLevel);

        myUndoStacksHolder.clearAllStacksInTests();
        myRedoStacksHolder.clearAllStacksInTests();
    }

    @TestOnly
    @RequiredUIAccess
    private void flushMergers() {
        assert myProject == null || !myProject.isDisposed();
        // Run dummy command in order to flush all mergers...
        CommandProcessor.getInstance().newCommand(EmptyRunnable.getInstance())
            .withProject(myProject)
            .withName(CommonLocalize.dropUndoHistoryCommandName())
            .execute();
    }

    @TestOnly
    public void flushCurrentCommandMerger() {
        myMerger.flushCurrentCommand();
    }

    @TestOnly
    public void clearUndoRedoQueueInTests(@Nonnull VirtualFile file) {
        clearUndoRedoQueue(DocumentReferenceManager.getInstance().create(file));
    }

    @TestOnly
    public void clearUndoRedoQueueInTests(@Nonnull Document document) {
        clearUndoRedoQueue(DocumentReferenceManager.getInstance().create(document));
    }

    @Override
    public String toString() {
        return "UndoManager for " + ObjectUtil.notNull(myProject, "application");
    }
}
