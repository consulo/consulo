/*
 * Copyright 2013-2017 consulo.io
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
package consulo.fileEditor;

import consulo.annotation.DeprecationInfo;
import consulo.application.AccessToken;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.AWTComponentProvider;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import javax.swing.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 28-Oct-17
 */
public interface FileEditorsSplitters extends AWTComponentProvider {
  Key<FileEditorsSplitters> KEY = Key.create("EditorsSplitters");
  Key<Boolean> OPENED_IN_BULK = Key.create("EditorSplitters.opened.in.bulk");

  static boolean isOpenedInBulk(@Nonnull VirtualFile file) {
    return file.getUserData(OPENED_IN_BULK) != null;
  }

  void readExternal(Element element);

  void writeExternal(Element element);

  void openFiles(@Nonnull UIAccess uiAccess);

  int getSplitCount();

  void startListeningFocus();

  void clear();

  @Nonnull
  FileEditorWindow getOrCreateCurrentWindow(VirtualFile file);

  void setCurrentWindow(FileEditorWindow window, boolean requestFocus);

  @Nullable
  FileEditorWindow getCurrentWindow();

  void updateFileIcon(VirtualFile virtualFile);

  void updateFileName(VirtualFile virtualFile);

  void updateFileColor(VirtualFile virtualFile);

  void updateFileBackgroundColor(VirtualFile virtualFile);

  VirtualFile[] getOpenFiles();

  AccessToken increaseChange();

  boolean isInsideChange();

  @RequiredUIAccess
  void closeFile(VirtualFile file, boolean moveFocus);

  @Nonnull
  VirtualFile[] getSelectedFiles();

  @Nonnull
  FileEditor[] getSelectedEditors();

  @Nonnull
  List<FileEditorWithProviderComposite> findEditorComposites(@Nonnull VirtualFile file);

  FileEditorWithProviderComposite[] getEditorsComposites();

  FileEditorWindow[] getWindows();

  FileEditorWindow[] getOrderedWindows();

  @Nullable
  VirtualFile getCurrentFile();

  boolean isShowing();

  void setTabsPlacement(final int tabPlacement);

  void setTabLayoutPolicy(int scrollTabLayout);

  void trimToSize(final int editor_tab_limit);

  default void toFront() {
  }

  @Nullable
  @RequiredUIAccess
  default FileEditorWindow openInRightSplit(@Nonnull VirtualFile file) {
    return openInRightSplit(file, true);
  }

  @Nullable
  @RequiredUIAccess
  default FileEditorWindow openInRightSplit(@Nonnull VirtualFile file, boolean requestFocus) {
    FileEditorWindow window = getCurrentWindow();
    if (window == null) {
      return null;
    }
    return window.split(SwingConstants.VERTICAL, true, file, true);
  }

  @Nonnull
  default Component getUIComponent() {
    throw new UnsupportedOperationException("Unsupported platform");
  }

  @Override
  @Deprecated
  @DeprecationInfo("See #getUIComponent()")
  @Nonnull
  default javax.swing.JComponent getComponent() {
    throw new UnsupportedOperationException("Unsupported platform");
  }

  default void revalidate() {
  }
}
