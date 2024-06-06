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
import consulo.application.ApplicationManager;
import consulo.application.CommonBundle;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.*;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.FileEditorStateLevel;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.util.lang.ObjectUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.internal.ExternalChangeAction;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.internal.ProjectEx;
import consulo.ui.ex.action.ActionsBundle;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.undoRedo.*;
import consulo.undoRedo.event.CommandEvent;
import consulo.undoRedo.event.CommandListener;
import consulo.util.lang.EmptyRunnable;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.*;

public class UndoManagerImpl implements UndoManager, Disposable {
  private static final Logger LOG = Logger.getInstance(UndoManagerImpl.class);

  private static final int COMMANDS_TO_KEEP_LIVE_QUEUES = 100;
  private static final int COMMAND_TO_RUN_COMPACT = 20;
  private static final int FREE_QUEUES_LIMIT = 30;

  @Nullable
  private final ProjectEx myProject;
  private final CommandProcessor myCommandProcessor;

  private List<? extends UndoProvider> myUndoProviders;
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
    return ApplicationManager.getApplication().hasWriteAction(ExternalChangeAction.class);
  }

  public static int getGlobalUndoLimit() {
    return Registry.intValue("undo.globalUndoLimit");
  }

  public static int getDocumentUndoLimit() {
    return Registry.intValue("undo.documentUndoLimit");
  }

  public UndoManagerImpl(CommandProcessor commandProcessor) {
    this(null, commandProcessor);
  }

  @Inject
  public UndoManagerImpl(@Nullable ProjectEx project, CommandProcessor commandProcessor) {
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
    myEditorProvider = new FocusBasedCurrentEditorProvider();
    myCommandProcessor.addCommandListener(new CommandListener() {
      private boolean myStarted;

      @Override
      public void commandStarted(CommandEvent event) {
        if (myProject != null && myProject.isDisposed()) return;
        onCommandStarted(event.getProject(), event.getUndoConfirmationPolicy(), event.shouldRecordActionForOriginalDocument());
      }

      @Override
      public void commandFinished(CommandEvent event) {
        if (myProject != null && myProject.isDisposed()) return;
        onCommandFinished(event.getProject(), event.getCommandName(), event.getCommandGroupId());
      }

      @Override
      public void undoTransparentActionStarted() {
        if (myProject != null && myProject.isDisposed()) return;
        if (!isInsideCommand()) {
          myStarted = true;
          onCommandStarted(myProject, UndoConfirmationPolicy.DEFAULT, true);
        }
      }

      @Override
      public void undoTransparentActionFinished() {
        if (myProject != null && myProject.isDisposed()) return;
        if (myStarted) {
          myStarted = false;
          onCommandFinished(myProject, "", null);
        }
      }
    }, this);

    if (myProject == null) {
      myUndoProviders = Application.get().getExtensionPoint(ApplicationUndoProvider.class).getExtensionList();
    } else {
      myUndoProviders = myProject.getExtensionPoint(ProjectUndoProvider.class).getExtensionList();
    }

    for (UndoProvider undoProvider : myUndoProviders) {
      if (undoProvider instanceof Disposable) {
        Disposer.register(this, (Disposable)undoProvider);
      }
    }
  }

  public boolean isActive() {
    return Comparing.equal(myProject, myCurrentActionProject);
  }

  private boolean isInsideCommand() {
    return myCommandLevel > 0;
  }

  private void onCommandStarted(final Project project, UndoConfirmationPolicy undoConfirmationPolicy, boolean recordOriginalReference) {
    if (myCommandLevel == 0) {
      for (UndoProvider undoProvider : myUndoProviders) {
        undoProvider.commandStarted(project);
      }
      myCurrentActionProject = project;
    }

    commandStarted(undoConfirmationPolicy, myProject == project && recordOriginalReference);

    LOG.assertTrue(myCommandLevel == 0 || !(myCurrentActionProject instanceof DummyProject));
  }

  private void onCommandFinished(final Project project, final String commandName, final Object commandGroupId) {
    commandFinished(commandName, commandGroupId);
    if (myCommandLevel == 0) {
      for (UndoProvider undoProvider : myUndoProviders) {
        undoProvider.commandFinished(project);
      }
      myCurrentActionProject = DummyProject.getInstance();
    }
    LOG.assertTrue(myCommandLevel == 0 || !(myCurrentActionProject instanceof DummyProject));
  }

  private void commandStarted(UndoConfirmationPolicy undoConfirmationPolicy, boolean recordOriginalReference) {
    if (myCommandLevel == 0) {
      myCurrentMerger = new CommandMerger(this, CommandProcessor.getInstance().isUndoTransparentActionInProgress());

      if (recordOriginalReference && myProject != null) {
        Editor editor = null;
        final Application application = ApplicationManager.getApplication();
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

  private void commandFinished(String commandName, Object groupId) {
    if (myCommandLevel == 0) return; // possible if command listener was added within command
    myCommandLevel--;
    if (myCommandLevel > 0) return;

    if (myProject != null && myCurrentMerger.hasActions() && !myCurrentMerger.isTransparent() && myCurrentMerger.isPhysical()) {
      addFocusedDocumentAsAffected();
    }
    myOriginatorReference = null;

    myCurrentMerger.setAfterState(getCurrentState());
    myMerger.commandFinished(commandName, groupId, myCurrentMerger);

    disposeCurrentMerger();
  }

  private void addFocusedDocumentAsAffected() {
    if (myOriginatorReference == null || myCurrentMerger.hasChangesOf(myOriginatorReference, true)) return;

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
  public void nonundoableActionPerformed(@Nonnull final DocumentReference ref, final boolean isGlobal) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (myProject != null && myProject.isDisposed()) return;
    undoableActionPerformed(new NonUndoableAction(ref, isGlobal));
  }

  @Override
  public void undoableActionPerformed(@Nonnull UndoableAction action) {
    ApplicationManager.getApplication().assertIsWriteThread();
    if (myProject != null && myProject.isDisposed()) return;

    if (myCurrentOperationState != OperationState.NONE) return;

    if (myCommandLevel == 0) {
      LOG.assertTrue(action instanceof NonUndoableAction, "Undoable actions allowed inside commands only (see consulo.ide.impl.idea.openapi.command.CommandProcessor.executeCommand())");
      commandStarted(UndoConfirmationPolicy.DEFAULT, false);
      myCurrentMerger.addAction(action);
      commandFinished("", null);
      return;
    }

    if (isRefresh()) myOriginatorReference = null;

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
      if (file != null && !file.isValid()) continue;

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
  public void invalidateActionsFor(@Nonnull DocumentReference ref) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myMerger.invalidateActionsFor(ref);
    if (myCurrentMerger != null) myCurrentMerger.invalidateActionsFor(ref);
    myUndoStacksHolder.invalidateActionsFor(ref);
    myRedoStacksHolder.invalidateActionsFor(ref);
  }

  @Override
  public void undo(@Nullable Object editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    LOG.assertTrue(isUndoAvailable(editor));
    undoOrRedo(editor, true);
  }

  @Override
  public void redo(@Nullable Object editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    LOG.assertTrue(isRedoAvailable(editor));
    undoOrRedo(editor, false);
  }

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

    String name = getUndoOrRedoActionNameAndDescription((FileEditor)editor, isUndoInProgress()).second;
    CommandProcessor.getInstance().executeCommand(myProject, executeUndoOrRedoAction, name, null, myMerger.getUndoConfirmationPolicy());
    if (exception[0] != null) throw exception[0];
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
  public boolean isUndoAvailable(@Nullable Object editor) {
    return isUndoOrRedoAvailable(editor, true);
  }

  @Override
  public boolean isRedoAvailable(@Nullable Object editor) {
    return isUndoOrRedoAvailable(editor, false);
  }

  boolean isUndoOrRedoAvailable(@Nullable Object editor, boolean undo) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    Collection<DocumentReference> refs = getDocRefs(editor);
    return refs != null && isUndoOrRedoAvailable(refs, undo);
  }

  boolean isUndoOrRedoAvailable(@Nonnull DocumentReference ref) {
    Set<DocumentReference> refs = Collections.singleton(ref);
    return isUndoOrRedoAvailable(refs, true) || isUndoOrRedoAvailable(refs, false);
  }

  private boolean isUndoOrRedoAvailable(@Nonnull Collection<DocumentReference> refs, boolean isUndo) {
    if (isUndo && myMerger.isUndoAvailable(refs)) return true;
    UndoRedoStacksHolder stackHolder = getStackHolder(isUndo);
    return stackHolder.canBeUndoneOrRedone(refs);
  }

  private static Collection<DocumentReference> getDocRefs(@Nullable Object editor) {
    if (editor instanceof TextEditor && ((TextEditor)editor).getEditor().isViewer()) {
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

    if (editor instanceof DocumentReferenceProvider) {
      result.addAll(((DocumentReferenceProvider)editor).getDocumentReferences());
      return result;
    }

    Document[] documents = TextEditorProvider.getDocuments(editor);
    if (documents != null) {
      for (Document each : documents) {
        Document original = getOriginal(each);
        // KirillK : in AnAction.update we may have an editor with an invalid file
        VirtualFile f = FileDocumentManager.getInstance().getFile(each);
        if (f != null && !f.isValid()) continue;
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
  public Pair<String, String> getUndoActionNameAndDescription(Object editor) {
    return getUndoOrRedoActionNameAndDescription(editor, true);
  }

  @Nonnull
  @Override
  public Pair<String, String> getRedoActionNameAndDescription(Object editor) {
    return getUndoOrRedoActionNameAndDescription(editor, false);
  }

  @Nonnull
  private Pair<String, String> getUndoOrRedoActionNameAndDescription(Object editor, boolean undo) {
    String desc = isUndoOrRedoAvailable(editor, undo) ? doFormatAvailableUndoRedoAction(editor, undo) : null;
    if (desc == null) desc = "";
    String shortActionName = StringUtil.first(desc, 30, true);

    if (desc.isEmpty()) {
      desc = undo ? ActionsBundle.message("action.undo.description.empty") : ActionsBundle.message("action.redo.description.empty");
    }

    return Pair.create((undo ? ActionsBundle.message("action.undo.text", shortActionName) : ActionsBundle.message("action.redo.text", shortActionName)).trim(),
                       (undo ? ActionsBundle.message("action.undo.description", desc) : ActionsBundle.message("action.redo.description", desc)).trim());
  }

  @Nullable
  private String doFormatAvailableUndoRedoAction(Object editor, boolean isUndo) {
    Collection<DocumentReference> refs = getDocRefs(editor);
    if (refs == null) return null;
    if (isUndo && myMerger.isUndoAvailable(refs)) return myMerger.getCommandName();
    return getStackHolder(isUndo).getLastAction(refs).getCommandName();
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

    if (refs.size() <= FREE_QUEUES_LIMIT) return;

    DocumentReference[] backSorted = refs.toArray(new DocumentReference[refs.size()]);
    Arrays.sort(backSorted, Comparator.comparingInt(this::getLastCommandTimestamp));

    for (int i = 0; i < backSorted.length - FREE_QUEUES_LIMIT; i++) {
      DocumentReference each = backSorted[i];
      if (getLastCommandTimestamp(each) + COMMANDS_TO_KEEP_LIVE_QUEUES > myCommandTimestamp) break;
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
  public void dropHistoryInTests() {
    flushMergers();
    LOG.assertTrue(myCommandLevel == 0, myCommandLevel);

    myUndoStacksHolder.clearAllStacksInTests();
    myRedoStacksHolder.clearAllStacksInTests();
  }

  @TestOnly
  private void flushMergers() {
    assert myProject == null || !myProject.isDisposed();
    // Run dummy command in order to flush all mergers...
    CommandProcessor.getInstance().executeCommand(myProject, EmptyRunnable.getInstance(), CommonBundle.message("drop.undo.history.command.name"), null);
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
