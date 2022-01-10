// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl.status;

import com.intellij.ide.DataManager;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.ui.ClickListener;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.PopupState;
import com.intellij.util.Alarm;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import consulo.disposer.Disposer;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

public abstract class EditorBasedStatusBarPopup extends EditorBasedWidget implements StatusBarWidget.Multiframe, CustomStatusBarWidget {
  private final PopupState<JBPopup> myPopupState = PopupState.forPopup();
  private final JPanel myComponent;
  private final boolean myWriteableFileRequired;
  private boolean actionEnabled;
  private final Alarm update;
  // store editor here to avoid expensive and EDT-only getSelectedEditor() retrievals
  private volatile Reference<Editor> myEditor = new WeakReference<>(null);

  public EditorBasedStatusBarPopup(@Nonnull Project project, boolean writeableFileRequired) {
    super(project);
    myWriteableFileRequired = writeableFileRequired;
    update = new Alarm(this);
    myComponent = createComponent();
    myComponent.setVisible(false);

    new ClickListener() {
      @Override
      public boolean onClick(@Nonnull MouseEvent e, int clickCount) {
        update();
        showPopup(e);
        return true;
      }
    }.installOn(myComponent, true);
    myComponent.setBorder(WidgetBorder.WIDE);
  }

  protected JPanel createComponent() {
    return new TextPanel.WithIconAndArrows();
  }

  @Override
  public final void selectionChanged(@Nonnull FileEditorManagerEvent event) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return;
    VirtualFile newFile = event.getNewFile();

    FileEditor fileEditor = newFile == null ? null : FileEditorManager.getInstance(getProject()).getSelectedEditor(newFile);
    Editor editor = fileEditor instanceof TextEditor ? ((TextEditor)fileEditor).getEditor() : null;
    setEditor(editor);

    fileChanged(newFile);
  }

  public final void setEditor(@Nullable Editor editor) {
    myEditor = new WeakReference<>(editor);
  }

  public final void selectionChanged(@Nullable VirtualFile newFile) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }

    FileEditor fileEditor = newFile == null ? null : FileEditorManager.getInstance(getProject()).getSelectedEditor(newFile);
    Editor editor = fileEditor instanceof TextEditor ? ((TextEditor)fileEditor).getEditor() : null;
    myEditor = new WeakReference<>(editor);

    fileChanged(newFile);
  }

  private void fileChanged(VirtualFile newFile) {
    handleFileChange(newFile);
    update();
  }

  protected void handleFileChange(VirtualFile file) {
  }

  @Override
  public void fileOpened(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
    fileChanged(file);
  }

  @Override
  public void fileClosed(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
    fileChanged(file);
  }

  @Override
  public final StatusBarWidget copy() {
    return createInstance(getProject());
  }

  @Nullable
  @Override
  public WidgetPresentation getPresentation() {
    return null;
  }

  @Override
  public void install(@Nonnull StatusBar statusBar) {
    super.install(statusBar);
    registerCustomListeners();
    EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(@Nonnull DocumentEvent e) {
        Document document = e.getDocument();
        updateForDocument(document);
      }
    }, this);
    if (myWriteableFileRequired) {
      ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(VirtualFileManager.VFS_CHANGES, new BulkVirtualFileListenerAdapter(new VirtualFileListener() {
        @Override
        public void propertyChanged(@Nonnull VirtualFilePropertyEvent event) {
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
   *
   * @param document null means update anyway
   */
  protected void updateForDocument(@Nullable Document document) {
    Editor selectedEditor = myEditor.get();
    if (document != null && (selectedEditor == null || selectedEditor.getDocument() != document)) return;
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

  private void showPopup(@Nonnull MouseEvent e) {
    if (!actionEnabled || myPopupState.isRecentlyHidden()) return; // do not show popup
    DataContext dataContext = getContext();
    ListPopup popup = createPopup(dataContext);

    if (popup != null) {
      Dimension dimension = popup.getContent().getPreferredSize();
      Point at = new Point(0, -dimension.height);
      myPopupState.prepareToShow(popup);
      popup.show(new RelativePoint(e.getComponent(), at));
      Disposer.register(this, popup); // destroy popup on unexpected project close
    }
  }

  @Nonnull
  protected DataContext getContext() {
    Editor editor = getEditor();
    DataContext parent = DataManager.getInstance().getDataContext((Component)myStatusBar);
    VirtualFile selectedFile = getSelectedFile();
    return SimpleDataContext.getSimpleContext(ContainerUtil.<Key, Object>immutableMapBuilder().put(CommonDataKeys.VIRTUAL_FILE, selectedFile)
                                                      .put(CommonDataKeys.VIRTUAL_FILE_ARRAY, selectedFile == null ? VirtualFile.EMPTY_ARRAY : new VirtualFile[]{selectedFile})
                                                      .put(CommonDataKeys.PROJECT, getProject())
                                                      .put(PlatformDataKeys.CONTEXT_COMPONENT, editor == null ? null : editor.getComponent()).build(), parent);
  }

  @Override
  public JComponent getComponent() {
    return myComponent;
  }

  protected boolean isEmpty() {
    Boolean result = ObjectUtils.doIfCast(myComponent, TextPanel.WithIconAndArrows.class, textPanel -> StringUtil.isEmpty(textPanel.getText()) && !textPanel.hasIcon());
    return result != null && result;
  }

  public boolean isActionEnabled() {
    return actionEnabled;
  }

  protected void updateComponent(@Nonnull WidgetState state) {
    myComponent.setToolTipText(state.toolTip);
    ObjectUtil.consumeIfCast(myComponent, TextPanel.WithIconAndArrows.class, textPanel -> {
      textPanel.setTextAlignment(Component.CENTER_ALIGNMENT);
      textPanel.setIcon(state.icon);
      textPanel.setText(state.text);
    });
  }

  @TestOnly
  public void updateInTests(boolean immediately) {
    update();
    update.drainRequestsInTest();
    UIUtil.dispatchAllInvocationEvents();
    if (immediately) {
      // for widgets with background activities, the first flush() adds handlers to be called
      update.drainRequestsInTest();
      UIUtil.dispatchAllInvocationEvents();
    }
  }

  @TestOnly
  public void flushUpdateInTests() {
    update.drainRequestsInTest();
  }

  public void update() {
    update(null);
  }

  public void update(@Nullable Runnable finishUpdate) {
    if (update.isDisposed()) return;

    update.cancelAllRequests();
    update.addRequest(() -> {
      if (isDisposed()) return;

      VirtualFile file = getSelectedFile();

      WidgetState state = getWidgetState(file);
      if (state == WidgetState.NO_CHANGE) {
        return;
      }

      if (state == WidgetState.NO_CHANGE_MAKE_VISIBLE) {
        myComponent.setVisible(true);
        return;
      }

      if (state == WidgetState.HIDDEN) {
        myComponent.setVisible(false);
        return;
      }

      myComponent.setVisible(true);

      actionEnabled = state.actionEnabled && isEnabledForFile(file);

      myComponent.setEnabled(actionEnabled);
      updateComponent(state);

      if (myStatusBar != null && !myComponent.isValid()) {
        myStatusBar.updateWidget(ID());
      }

      if (finishUpdate != null) {
        finishUpdate.run();
      }
      afterVisibleUpdate(state);
    }, 200, ModalityState.any());
  }

  protected void afterVisibleUpdate(@Nonnull WidgetState state) {
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

    protected final String toolTip;
    private final String text;
    private final boolean actionEnabled;
    private Image icon;

    private WidgetState() {
      this("", "", false);
    }

    public WidgetState(String toolTip, String text, boolean actionEnabled) {
      this.toolTip = toolTip;
      this.text = text;
      this.actionEnabled = actionEnabled;
    }

    /**
     * Returns a special state for dumb mode (when indexes are not ready).
     * Your widget should show this state if it depends on indexes, when DumbService.isDumb is true.
     * <p>
     * Use myConnection.subscribe(DumbService.DUMB_MODE, your_listener) inside registerCustomListeners,
     * and call update() inside listener callbacks, to refresh your widget state when indexes are loaded
     */
    public static WidgetState getDumbModeState(@Nls String name, String widgetPrefix) {
      // todo: update accordingly to UX-252
      return new WidgetState(ActionUtil.getUnavailableMessage(name, false), widgetPrefix + IdeBundle.message("progress.indexing.updating"), false);
    }

    public void setIcon(Image icon) {
      this.icon = icon;
    }

    @Nls
    public String getText() {
      return text;
    }

    public String getToolTip() {
      return toolTip;
    }

    public Image getIcon() {
      return icon;
    }
  }

  @Nonnull
  protected abstract WidgetState getWidgetState(@Nullable VirtualFile file);

  /**
   * @param file result of {@link EditorBasedStatusBarPopup#getSelectedFile()}
   * @return false if widget should be disabled for {@code file}
   * even if {@link EditorBasedStatusBarPopup#getWidgetState(VirtualFile)} returned {@link WidgetState#actionEnabled}.
   */
  protected boolean isEnabledForFile(@Nullable VirtualFile file) {
    return file == null || !myWriteableFileRequired || file.isWritable();
  }

  @Nullable
  protected abstract ListPopup createPopup(DataContext context);

  protected void registerCustomListeners() {
  }

  @Nonnull
  protected abstract StatusBarWidget createInstance(@Nonnull Project project);
}
