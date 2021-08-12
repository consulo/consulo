/*
 * Copyright 2013-2018 consulo.io
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
package consulo.fileEditor.impl;

import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.fileEditor.impl.EditorTabbedContainer;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.fileTypes.impl.VfsIconUtil;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2018-05-11
 */
public abstract class EditorWindowBase implements EditorWindow {
  private static final Logger LOG = Logger.getInstance(EditorWindowBase.class);

  protected abstract EditorWithProviderComposite getEditorAt(final int i);

  protected abstract void setTitleAt(final int index, final String text);

  protected abstract void setBackgroundColorAt(final int index, final java.awt.Color color);

  protected abstract void setToolTipTextAt(final int index, final String text);

  protected abstract void setForegroundAt(final int index, final Color color);

  protected abstract void setWaveColor(final int index, @Nullable final Color color);

  protected abstract void setIconAt(final int index, final consulo.ui.image.Image icon);

  protected abstract void setTabLayoutPolicy(final int policy);

  protected abstract void trimToSize(final int limit, @Nullable final VirtualFile fileToIgnore, final boolean transferFocus);

  protected void updateFileName(VirtualFile file) {
    final int index = findEditorIndex(findFileComposite(file));
    if (index != -1) {
      setTitleAt(index, EditorTabbedContainer.calcTabTitle(getManager().getProject(), file));
      setToolTipTextAt(index, UISettings.getInstance().getShowTabsTooltips() ? getManager().getFileTooltipText(file) : null);
    }
  }

  protected void updateFileIcon(VirtualFile file) {
    final int index = findEditorIndex(findFileComposite(file));
    LOG.assertTrue(index != -1);
    setIconAt(index, getFileIcon(file));
  }

  protected void updateFileBackgroundColor(@Nonnull VirtualFile file) {
    final int index = findEditorIndex(findFileComposite(file));
    if (index != -1) {
      final Color color = EditorTabbedContainer.calcTabColor(getManager().getProject(), file);
      setBackgroundColorAt(index, color);
    }
  }

  /**
   * @return icon which represents file's type and modification status
   */
  @Nullable
  private consulo.ui.image.Image getFileIcon(@Nonnull final VirtualFile file) {
    if (!file.isValid()) {
      return UnknownFileType.INSTANCE.getIcon();
    }

    final consulo.ui.image.Image baseIcon = VfsIconUtil.getIconNoDefer(file, Iconable.ICON_FLAG_READ_STATUS, getManager().getProject());
    int count = 1;

    final Image pinIcon;
    final EditorComposite composite = findFileComposite(file);
    if (composite != null && composite.isPinned()) {
      count++;
      pinIcon = AllIcons.Nodes.TabPin;
    }
    else {
      pinIcon = null;
    }

    // FIXME [VISTALL] not supported for now
    consulo.ui.image.Image modifiedIcon = null;
    //UISettings settings = UISettings.getInstance();
    //if (settings.getMarkModifiedTabsWithAsterisk() || !settings.getHideTabsIfNeed()) {
    //  modifiedIcon = settings.getMarkModifiedTabsWithAsterisk() && composite != null && composite.isModified() ? MODIFIED_ICON : GAP_ICON;
    //  count++;
    //}
    //else {
    //  modifiedIcon = null;
    //}
    //
    //if (count == 1) return baseIcon;

    if (pinIcon != null && modifiedIcon == null) {
      return ImageEffects.layered(baseIcon, pinIcon);
    }

    // FIXME [VISTALL] not supported for now
    //int i = 0;
    //final LayeredIcon result = new LayeredIcon(count);
    //int xShift = !settings.getHideTabsIfNeed() ? 4 : 0;
    //result.setIcon(baseIcon, i++, xShift, 0);
    //if (pinIcon != null) result.setIcon(pinIcon, i++, xShift, 0);
    //if (modifiedIcon != null) result.setIcon(modifiedIcon, i++);
    //
    //return JBUI.scale(result);
    return baseIcon;
  }

  public int findEditorIndex(final EditorComposite editorToFind) {
    for (int i = 0; i != getTabCount(); ++i) {
      final EditorWithProviderComposite editor = getEditorAt(i);
      if (editor.equals(editorToFind)) {
        return i;
      }
    }
    return -1;
  }

  public VirtualFile getFileAt(int i) {
    return getEditorAt(i).getFile();
  }

  @Override
  public void closeAllExcept(VirtualFile selectedFile) {
    final VirtualFile[] files = getFiles();
    for (final VirtualFile file : files) {
      if (!Comparing.equal(file, selectedFile) && !isFilePinned(file)) {
        closeFile(file);
      }
    }
  }

  @Nonnull
  @Override
  public VirtualFile[] getFiles() {
    final int tabCount = getTabCount();
    final VirtualFile[] res = new VirtualFile[tabCount];
    for (int i = 0; i != tabCount; ++i) {
      res[i] = getEditorAt(i).getFile();
    }
    return res;
  }

  @Override
  public int findFileIndex(final VirtualFile fileToFind) {
    for (int i = 0; i != getTabCount(); ++i) {
      final VirtualFile file = getFileAt(i);
      if (file.equals(fileToFind)) {
        return i;
      }
    }
    return -1;
  }

  @Nonnull
  @Override
  public EditorWithProviderComposite[] getEditors() {
    final int tabCount = getTabCount();
    final EditorWithProviderComposite[] res = new EditorWithProviderComposite[tabCount];
    for (int i = 0; i != tabCount; ++i) {
      res[i] = getEditorAt(i);
    }
    return res;
  }

  @Nonnull
  @Override
  public abstract FileEditorManagerImpl getManager();

  @Override
  @Nullable
  public EditorWithProviderComposite findFileComposite(final VirtualFile file) {
    for (int i = 0; i != getTabCount(); ++i) {
      final EditorWithProviderComposite editor = getEditorAt(i);
      if (editor.getFile().equals(file)) {
        return editor;
      }
    }
    return null;
  }
}
