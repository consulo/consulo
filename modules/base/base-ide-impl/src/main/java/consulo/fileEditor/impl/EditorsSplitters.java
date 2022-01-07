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
package consulo.fileEditor.impl;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.annotation.DeprecationInfo;
import consulo.desktop.util.awt.migration.AWTComponentProvider;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 28-Oct-17
 */
public interface EditorsSplitters extends AWTComponentProvider {
  Key<EditorsSplitters> KEY = Key.create("EditorsSplitters");
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
  EditorWindow getOrCreateCurrentWindow(VirtualFile file);

  void setCurrentWindow(EditorWindow window, boolean requestFocus);

  @Nullable
  EditorWindow getCurrentWindow();

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
  List<EditorWithProviderComposite> findEditorComposites(@Nonnull VirtualFile file);

  EditorWithProviderComposite[] getEditorsComposites();

  EditorWindow[] getWindows();

  EditorWindow[] getOrderedWindows();

  @Nullable
  VirtualFile getCurrentFile();

  boolean isShowing();

  void setTabsPlacement(final int tabPlacement);

  void setTabLayoutPolicy(int scrollTabLayout);

  void trimToSize(final int editor_tab_limit);

  @Nullable
  @RequiredUIAccess
  default EditorWindow openInRightSplit(@Nonnull VirtualFile file) {
    return openInRightSplit(file, true);
  }

  @Nullable
  @RequiredUIAccess
  default EditorWindow openInRightSplit(@Nonnull VirtualFile file, boolean requestFocus) {
    EditorWindow window = getCurrentWindow();
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
