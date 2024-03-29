// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.codeEditor.event.EditorEventMulticaster;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Provides services for creating document and editor instances.
 * <p>
 * Creating and releasing of editors must be done from EDT.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class EditorFactory {
  /**
   * Returns the editor factory instance.
   *
   * @return the editor factory instance.
   */
  @Nonnull
  public static EditorFactory getInstance() {
    return Application.get().getInstance(EditorFactory.class);
  }

  /**
   * Creates a document from the specified text specified as a character sequence.
   */
  @Nonnull
  public abstract Document createDocument(@Nonnull CharSequence text);

  /**
   * Creates a document from the specified text specified as an array of characters.
   */
  @Nonnull
  public abstract Document createDocument(@Nonnull char[] text);

  /**
   * Creates an editor for the specified document. Must be invoked in EDT.
   * <p>
   * The created editor must be disposed after use by calling {@link #releaseEditor(Editor)}.
   * </p>
   */
  public abstract Editor createEditor(@Nonnull Document document);

  /**
   * Creates a read-only editor for the specified document. Must be invoked in EDT.
   * <p>
   * The created editor must be disposed after use by calling {@link #releaseEditor(Editor)}.
   * </p>
   */
  public abstract Editor createViewer(@Nonnull Document document);

  /**
   * Creates an editor for the specified document associated with the specified project. Must be invoked in EDT.
   * <p>
   * The created editor must be disposed after use by calling {@link #releaseEditor(Editor)}.
   * </p>
   *
   * @see Editor#getProject()
   */
  public abstract Editor createEditor(@Nonnull Document document, @Nullable Project project);

  /**
   * Does the same as {@link #createEditor(Document, Project)} and also sets the special kind for the created editor
   */
  public abstract Editor createEditor(@Nonnull Document document, @Nullable Project project, @Nonnull EditorKind kind);

  /**
   * Creates an editor for the specified document associated with the specified project. Must be invoked in EDT.
   * <p>
   * The created editor must be disposed after use by calling {@link #releaseEditor(Editor)}.
   * </p>
   *
   * @param document the document to create the editor for.
   * @param project  the project for which highlighter should be created
   * @param fileType the file type according to which the editor contents is highlighted.
   * @param isViewer true if read-only editor should be created
   * @see Editor#getProject()
   */
  public abstract Editor createEditor(@Nonnull Document document, Project project, @Nonnull FileType fileType, boolean isViewer);

  /**
   * Creates an editor for the specified document associated with the specified project. Must be invoked in EDT.
   * <p>
   * The created editor must be disposed after use by calling {@link #releaseEditor(Editor)}.
   * </p>
   *
   * @param document the document to create the editor for.
   * @param project  the project for which highlighter should be created
   * @param file     the file according to which the editor contents is highlighted.
   * @param isViewer true if read-only editor should be created
   * @return the editor instance.
   * @see Editor#getProject()
   */
  public abstract Editor createEditor(@Nonnull Document document, Project project, @Nonnull VirtualFile file, boolean isViewer);

  /**
   * Does the same as {@link #createEditor(Document, Project, VirtualFile, boolean)} and also sets the special kind for the created editor
   */
  public abstract Editor createEditor(@Nonnull Document document, Project project, @Nonnull VirtualFile file, boolean isViewer, @Nonnull EditorKind kind);

  /**
   * Creates a read-only editor for the specified document associated with the specified project. Must be invoked in EDT.
   * <p>
   * The created editor must be disposed after use by calling {@link #releaseEditor(Editor)}
   * </p>
   */
  public abstract Editor createViewer(@Nonnull Document document, @Nullable Project project);

  /**
   * Does the same as {@link #createViewer(Document, Project)} and also sets the special kind for the created viewer
   */
  public abstract Editor createViewer(@Nonnull Document document, @Nullable Project project, @Nonnull EditorKind kind);

  /**
   * Disposes the specified editor instance. Must be invoked in EDT.
   */
  public abstract void releaseEditor(@Nonnull Editor editor);

  /**
   * Returns the list of editors for the specified document associated with the specified project.
   *
   * @param document the document for which editors are requested.
   * @param project  the project with which editors should be associated, or null if any editors
   *                 for this document should be returned.
   */
  @Nonnull
  public abstract Editor[] getEditors(@Nonnull Document document, @Nullable Project project);

  /**
   * Returns the list of all editors for the specified document.
   */
  @Nonnull
  public Editor[] getEditors(@Nonnull Document document) {
    return getEditors(document, null);
  }

  /**
   * Returns the list of all currently open editors.
   */
  @Nonnull
  public abstract Editor[] getAllEditors();

  /**
   * Registers a listener for receiving notifications when editor instances are created
   * and released.
   *
   * @deprecated use the {@link #addEditorFactoryListener(EditorFactoryListener, Disposable)} instead
   */
  @Deprecated
  public abstract void addEditorFactoryListener(@Nonnull EditorFactoryListener listener);

  /**
   * Registers a listener for receiving notifications when editor instances are created and released
   * and removes the listener when the {@code parentDisposable} gets disposed.
   */
  public abstract void addEditorFactoryListener(@Nonnull EditorFactoryListener listener, @Nonnull Disposable parentDisposable);

  /**
   * Un-registers a listener for receiving notifications when editor instances are created
   * and released.
   *
   * @deprecated you should have used the {@link #addEditorFactoryListener(EditorFactoryListener, Disposable)} instead
   */
  @Deprecated
  public abstract void removeEditorFactoryListener(@Nonnull EditorFactoryListener listener);

  /**
   * Returns the service for attaching event listeners to all editor instances.
   */
  @Nonnull
  public abstract EditorEventMulticaster getEventMulticaster();

  /**
   * Reloads the editor settings and refreshes all currently open editors.
   */
  public abstract void refreshAllEditors();
}
