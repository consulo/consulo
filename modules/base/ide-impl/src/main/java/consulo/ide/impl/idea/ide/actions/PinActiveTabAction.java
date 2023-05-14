/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ui.content.ContentManagerUtil;
import consulo.application.AllIcons;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.FileEditorWindow;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Toggleable;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Pins any kind of tab in context: editor tab, toolwindow tab or other tabs.
 * <p>
 * todo drop TW and EW, both are only for menu|Window tab/editor sub-menus.
 */
public class PinActiveTabAction extends DumbAwareAction implements Toggleable {

  public static abstract class Handler {
    public final boolean isPinned;
    public final boolean isActiveTab;

    abstract void setPinned(boolean value);

    public Handler(boolean isPinned, boolean isActiveTab) {
      this.isPinned = isPinned;
      this.isActiveTab = isActiveTab;
    }
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Handler handler = getHandler(e);
    if (handler == null) return;
    boolean selected = !handler.isPinned;
    handler.setPinned(selected);
    e.getPresentation().putClientProperty(SELECTED_PROPERTY, selected);
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Handler handler = getHandler(e);
    boolean enabled = handler != null;
    boolean selected = enabled && handler.isPinned;

    e.getPresentation().setIcon(ActionPlaces.isToolbarPlace(e.getPlace()) ? AllIcons.General.Pin_tab : null);
    e.getPresentation().putClientProperty(SELECTED_PROPERTY, selected);

    String text;
    // add the word "active" if the target tab is not current
    if (ActionPlaces.isMainMenuOrActionSearch(e.getPlace()) || handler != null && !handler.isActiveTab) {
      text = selected ? IdeBundle.message("action.unpin.active.tab") : IdeBundle.message("action.pin.active.tab");
    }
    else {
      text = selected ? IdeBundle.message("action.unpin.tab") : IdeBundle.message("action.pin.tab");
    }
    e.getPresentation().setText(text);
    e.getPresentation().setEnabledAndVisible(enabled);
  }

  protected Handler getHandler(@Nonnull AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    FileEditorWindow currentWindow = e.getData(FileEditorWindow.DATA_KEY);

    Content content = currentWindow != null ? null : getContentFromEvent(e);
    if (content != null && content.isPinnable()) {
      return createHandler(content);
    }

    final FileEditorWindow window = currentWindow != null ? currentWindow : project != null ? FileEditorManager.getInstance(project).getCurrentWindow() : null;
    VirtualFile selectedFile = window == null ? null : getFileFromEvent(e, window);
    if (selectedFile != null) {
      return createHandler(window, selectedFile);
    }
    return null;
  }

  @Nullable
  protected VirtualFile getFileFromEvent(@Nonnull AnActionEvent e, @Nonnull FileEditorWindow window) {
    return getFileInWindow(e, window);
  }

  @Nullable
  protected Content getContentFromEvent(@Nonnull AnActionEvent e) {
    Content content = getNonToolWindowContent(e);
    if (content == null) content = getToolWindowContent(e);
    return content != null && content.isValid() ? content : null;
  }

  @Nonnull
  private static Handler createHandler(final Content content) {
    return new Handler(content.isPinned(), content.getManager().getSelectedContent() == content) {
      @Override
      void setPinned(boolean value) {
        content.setPinned(value);
      }
    };
  }

  @Nonnull
  private static Handler createHandler(final FileEditorWindow window, final VirtualFile selectedFile) {
    return new Handler(window.isFilePinned(selectedFile), selectedFile.equals(window.getSelectedFile())) {
      @Override
      void setPinned(boolean value) {
        window.setFilePinned(selectedFile, value);
      }
    };
  }

  @Nullable
  private static Content getNonToolWindowContent(@Nonnull AnActionEvent e) {
    Content result = null;
    Content[] contents = e.getData(Content.KEY_OF_ARRAY);
    if (contents != null && contents.length == 1) result = contents[0];
    if (result != null && result.isPinnable()) return result;

    ContentManager contentManager = ContentManagerUtil.getContentManagerFromContext(e.getDataContext(), true);
    result = contentManager != null ? contentManager.getSelectedContent() : null;
    if (result != null && result.isPinnable()) return result;
    return null;
  }

  @Nullable
  private static Content getToolWindowContent(@Nonnull AnActionEvent e) {
    // note to future readers: TW tab "pinned" icon is shown when content.getUserData(TW.SHOW_CONTENT_ICON) is true
    ToolWindow window = e.getData(ToolWindow.KEY);
    Content result = window != null ? window.getContentManager().getSelectedContent() : null;
    return result != null && result.isPinnable() ? result : null;
  }

  @Nullable
  private static VirtualFile getFileInWindow(@Nonnull AnActionEvent e, @Nonnull FileEditorWindow window) {
    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (file == null) file = window.getSelectedFile();
    if (file != null && window.isFileOpen(file)) return file;
    return null;
  }

  public static class TW extends PinActiveTabAction {
    @Nullable
    @Override
    protected VirtualFile getFileFromEvent(@Nonnull AnActionEvent e, @Nonnull FileEditorWindow window) {
      return null;
    }

    @Override
    protected Content getContentFromEvent(@Nonnull AnActionEvent e) {
      return getToolWindowContent(e);
    }
  }

  public static class EW extends PinActiveTabAction {
    @Nullable
    @Override
    protected VirtualFile getFileFromEvent(@Nonnull AnActionEvent e, @Nonnull FileEditorWindow window) {
      return window.getSelectedFile();
    }

    @Override
    protected Content getContentFromEvent(@Nonnull AnActionEvent e) {
      return null;
    }
  }
}
