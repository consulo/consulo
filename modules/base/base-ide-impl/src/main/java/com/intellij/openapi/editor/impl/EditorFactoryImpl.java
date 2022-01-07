// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.impl;

import com.intellij.injected.editor.DocumentWindow;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.actionSystem.ActionPlan;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.editor.actionSystem.TypedActionHandlerEx;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.EditorEventMulticaster;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.editor.impl.event.EditorEventMulticasterImpl;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.EventDispatcher;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.text.CharArrayCharSequence;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.editor.internal.EditorInternal;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class EditorFactoryImpl extends EditorFactory {
  private static final Logger LOG = Logger.getInstance(EditorFactoryImpl.class);

  private static final ExtensionPointName<EditorFactoryListener> EP = ExtensionPointName.create("com.intellij.editorFactoryListener");

  private final EditorEventMulticasterImpl myEditorEventMulticaster = new EditorEventMulticasterImpl();
  private final EventDispatcher<EditorFactoryListener> myEditorFactoryEventDispatcher = EventDispatcher.create(EditorFactoryListener.class);
  protected final List<Editor> myEditors = ContainerUtil.createLockFreeCopyOnWriteList();

  @Inject
  public EditorFactoryImpl(Application application) {
    MessageBusConnection busConnection = application.getMessageBus().connect();
    busConnection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectClosed(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        // validate all editors are disposed after fireProjectClosed() was called, because it's the place where editor should be released
        Disposer.register(project, () -> {
          Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
          boolean isLastProjectClosed = openProjects.length == 0;
          // EditorTextField.releaseEditorLater defer releasing its editor; invokeLater to avoid false positives about such editors.
          application.invokeLater(() -> validateEditorsAreReleased(project, isLastProjectClosed));
        });
      }
    });
    busConnection.subscribe(EditorColorsManager.TOPIC, scheme -> refreshAllEditors());
  }

  @Nonnull
  protected abstract EditorInternal createEditorImpl(@Nonnull Document document, boolean isViewer, Project project, @Nonnull EditorKind kind);

  protected final Editor createEditor(@Nonnull Document document, boolean isViewer, Project project, @Nonnull EditorKind kind) {
    Document hostDocument = document instanceof DocumentWindow ? ((DocumentWindow)document).getDelegate() : document;
    EditorInternal editor = createEditorImpl(hostDocument, isViewer, project, kind);
    myEditors.add(editor);
    myEditorEventMulticaster.registerEditor(editor);

    EditorFactoryEvent event = new EditorFactoryEvent(this, editor);
    myEditorFactoryEventDispatcher.getMulticaster().editorCreated(event);
    EP.forEachExtensionSafe(it -> it.editorCreated(event));

    if (LOG.isDebugEnabled()) {
      LOG.debug("number of Editors after create: " + myEditors.size());
    }

    return editor;
  }

  public void validateEditorsAreReleased(Project project, boolean isLastProjectClosed) {
    for (final Editor editor : myEditors) {
      if (editor.getProject() == project || editor.getProject() == null && isLastProjectClosed) {
        try {
          throwNotReleasedError(editor);
        }
        finally {
          releaseEditor(editor);
        }
      }
    }
  }

  public static void throwNotReleasedError(@Nonnull Editor editor) {
    if (editor instanceof EditorInternal) {
      ((EditorInternal)editor).throwEditorNotDisposedError("Editor of " + editor.getClass() + " hasn't been released:");
    }
    else {
      throw new RuntimeException("Editor of " + editor.getClass() + " and the following text hasn't been released:\n" + editor.getDocument().getText());
    }
  }

  @Override
  @Nonnull
  public Document createDocument(@Nonnull char[] text) {
    return createDocument(new CharArrayCharSequence(text));
  }

  @Override
  @Nonnull
  public Document createDocument(@Nonnull CharSequence text) {
    DocumentEx document = new DocumentImpl(text);
    myEditorEventMulticaster.registerDocument(document);
    return document;
  }

  @Nonnull
  public Document createDocument(boolean allowUpdatesWithoutWriteAction) {
    DocumentEx document = new DocumentImpl("", allowUpdatesWithoutWriteAction);
    myEditorEventMulticaster.registerDocument(document);
    return document;
  }

  @Nonnull
  public Document createDocument(@Nonnull CharSequence text, boolean acceptsSlashR, boolean allowUpdatesWithoutWriteAction) {
    DocumentEx document = new DocumentImpl(text, acceptsSlashR, allowUpdatesWithoutWriteAction);
    myEditorEventMulticaster.registerDocument(document);
    return document;
  }

  @Override
  public void refreshAllEditors() {
    for (Editor editor : myEditors) {
      ((EditorEx)editor).reinitSettings();
    }
  }

  @Override
  public Editor createEditor(@Nonnull Document document) {
    return createEditor(document, false, null, EditorKind.UNTYPED);
  }

  @Override
  public Editor createViewer(@Nonnull Document document) {
    return createEditor(document, true, null, EditorKind.UNTYPED);
  }

  @Override
  public Editor createEditor(@Nonnull Document document, Project project) {
    return createEditor(document, false, project, EditorKind.UNTYPED);
  }

  @Override
  public Editor createEditor(@Nonnull Document document, @Nullable Project project, @Nonnull EditorKind kind) {
    return createEditor(document, false, project, kind);
  }

  @Override
  public Editor createViewer(@Nonnull Document document, Project project) {
    return createEditor(document, true, project, EditorKind.UNTYPED);
  }

  @Override
  public Editor createViewer(@Nonnull Document document, @Nullable Project project, @Nonnull EditorKind kind) {
    return createEditor(document, true, project, kind);
  }

  @Override
  public Editor createEditor(@Nonnull final Document document, final Project project, @Nonnull final FileType fileType, final boolean isViewer) {
    Editor editor = createEditor(document, isViewer, project, EditorKind.UNTYPED);
    ((EditorEx)editor).setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType));
    return editor;
  }

  @Override
  public Editor createEditor(@Nonnull Document document, Project project, @Nonnull VirtualFile file, boolean isViewer) {
    Editor editor = createEditor(document, isViewer, project, EditorKind.UNTYPED);
    ((EditorEx)editor).setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file));
    return editor;
  }

  @Override
  public Editor createEditor(@Nonnull Document document, Project project, @Nonnull VirtualFile file, boolean isViewer, @Nonnull EditorKind kind) {
    Editor editor = createEditor(document, isViewer, project, kind);
    ((EditorEx)editor).setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file));
    return editor;
  }



  @Override
  public void releaseEditor(@Nonnull Editor editor) {
    try {
      EditorFactoryEvent event = new EditorFactoryEvent(this, editor);
      myEditorFactoryEventDispatcher.getMulticaster().editorReleased(event);
      EP.forEachExtensionSafe(it -> it.editorReleased(event));
    }
    finally {
      try {
        ((EditorInternal)editor).release();
      }
      finally {
        myEditors.remove(editor);
        if (LOG.isDebugEnabled()) {
          LOG.debug("number of Editors after release: " + myEditors.size());
        }
      }
    }
  }

  @Override
  @Nonnull
  public Editor[] getEditors(@Nonnull Document document, Project project) {
    List<Editor> list = null;
    for (Editor editor : myEditors) {
      if (editor.getDocument().equals(document) && (project == null || project.equals(editor.getProject()))) {
        if (list == null) list = new SmartList<>();
        list.add(editor);
      }
    }
    return list == null ? Editor.EMPTY_ARRAY : list.toArray(Editor.EMPTY_ARRAY);
  }

  @Override
  @Nonnull
  public Editor[] getAllEditors() {
    return myEditors.toArray(Editor.EMPTY_ARRAY);
  }

  @Override
  @Deprecated
  public void addEditorFactoryListener(@Nonnull EditorFactoryListener listener) {
    myEditorFactoryEventDispatcher.addListener(listener);
  }

  @Override
  public void addEditorFactoryListener(@Nonnull EditorFactoryListener listener, @Nonnull Disposable parentDisposable) {
    myEditorFactoryEventDispatcher.addListener(listener, parentDisposable);
  }

  @Override
  @Deprecated
  public void removeEditorFactoryListener(@Nonnull EditorFactoryListener listener) {
    myEditorFactoryEventDispatcher.removeListener(listener);
  }

  @Override
  @Nonnull
  public EditorEventMulticaster getEventMulticaster() {
    return myEditorEventMulticaster;
  }

  public static class MyRawTypedHandler implements TypedActionHandlerEx {
    private final TypedActionHandler myDelegate;

    @SuppressWarnings("NonDefaultConstructor")
    public MyRawTypedHandler(TypedActionHandler delegate) {
      myDelegate = delegate;
    }

    @Override
    public void execute(@Nonnull Editor editor, char charTyped, @Nonnull DataContext dataContext) {
      editor.putUserData(EditorEx.DISABLE_CARET_SHIFT_ON_WHITESPACE_INSERTION, Boolean.TRUE);
      try {
        myDelegate.execute(editor, charTyped, dataContext);
      }
      finally {
        editor.putUserData(EditorEx.DISABLE_CARET_SHIFT_ON_WHITESPACE_INSERTION, null);
      }
    }

    @Override
    public void beforeExecute(@Nonnull Editor editor, char c, @Nonnull DataContext context, @Nonnull ActionPlan plan) {
      if (myDelegate instanceof TypedActionHandlerEx) ((TypedActionHandlerEx)myDelegate).beforeExecute(editor, c, context, plan);
    }
  }
}
