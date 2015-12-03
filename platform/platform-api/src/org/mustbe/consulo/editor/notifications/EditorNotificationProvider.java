package org.mustbe.consulo.editor.notifications;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredReadAction;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 12.11.2015
 */
public interface EditorNotificationProvider<T extends JComponent> {
  ExtensionPointName<EditorNotificationProvider<?>> EP_NAME = new ExtensionPointName<EditorNotificationProvider<?>>("com.intellij.editorNotificationProvider");

  @NotNull
  Key<T> getKey();

  @Nullable
  @RequiredReadAction
  T createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor fileEditor);
}