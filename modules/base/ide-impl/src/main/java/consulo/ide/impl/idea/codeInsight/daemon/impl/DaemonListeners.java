// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.PowerSaveModeListener;
import consulo.application.event.ApplicationListener;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.impl.internal.LaterInvocator;
import consulo.application.impl.internal.ModalityStateListener;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.event.*;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.event.EditorColorsListener;
import consulo.component.messagebus.MessageBus;
import consulo.component.messagebus.MessageBusConnection;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.DocCommandGroupId;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.codeInsight.documentation.DocumentationManager;
import consulo.ide.impl.idea.codeInsight.folding.impl.FoldingUtil;
import consulo.ide.impl.idea.codeInsight.hint.TooltipController;
import consulo.ide.impl.idea.ide.AppLifecycleListener;
import consulo.ide.impl.idea.ide.IdeTooltipManagerImpl;
import consulo.ide.impl.idea.ide.todo.TodoConfiguration;
import consulo.ide.impl.idea.ide.todo.TodoConfigurationListener;
import consulo.ide.impl.idea.openapi.editor.ex.EditorEventMulticasterEx;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.openapi.editor.impl.EditorMouseHoverPopupControl;
import consulo.ide.impl.idea.openapi.project.ProjectUtil;
import consulo.ide.impl.language.editor.rawHighlight.HighlightInfoImpl;
import consulo.ide.impl.psi.RefResolveService;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.DaemonListener;
import consulo.language.editor.Pass;
import consulo.language.editor.inspection.scheme.Profile;
import consulo.language.editor.inspection.scheme.event.ProfileChangeAdapter;
import consulo.language.editor.scratch.ScratchUtil;
import consulo.language.file.event.FileTypeEvent;
import consulo.language.file.event.FileTypeListener;
import consulo.language.impl.internal.psi.PsiManagerEx;
import consulo.language.psi.*;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.language.util.ModuleUtilCore;
import consulo.logging.Logger;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.module.extension.event.ModuleExtensionChangeListener;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.awt.UIUtil;
import consulo.undoRedo.ProjectUndoManager;
import consulo.undoRedo.UndoManager;
import consulo.undoRedo.event.CommandEvent;
import consulo.undoRedo.event.CommandListener;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderEx;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.change.VcsDirtyScopeManager;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.event.VFilePropertyChangeEvent;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@Singleton
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
@ServiceImpl
public final class DaemonListeners implements Disposable {
  private static final Logger LOG = Logger.getInstance(DaemonListeners.class);

  private final Project myProject;
  private final DaemonCodeAnalyzerImpl myDaemonCodeAnalyzer;

  private boolean myEscPressed;

  private volatile boolean cutOperationJustHappened;
  private final DaemonListener myDaemonEventPublisher;
  private List<Editor> myActiveEditors = Collections.emptyList();

  private static final Key<Boolean> DAEMON_INITIALIZED = Key.create("DAEMON_INITIALIZED");

  public static DaemonListeners getInstance(Project project) {
    return project.getComponent(DaemonListeners.class);
  }

  @Inject
  public DaemonListeners(@Nonnull Project project) {
    myProject = project;
    myDaemonCodeAnalyzer = (DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(myProject);

    boolean replaced = ((UserDataHolderEx)myProject).replace(DAEMON_INITIALIZED, null, Boolean.TRUE);
    if (!replaced) {
      LOG.error("Daemon listeners already initialized for the project " + myProject);
    }

    MessageBus messageBus = myProject.getMessageBus();
    myDaemonEventPublisher = messageBus.syncPublisher(DaemonListener.class);
    if (project.isDefault()) {
      return;
    }

    MessageBusConnection connection = messageBus.connect(this);
    connection.subscribe(AppLifecycleListener.class, new AppLifecycleListener() {
      @Override
      public void appClosing() {
        stopDaemon(false, "App closing");
      }
    });

    EditorFactory editorFactory = EditorFactory.getInstance();
    EditorEventMulticasterEx eventMulticaster = (EditorEventMulticasterEx)editorFactory.getEventMulticaster();
    eventMulticaster.addDocumentListener(new DocumentListener() {
      // clearing highlighters before changing document because change can damage editor highlighters drastically, so we'll clear more than necessary
      @Override
      public void beforeDocumentChange(@Nonnull final DocumentEvent e) {
        Document document = e.getDocument();
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        Project project = virtualFile == null ? null : ProjectUtil.guessProjectForFile(virtualFile);
        //no need to stop daemon if something happened in the console or in non-physical document
        if (worthBothering(document, project) && ApplicationManager.getApplication().isDispatchThread()) {
          stopDaemon(true, "Document change");
          UpdateHighlightersUtil.updateHighlightersByTyping(myProject, e);
        }
      }
    }, this);

    eventMulticaster.addCaretListener(new CaretListener() {
      @Override
      public void caretPositionChanged(@Nonnull CaretEvent e) {
        final Editor editor = e.getEditor();
        Application app = ApplicationManager.getApplication();
        if ((editor.isShowing() || app.isHeadlessEnvironment()) && worthBothering(editor.getDocument(), editor.getProject())) {

          if (!app.isUnitTestMode()) {
            ApplicationManager.getApplication().invokeLater(() -> {
              if ((editor.isShowing() || app.isHeadlessEnvironment()) && !myProject.isDisposed()) {
                IntentionsUI.getInstance(myProject).invalidate();
              }
            }, IdeaModalityState.current());
          }
        }
      }
    }, this);
    eventMulticaster.addEditorMouseMotionListener(new MyEditorMouseMotionListener(), this);
    eventMulticaster.addEditorMouseListener(new MyEditorMouseListener(TooltipController.getInstance()), this);

    connection.subscribe(EditorTrackerListener.class, new EditorTrackerListener() {
      @Override
      public void activeEditorsChanged(@Nonnull List<Editor> activeEditors) {
        if (myActiveEditors.equals(activeEditors)) {
          return;
        }

        myActiveEditors = activeEditors;
        // do not stop daemon if idea loses/gains focus
        DaemonListeners.this.stopDaemon(true, "Active editor change");
        if (ApplicationManager.getApplication().isDispatchThread() && LaterInvocator.isInModalContext()) {
          // editor appear in modal context, re-enable the daemon
          myDaemonCodeAnalyzer.setUpdateByTimerEnabled(true);
        }

        ErrorStripeUpdateManager errorStripeUpdateManager = ErrorStripeUpdateManager.getInstance(myProject);
        for (Editor editor : activeEditors) {
          errorStripeUpdateManager.repaintErrorStripePanel(editor);
        }
      }
    });

    editorFactory.addEditorFactoryListener(new EditorFactoryListener() {
      @Override
      public void editorCreated(@Nonnull EditorFactoryEvent event) {
        if (!Application.get().isSwingApplication()) {
          return;
        }

        Editor editor = event.getEditor();
        Document document = editor.getDocument();
        Project editorProject = editor.getProject();
        // worthBothering() checks for getCachedPsiFile, so call getPsiFile here
        PsiFile file = editorProject == null ? null : PsiDocumentManager.getInstance(editorProject).getPsiFile(document);
        boolean showing = editor.isShowing();
        boolean worthBothering = worthBothering(document, editorProject);
        if (!showing || !worthBothering) {
          LOG.debug("Not worth bothering about editor created for : " + file + " because editor isShowing(): " + showing + "; project is open and file is mine: " + worthBothering);
          return;
        }

        ErrorStripeUpdateManager.getInstance(myProject).repaintErrorStripePanel(editor);
      }

      @Override
      public void editorReleased(@Nonnull EditorFactoryEvent event) {
        // mem leak after closing last editor otherwise
        UIUtil.invokeLaterIfNeeded(() -> {
          IntentionsUI intentionUI = IntentionsUI.getInstance(project);
          if (intentionUI != null) {
            intentionUI.invalidate();
          }
        });
      }
    }, this);

    PsiChangeHandler changeHandler = new PsiChangeHandler(myProject, connection);
    Disposer.register(this, changeHandler);
    PsiManager.getInstance(myProject).addPsiTreeChangeListener(changeHandler, changeHandler);

    connection.subscribe(ModuleRootListener.class, new ModuleRootListener() {
      @Override
      public void rootsChanged(@Nonnull ModuleRootEvent event) {
        stopDaemonAndRestartAllFiles("Project roots changed");
      }
    });

    connection.subscribe(DumbModeListener.class, new DumbModeListener() {
      @Override
      public void enteredDumbMode() {
        stopDaemonAndRestartAllFiles("Dumb mode started");
      }

      @Override
      public void exitDumbMode() {
        stopDaemonAndRestartAllFiles("Dumb mode finished");
      }
    });

    connection.subscribe(PowerSaveModeListener.class, () -> stopDaemon(true, "Power save mode change"));
    connection.subscribe(EditorColorsListener.class, __ -> stopDaemonAndRestartAllFiles("Editor color scheme changed"));
    connection.subscribe(CommandListener.class, new MyCommandListener());
    connection.subscribe(ProfileChangeAdapter.class, new MyProfileChangeListener());

    project.getApplication().addApplicationListener(new MyApplicationListener(), this);

    connection.subscribe(TodoConfigurationListener.class, new MyTodoListener());

    connection.subscribe(AnActionListener.class, new MyAnActionListener());
    connection.subscribe(BulkFileListener.class, new BulkFileListener() {
      @Override
      public void after(@Nonnull List<? extends VFileEvent> events) {
        boolean isDaemonShouldBeStopped = false;
        for (VFileEvent event : events) {
          if (event instanceof VFilePropertyChangeEvent) {
            VFilePropertyChangeEvent e = (VFilePropertyChangeEvent)event;
            String propertyName = e.getPropertyName();
            if (VirtualFile.PROP_NAME.equals(propertyName)) {
              fileRenamed(e);
            }
            if (!isDaemonShouldBeStopped && !propertyName.equals(PsiTreeChangeEvent.PROP_WRITABLE)) {
              isDaemonShouldBeStopped = true;
            }
          }
        }

        if (isDaemonShouldBeStopped) {
          stopDaemon(true, "Virtual file property change");
        }
      }

      private void fileRenamed(@Nonnull VFilePropertyChangeEvent event) {
        stopDaemonAndRestartAllFiles("Virtual file name changed");
        VirtualFile virtualFile = event.getFile();
        PsiFile psiFile = !virtualFile.isValid() ? null : PsiManagerEx.getInstanceEx(myProject).getFileManager().getCachedPsiFile(virtualFile);
        if (psiFile == null || myDaemonCodeAnalyzer.isHighlightingAvailable(psiFile)) {
          return;
        }

        Document document = FileDocumentManager.getInstance().getCachedDocument(virtualFile);
        if (document == null) {
          return;
        }

        // highlight markers no more
        //todo clear all highlights regardless the pass id

        // Here color scheme required for TextEditorFields, as far as I understand this
        // code related to standard file editors, which always use Global color scheme,
        // thus we can pass null here.
        UpdateHighlightersUtil.setHighlightersToEditor(myProject, document, 0, document.getTextLength(), Collections.emptyList(), null, Pass.UPDATE_ALL);
      }
    });
    connection.subscribe(FileTypeListener.class, new FileTypeListener() {
      @Override
      public void fileTypesChanged(@Nonnull FileTypeEvent event) {
        IntentionsUI.getInstance(project).invalidate();
      }
    });

    eventMulticaster.addErrorStripeListener(e -> {
      RangeHighlighter highlighter = e.getHighlighter();
      if (!highlighter.isValid()) return;
      HighlightInfoImpl info = HighlightInfoImpl.fromRangeHighlighter(highlighter);
      if (info != null) {
        GotoNextErrorHandler.navigateToError(myProject, e.getEditor(), info);
      }
    }, this);

    LaterInvocator.addModalityStateListener(new ModalityStateListener() {
      @Override
      public void beforeModalityStateChanged(boolean __1, Object modalEntity) {
        // before showing dialog we are in non-modal context yet, and before closing dialog we are still in modal context
        boolean inModalContext = Registry.is("ide.perProjectModality") || LaterInvocator.isInModalContext();
        DaemonListeners.this.stopDaemon(inModalContext, "Modality change. Was modal: " + inModalContext);
        myDaemonCodeAnalyzer.setUpdateByTimerEnabled(inModalContext);
      }
    }, this);

    connection.subscribe(SeverityRegistrarChangeListener.class, () -> stopDaemonAndRestartAllFiles("Severities changed"));

    if (RefResolveService.ENABLED) {
      RefResolveService resolveService = RefResolveService.getInstance(project);
      resolveService.addListener(this, new RefResolveService.Listener() {
        @Override
        public void allFilesResolved() {
          stopDaemon(true, "RefResolveService is up to date");
        }
      });
    }
    connection.subscribe(ModuleExtensionChangeListener.class, (oldExtension, newExtension) -> {
      stopDaemonAndRestartAllFiles("extension changed: " + newExtension.getId());
    });
  }

  private boolean worthBothering(final Document document, Project project) {
    if (document == null) return true;
    if (project != null && project != myProject) return false;
    // cached is essential here since we do not want to create PSI file in alien project
    PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getCachedPsiFile(document);
    return psiFile != null && psiFile.isPhysical() && psiFile.getOriginalFile() == psiFile;
  }

  @Override
  public void dispose() {
    stopDaemonAndRestartAllFiles("Project closed");
    boolean replaced = ((UserDataHolderEx)myProject).replace(DAEMON_INITIALIZED, Boolean.TRUE, Boolean.FALSE);
    LOG.assertTrue(replaced, "Daemon listeners already disposed for the project " + myProject);
  }

  public static boolean canChangeFileSilently(@Nonnull PsiFileSystemItem file) {
    Project project = file.getProject();
    DaemonListeners listeners = getInstance(project);
    if (listeners == null) return true;

    if (listeners.cutOperationJustHappened) return false;
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return false;
    if (file instanceof PsiCodeFragment) return true;
    if (ScratchUtil.isScratch(virtualFile)) return listeners.canUndo(virtualFile);
    if (!ModuleUtilCore.projectContainsFile(project, virtualFile, false)) return false;
    Result vcs = listeners.vcsThinksItChanged(virtualFile);
    if (vcs == Result.CHANGED) return true;
    if (vcs == Result.UNCHANGED) return false;

    return listeners.canUndo(virtualFile);
  }

  private boolean canUndo(@Nonnull VirtualFile virtualFile) {
    FileEditor[] editors = FileEditorManager.getInstance(myProject).getEditors(virtualFile);
    if (editors.length == 0) {
      return false;
    }

    UndoManager undoManager = ProjectUndoManager.getInstance(myProject);
    for (FileEditor editor : editors) {
      if (undoManager.isUndoAvailable(editor)) {
        return true;
      }
    }
    return false;
  }

  private enum Result {
    CHANGED,
    UNCHANGED,
    NOT_SURE
  }

  @Nonnull
  private Result vcsThinksItChanged(@Nonnull VirtualFile virtualFile) {
    AbstractVcs activeVcs = ProjectLevelVcsManager.getInstance(myProject).getVcsFor(virtualFile);
    if (activeVcs == null) return Result.NOT_SURE;

    FilePath path = VcsUtil.getFilePath(virtualFile);
    boolean vcsIsThinking = !VcsDirtyScopeManager.getInstance(myProject).whatFilesDirty(Collections.singletonList(path)).isEmpty();
    if (vcsIsThinking) return Result.NOT_SURE; // do not modify file which is in the process of updating

    FileStatus status = FileStatusManager.getInstance(myProject).getStatus(virtualFile);
    if (status == FileStatus.UNKNOWN) return Result.NOT_SURE;
    return status == FileStatus.MODIFIED || status == FileStatus.ADDED ? Result.CHANGED : Result.UNCHANGED;
  }

  private class MyApplicationListener implements ApplicationListener {
    @Override
    public void beforeWriteActionStart(@Nonnull Object action) {
      if (!myDaemonCodeAnalyzer.isRunning()) return; // we'll restart in writeActionFinished()
      stopDaemon(true, "Write action start");
    }

    @Override
    public void writeActionFinished(@Nonnull Object action) {
      stopDaemon(true, "Write action finish");
    }
  }

  private class MyCommandListener implements CommandListener {
    private final String myCutActionName;

    private MyCommandListener() {
      myCutActionName = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_CUT).getTemplatePresentation().getText();
    }

    @Override
    public void commandStarted(@Nonnull CommandEvent event) {
      Document affectedDocument = extractDocumentFromCommand(event);
      if (!worthBothering(affectedDocument, event.getProject())) return;

      cutOperationJustHappened = myCutActionName.equals(event.getCommandName());
      if (!myDaemonCodeAnalyzer.isRunning()) return;
      if (LOG.isDebugEnabled()) {
        LOG.debug("cancelling code highlighting by command:" + event.getCommand());
      }
      stopDaemon(false, "Command start");
    }

    @Nullable
    private Document extractDocumentFromCommand(@Nonnull CommandEvent event) {
      Document affectedDocument = event.getDocument();
      if (affectedDocument != null) return affectedDocument;
      Object id = event.getCommandGroupId();

      if (id instanceof Document) {
        affectedDocument = (Document)id;
      }
      else if (id instanceof DocCommandGroupId) {
        affectedDocument = ((DocCommandGroupId)id).getDocument();
      }
      return affectedDocument;
    }

    @Override
    public void commandFinished(@Nonnull CommandEvent event) {
      Document affectedDocument = extractDocumentFromCommand(event);
      if (!worthBothering(affectedDocument, event.getProject())) return;

      if (myEscPressed) {
        myEscPressed = false;
        if (affectedDocument != null) {
          // prevent Esc key to leave the document in the not-highlighted state
          if (!myDaemonCodeAnalyzer.getFileStatusMap().allDirtyScopesAreNull(affectedDocument)) {
            stopDaemon(true, "Command finish");
          }
        }
      }
      else if (!myDaemonCodeAnalyzer.isRunning()) {
        stopDaemon(true, "Command finish");
      }
    }
  }

  private class MyTodoListener implements TodoConfigurationListener {
    @Override
    public void propertyChanged(String propertyName, Object oldValue, Object newValue) {
      if (TodoConfiguration.PROP_TODO_PATTERNS.equals(propertyName)) {
        stopDaemonAndRestartAllFiles("Todo patterns changed");
      }
      else if (TodoConfiguration.PROP_MULTILINE.equals(propertyName)) {
        stopDaemonAndRestartAllFiles("Todo multi-line detection changed");
      }
    }
  }

  private class MyProfileChangeListener implements ProfileChangeAdapter {
    @Override
    public void profileChanged(Profile profile) {
      stopDaemonAndRestartAllFiles("Profile changed");
    }

    @Override
    public void profileActivated(Profile oldProfile, @Nullable Profile profile) {
      stopDaemonAndRestartAllFiles("Profile activated");
    }

    @Override
    public void profilesInitialized() {
      UIUtil.invokeLaterIfNeeded(() -> {
        if (myProject.isDisposed()) return;

        stopDaemonAndRestartAllFiles("Inspection profiles activated");
      });
    }
  }

  private class MyAnActionListener implements AnActionListener {
    private final AnAction escapeAction;

    private MyAnActionListener() {
      escapeAction = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_ESCAPE);
    }

    @Override
    public void beforeActionPerformed(@Nonnull AnAction action, @Nonnull DataContext dataContext, @Nonnull AnActionEvent event) {
      myEscPressed = action == escapeAction;
    }

    @Override
    public void beforeEditorTyping(char c, @Nonnull DataContext dataContext) {
      Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
      //no need to stop daemon if something happened in the console
      if (editor != null && !worthBothering(editor.getDocument(), editor.getProject())) {
        return;
      }
      stopDaemon(true, "Editor typing");
    }
  }

  private static class MyEditorMouseListener implements EditorMouseListener {
    @Nonnull
    private final TooltipController myTooltipController;

    MyEditorMouseListener(@Nonnull TooltipController tooltipController) {
      myTooltipController = tooltipController;
    }

    @Override
    public void mouseExited(@Nonnull EditorMouseEvent e) {
      if (Registry.is("editor.new.mouse.hover.popups")) return;
      if (!myTooltipController.shouldSurvive(e.getMouseEvent())) {
        DaemonTooltipUtil.cancelTooltips();
      }
    }
  }

  private class MyEditorMouseMotionListener implements EditorMouseMotionListener {
    @Override
    public void mouseMoved(@Nonnull EditorMouseEvent e) {
      if (Registry.is("ide.disable.editor.tooltips") || Registry.is("editor.new.mouse.hover.popups")) {
        return;
      }
      Editor editor = e.getEditor();
      if (myProject != editor.getProject()) return;
      if (EditorMouseHoverPopupControl.arePopupsDisabled(editor)) return;

      boolean shown = false;
      try {
        if (e.getArea() == EditorMouseEventArea.EDITING_AREA &&
            !UIUtil.isControlKeyDown(e.getMouseEvent()) &&
            DocumentationManager.getInstance(myProject).getDocInfoHint() == null &&
            EditorUtil.isPointOverText(editor, e.getMouseEvent().getPoint())) {
          LogicalPosition logical = editor.xyToLogicalPosition(e.getMouseEvent().getPoint());
          int offset = editor.logicalPositionToOffset(logical);
          HighlightInfoImpl info = myDaemonCodeAnalyzer.findHighlightByOffset(editor.getDocument(), offset, false);
          if (info == null || info.getDescription() == null || info.getHighlighter() != null && FoldingUtil.isHighlighterFolded(editor, info.getHighlighter())) {
            IdeTooltipManagerImpl.getInstanceImpl().hideCurrent(e.getMouseEvent());
            return;
          }
          DaemonTooltipUtil.showInfoTooltip(info, editor, offset);
          shown = true;
        }
      }
      finally {
        if (!shown && !TooltipController.getInstance().shouldSurvive(e.getMouseEvent())) {
          DaemonTooltipUtil.cancelTooltips();
        }
      }
    }

    @Override
    public void mouseDragged(@Nonnull EditorMouseEvent e) {
      if (Registry.is("editor.new.mouse.hover.popups")) {
        return;
      }
      TooltipController.getInstance().cancelTooltips();
    }
  }

  private void stopDaemon(boolean toRestartAlarm, @NonNls @Nonnull String reason) {
    if (myDaemonCodeAnalyzer.stopProcess(toRestartAlarm, reason)) {
      myDaemonEventPublisher.daemonCancelEventOccurred(reason);
    }
  }

  private void stopDaemonAndRestartAllFiles(@Nonnull String reason) {
    if (myDaemonCodeAnalyzer.doRestart()) {
      myDaemonEventPublisher.daemonCancelEventOccurred(reason);
    }
  }

  //private void removeQuickFixesContributedByPlugin(@Nonnull PluginDescriptor pluginDescriptor) {
  //  for (FileEditor fileEditor : FileEditorManager.getInstance(myProject).getAllEditors()) {
  //    if (fileEditor instanceof TextEditor) {
  //      Editor editor = ((TextEditor)fileEditor).getEditor();
  //      removeHighlightersContributedByPlugin(pluginDescriptor, editor.getMarkupModel().getAllHighlighters());
  //      MarkupModel documentMarkupModel = DocumentMarkupModel.forDocument(editor.getDocument(), myProject, false);
  //      if (documentMarkupModel != null) {
  //        removeHighlightersContributedByPlugin(pluginDescriptor, documentMarkupModel.getAllHighlighters());
  //      }
  //    }
  //  }
  //}

  //private static void removeHighlightersContributedByPlugin(@Nonnull PluginDescriptor pluginDescriptor, RangeHighlighter[] highlighters) {
  //  for (RangeHighlighter highlighter : highlighters) {
  //    HighlightInfo info = HighlightInfo.fromRangeHighlighter(highlighter);
  //    if (info == null) continue;
  //    List<Pair<HighlightInfo.IntentionActionDescriptor, TextRange>> ranges = info.quickFixActionRanges;
  //    if (ranges != null) {
  //      ranges.removeIf((pair) -> isContributedByPlugin(pair.first, pluginDescriptor));
  //    }
  //    List<Pair<HighlightInfo.IntentionActionDescriptor, RangeMarker>> markers = info.quickFixActionMarkers;
  //    if (markers != null) {
  //      markers.removeIf((pair) -> isContributedByPlugin(pair.first, pluginDescriptor));
  //    }
  //  }
  //}

  //private static boolean isContributedByPlugin(@Nonnull HighlightInfo.IntentionActionDescriptor intentionActionDescriptor, @Nonnull PluginDescriptor descriptor) {
  //  IntentionAction action = intentionActionDescriptor.getAction();
  //  PluginId pluginId = PluginManager.getPluginId(action.getClass());
  //  return descriptor.getPluginId().equals(pluginId);
  //}
}
