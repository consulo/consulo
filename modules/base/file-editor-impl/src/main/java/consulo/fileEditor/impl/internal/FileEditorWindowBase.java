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
package consulo.fileEditor.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ui.UISettings;
import consulo.component.util.Iconable;
import consulo.fileEditor.*;
import consulo.fileEditor.internal.FileEditorWithModifiedIcon;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.UIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2018-05-11
 */
public abstract class FileEditorWindowBase implements FileEditorWindow {
  private static final Logger LOG = Logger.getInstance(FileEditorWindowBase.class);

  protected abstract FileEditorWithProviderComposite getEditorAt(final int i);

  protected abstract void setTitleAt(final int index, final String text);

  protected abstract void setBackgroundColorAt(final int index, final Color color);

  protected abstract void setToolTipTextAt(final int index, final String text);

  protected abstract void setForegroundAt(final int index, final Color color);

  protected abstract void setWaveColor(final int index, @Nullable final Color color);

  protected abstract void setIconAt(final int index, final Image icon);

  protected abstract void setTabLayoutPolicy(final int policy);

  protected abstract void trimToSize(final int limit, @Nullable final VirtualFile fileToIgnore, final boolean transferFocus);

  protected void updateFileName(VirtualFile file, String tabText, String tabTooltip) {
    final int index = findEditorIndex(findFileComposite(file));
    if (index != -1) {
      setTitleAt(index, tabText);
      setToolTipTextAt(index, tabTooltip);
    }
  }

  protected void updateFileIcon(VirtualFile file, Image image) {
    final int index = findEditorIndex(findFileComposite(file));
    if (index != -1) {
      setIconAt(index, image);
    }
  }

  protected void updateFileBackgroundColor(@Nonnull VirtualFile file) {
    final int index = findEditorIndex(findFileComposite(file));
    if (index != -1) {
      final ColorValue color = EditorTabPresentationUtil.getEditorTabBackgroundColor(getManager().getProject(), file, this);
      setBackgroundColorAt(index, TargetAWT.to(color));
    }
  }

  /**
   * @return icon which represents file's type and modification status
   */
  @Nullable
  @RequiredReadAction
  protected Image getFileIcon(@Nonnull final VirtualFile file) {
    UIAccess.assetIsNotUIThread();
    if (!file.isValid()) {
      return UnknownFileType.INSTANCE.getIcon();
    }

    final Image baseIcon = VirtualFileManager.getInstance().getFileIconNoDefer(file, getManager().getProject(), Iconable.ICON_FLAG_READ_STATUS);

    final FileEditorWithProviderComposite composite = findFileComposite(file);

    boolean wantModifiedIcon = false;

    UISettings settings = UISettings.getInstance();
    if (settings.getMarkModifiedTabsWithAsterisk() || !settings.getHideTabsIfNeed()) {
      if (settings.getMarkModifiedTabsWithAsterisk() && composite != null && composite.isModified()) {
        wantModifiedIcon = true;
      }
    }

    if (!wantModifiedIcon && composite != null) {
      for (FileEditor fileEditor : composite.getEditors()) {
        if (fileEditor instanceof FileEditorWithModifiedIcon && fileEditor.isModified()) {
          wantModifiedIcon = true;
          break;
        }
      }
    }

    if (wantModifiedIcon) {
      return ImageEffects.layered(baseIcon, PlatformIconGroup.generalModified());
    }

    return baseIcon;
  }

  public int findEditorIndex(final FileEditorComposite editorToFind) {
    for (int i = 0; i != getTabCount(); ++i) {
      final FileEditorWithProviderComposite editor = getEditorAt(i);
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
      if (!Objects.equals(file, selectedFile) && !isFilePinned(file)) {
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
  public FileEditorWithProviderComposite[] getEditors() {
    final int tabCount = getTabCount();
    final FileEditorWithProviderComposite[] res = new FileEditorWithProviderComposite[tabCount];
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
  public FileEditorWithProviderComposite findFileComposite(final VirtualFile file) {
    for (int i = 0; i != getTabCount(); ++i) {
      final FileEditorWithProviderComposite editor = getEditorAt(i);
      if (editor.getFile().equals(file)) {
        return editor;
      }
    }
    return null;
  }
}
