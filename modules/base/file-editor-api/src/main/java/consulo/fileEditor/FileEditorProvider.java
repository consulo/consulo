/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.dumb.DumbAware;
import consulo.component.extension.ExtensionPointName;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import org.jdom.Element;

import jakarta.annotation.Nonnull;

/**
 * Should be registered via {@link #EP_FILE_EDITOR_PROVIDER}.
 *
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 * @see DumbAware
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface FileEditorProvider {
  ExtensionPointName<FileEditorProvider> EP_FILE_EDITOR_PROVIDER = ExtensionPointName.create(FileEditorProvider.class);

  Key<FileEditorProvider> KEY = Key.create("com.intellij.fileEditorProvider");

  /**
   * @param file file to be tested for acceptance. This
   *             parameter is never {@code null}.
   * @return whether the provider can create valid editor for the specified
   * {@code file} or not
   */
  boolean accept(@Nonnull Project project, @Nonnull VirtualFile file);

  /**
   * Creates editor for the specified file. This method
   * is called only if the provider has accepted this file (i.e. method {@link #accept(Project, VirtualFile)} returned
   * {@code true}).
   * The provider should return only valid editor.
   *
   * @return created editor for specified file. This method should never return {@code null}.
   */
  @Nonnull
  @RequiredUIAccess
  FileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile file);

  /**
   * Disposes the specified {@code editor}. It is guaranteed that this method is invoked only for editors
   * created with this provider.
   *
   * @param editor editor to be disposed. This parameter is always not {@code null}.
   */
  default void disposeEditor(@Nonnull FileEditor editor) {
    Disposer.dispose(editor);
  }

  /**
   * Deserialize state from the specified {@code sourceElement}
   * Use {@link FileEditorState#INSTANCE} as default implementation
   */
  @Nonnull
  default FileEditorState readState(@Nonnull Element sourceElement, @Nonnull Project project, @Nonnull VirtualFile file) {
    return FileEditorState.INSTANCE;
  }

  /**
   * Serializes state into the specified {@code targetElement}
   */
  default void writeState(@Nonnull FileEditorState state, @Nonnull Project project, @Nonnull Element targetElement) {
  }

  /**
   * @return id of type of the editors that are created with this FileEditorProvider. Each FileEditorProvider should have
   * unique non null id. The id is used for saving/loading of EditorStates.
   */
  @Nonnull
  String getEditorTypeId();

  /**
   * @return policy that specifies how show editor created via this provider be opened
   * @see FileEditorPolicy#NONE
   * @see FileEditorPolicy#HIDE_DEFAULT_EDITOR
   * @see FileEditorPolicy#PLACE_BEFORE_DEFAULT_EDITOR
   */
  @Nonnull
  default FileEditorPolicy getPolicy() {
    return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
  }
}