// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.statusBar;

import consulo.annotation.DeprecationInfo;
import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.BulkVirtualFileListenerAdapter;
import consulo.virtualFileSystem.event.VirtualFileListener;
import consulo.virtualFileSystem.event.VirtualFilePropertyEvent;
import org.jspecify.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class EditorBasedStatusBarPopup extends EditorBasedWidget
    implements StatusBarWidget.Multiframe, StatusBarWidget.MultipleTextValuesPresentation {
    private final boolean myWriteableFileRequired;
    private @Nullable Future<?> myUpdateRequest;
    // store editor here to avoid expensive and EDT-only getSelectedEditor() retrievals
    private volatile Reference<Editor> myEditor = new WeakReference<>(null);

    private WidgetState myState = WidgetState.HIDDEN;
    private boolean myVisible;
    private boolean myActionEnabled;

    public EditorBasedStatusBarPopup(Project project, StatusBarWidgetFactory factory, boolean writeableFileRequired) {
        super(project, factory);
        myWriteableFileRequired = writeableFileRequired;
    }

    @Override
    public final void selectionChanged(FileEditorManagerEvent event) {
        if (myProject.getApplication().isUnitTestMode()) {
            return;
        }
        VirtualFile newFile = event.getNewFile();

        FileEditor fileEditor = newFile == null ? null : FileEditorManager.getInstance(getProject()).getSelectedEditor(newFile);
        Editor editor = fileEditor instanceof TextEditor textEditor ? textEditor.getEditor() : null;
        setEditor(editor);

        fileChanged(newFile);
    }

    public final void setEditor(@Nullable Editor editor) {
        myEditor = new WeakReference<>(editor);
    }

    public final void selectionChanged(@Nullable VirtualFile newFile) {
        if (myProject.getApplication().isUnitTestMode()) {
            return;
        }

        FileEditor fileEditor = newFile == null ? null : FileEditorManager.getInstance(getProject()).getSelectedEditor(newFile);
        Editor editor = fileEditor instanceof TextEditor textEditor ? textEditor.getEditor() : null;
        myEditor = new WeakReference<>(editor);

        fileChanged(newFile);
    }

    private void fileChanged(@Nullable VirtualFile newFile) {
        handleFileChange(newFile);
        update();
    }

    protected void handleFileChange(@Nullable VirtualFile file) {
    }

    @Override
    public void fileOpened(FileEditorManager source, VirtualFile file) {
        fileChanged(file);
    }

    @Override
    public void fileClosed(FileEditorManager source, VirtualFile file) {
        fileChanged(file);
    }

    @Override
    public final StatusBarWidget copy() {
        return createInstance(getProject());
    }

    @Override
    public WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    @RequiredUIAccess
    public @Nullable String getSelectedValue() {
        return myVisible ? myState.getText() : null;
    }

    @Override
    public @Nullable Image getIcon() {
        return myVisible ? myState.getIcon() : null;
    }

    @Override
    public LocalizeValue getTooltipText() {
        return myState.getToolTip();
    }

    @Override
    public @Nullable String getShortcutText() {
        return myState.getShortcutText();
    }

    @Override
    public boolean isEnabled() {
        return myActionEnabled;
    }

    @Override
    @RequiredUIAccess
    public @Nullable ListPopup getPopupStep(DataContext dataContext) {
        if (!myActionEnabled) {
            return null;
        }

        ListPopup popup = createPopup(getContext(dataContext));
        if (popup != null) {
            Disposer.register(this, popup); // destroy popup on unexpected project close
        }
        return popup;
    }

    @Override
    public void install(StatusBar statusBar) {
        super.install(statusBar);
        registerCustomListeners();
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(
            new DocumentListener() {
                @Override
                public void documentChanged(DocumentEvent e) {
                    Document document = e.getDocument();
                    updateForDocument(document);
                }
            },
            this
        );
        if (myWriteableFileRequired) {
            myProject.getApplication()
                .getMessageBus()
                .connect(this)
                .subscribe(BulkFileListener.class, new BulkVirtualFileListenerAdapter(new VirtualFileListener() {
                    @Override
                    public void propertyChanged(VirtualFilePropertyEvent event) {
                        if (VirtualFile.PROP_WRITABLE.equals(event.getPropertyName())) {
                            updateForFile(event.getFile());
                        }
                    }
                }));
        }
        setEditor(getEditor());
        update();
    }

    /**
     * @param document null means update anyway
     */
    protected void updateForDocument(@Nullable Document document) {
        Editor selectedEditor = myEditor.get();
        if (document != null && (selectedEditor == null || selectedEditor.getDocument() != document)) {
            return;
        }
        update();
    }

    /**
     * @param file null means update anyway
     */
    protected void updateForFile(@Nullable VirtualFile file) {
        if (file == null) {
            update();
        }
        else {
            updateForDocument(FileDocumentManager.getInstance().getCachedDocument(file));
        }
    }

    protected DataContext getContext(DataContext parent) {
        Editor editor = getEditor();
        VirtualFile selectedFile = getSelectedFile();
        return DataContext.builder()
            .parent(parent)
            .add(VirtualFile.KEY, selectedFile)
            .add(VirtualFile.KEY_OF_ARRAY, selectedFile == null ? VirtualFile.EMPTY_ARRAY : new VirtualFile[]{selectedFile})
            .add(Project.KEY, getProject())
            .add(Editor.KEY, editor)
            .build();
    }

    public boolean isActionEnabled() {
        return myActionEnabled;
    }

    public void update() {
        update(null);
    }

    public synchronized void update(@Nullable Runnable finishUpdate) {
        if (isDisposed()) {
            return;
        }

        Future<?> previous = myUpdateRequest;
        if (previous != null) {
            previous.cancel(false);
        }

        myUpdateRequest = AppExecutorUtil.getAppScheduledExecutorService().schedule(
            () -> giveInProjectUI(() -> {
                if (isDisposed()) {
                    return;
                }

                VirtualFile file = getSelectedFile();

                ReadAction.nonBlocking(() -> getWidgetState(file))
                    .expireWith(this)
                    .submitDefault()
                    .onSuccess(state -> giveInProjectUI(() -> applyState(file, state, finishUpdate)));
            }),
            200,
            TimeUnit.MILLISECONDS
        );
    }

    private void giveInProjectUI(@RequiredUIAccess Runnable runnable) {
        UIAccess uiAccess = myProject.getUIAccess();
        if (uiAccess.isValid()) {
            uiAccess.give(runnable);
        }
    }

    @Override
    public synchronized void dispose() {
        super.dispose();

        Future<?> request = myUpdateRequest;
        myUpdateRequest = null;
        if (request != null) {
            request.cancel(false);
        }
    }

    @RequiredUIAccess
    private void applyState(@Nullable VirtualFile file, WidgetState state, @Nullable Runnable finishUpdate) {
        if (isDisposed() || state == WidgetState.NO_CHANGE) {
            return;
        }

        if (state == WidgetState.NO_CHANGE_MAKE_VISIBLE || state == WidgetState.HIDDEN) {
            myVisible = state == WidgetState.NO_CHANGE_MAKE_VISIBLE;
            updateWidget();
            return;
        }

        myVisible = true;
        myState = state;
        myActionEnabled = state.actionEnabled && isEnabledForFile(file);

        updateWidget();

        if (finishUpdate != null) {
            finishUpdate.run();
        }
        afterVisibleUpdate(state);
    }

    private void updateWidget() {
        StatusBar statusBar = myStatusBar;
        if (statusBar != null) {
            statusBar.updateWidget(getId());
        }
    }

    protected void afterVisibleUpdate(WidgetState state) {
    }

    protected static class WidgetState {
        /**
         * Return this state if you want to hide the widget
         */
        public static final WidgetState HIDDEN = new WidgetState();

        /**
         * Return this state if you don't want to change widget presentation
         */
        public static final WidgetState NO_CHANGE = new WidgetState();

        /**
         * Return this state if you want to show widget in its previous state
         * but without updating its content
         */
        public static final WidgetState NO_CHANGE_MAKE_VISIBLE = new WidgetState();

        protected final LocalizeValue toolTip;
        protected @Nullable String shortcutText;

        private final LocalizeValue text;
        private final boolean actionEnabled;
        private @Nullable Image icon;

        private WidgetState() {
            this(LocalizeValue.empty(), LocalizeValue.empty(), false);
        }

        public WidgetState(LocalizeValue toolTip, LocalizeValue text, boolean actionEnabled) {
            this.toolTip = toolTip;
            this.text = text;
            this.actionEnabled = actionEnabled;
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public WidgetState(String toolTip, String text, boolean actionEnabled) {
            this(LocalizeValue.ofNullable(toolTip), LocalizeValue.ofNullable(text), actionEnabled);
        }

        public void setIcon(@Nullable Image icon) {
            this.icon = icon;
        }

        public void setShortcutText(@Nullable String shortcutText) {
            this.shortcutText = shortcutText;
        }

        public @Nullable String getShortcutText() {
            return shortcutText;
        }

        public String getText() {
            return text.get();
        }

        public LocalizeValue getToolTip() {
            return toolTip;
        }

        public @Nullable Image getIcon() {
            return icon;
        }
    }

    protected abstract WidgetState getWidgetState(@Nullable VirtualFile file);

    /**
     * @param file result of {@link EditorBasedStatusBarPopup#getSelectedFile()}
     * @return false if widget should be disabled for {@code file}
     * even if {@link EditorBasedStatusBarPopup#getWidgetState(VirtualFile)} returned {@link WidgetState#actionEnabled}.
     */
    protected boolean isEnabledForFile(@Nullable VirtualFile file) {
        return file == null || !myWriteableFileRequired || file.isWritable();
    }

    protected abstract @Nullable ListPopup createPopup(DataContext context);

    protected void registerCustomListeners() {
    }

    protected abstract StatusBarWidget createInstance(Project project);
}
