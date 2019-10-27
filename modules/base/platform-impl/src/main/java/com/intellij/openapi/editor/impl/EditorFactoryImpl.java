/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.editor.impl;

import com.intellij.injected.editor.DocumentWindow;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityStateListener;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.actionSystem.*;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.EditorEventMulticaster;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.editor.impl.event.EditorEventMulticasterImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.impl.ProjectLifecycleListener;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.EventDispatcher;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.text.CharArrayCharSequence;
import consulo.logging.Logger;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class EditorFactoryImpl extends EditorFactory {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.editor.impl.EditorFactoryImpl");
  private final EditorEventMulticasterImpl myEditorEventMulticaster = new EditorEventMulticasterImpl();
  private final EventDispatcher<EditorFactoryListener> myEditorFactoryEventDispatcher = EventDispatcher.create(EditorFactoryListener.class);
  private final List<Editor> myEditors = ContainerUtil.createLockFreeCopyOnWriteList();

  @Inject
  public EditorFactoryImpl(EditorActionManager editorActionManager) {
    Application application = ApplicationManager.getApplication();
    MessageBus bus = application.getMessageBus();
    MessageBusConnection connect = bus.connect();
    connect.subscribe(ProjectLifecycleListener.TOPIC, new ProjectLifecycleListener() {
      @Override
      public void beforeProjectLoaded(@Nonnull final Project project) {
        // validate all editors are disposed after fireProjectClosed() was called, because it's the place where editor should be released
        Disposer.register(project, () -> {
          final Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
          final boolean isLastProjectClosed = openProjects.length == 0;
          validateEditorsAreReleased(project, isLastProjectClosed);
        });
      }
    });

    ApplicationManager.getApplication().getMessageBus().connect().subscribe(EditorColorsManager.TOPIC, new EditorColorsListener() {
      @Override
      public void globalSchemeChange(EditorColorsScheme scheme) {
        refreshAllEditors();
      }
    });
    TypedAction typedAction = editorActionManager.getTypedAction();
    TypedActionHandler originalHandler = typedAction.getRawHandler();
    typedAction.setupRawHandler(new MyTypedHandler(originalHandler));

    ModalityStateListener myModalityStateListener = entering -> {
      for (Editor editor : myEditors) {
        ((DesktopEditorImpl)editor).beforeModalityStateChanged();
      }
    };
    LaterInvocator.addModalityStateListener(myModalityStateListener, ApplicationManager.getApplication());

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

  @NonNls
  public static void throwNotReleasedError(@Nonnull Editor editor) {
    if (editor instanceof DesktopEditorImpl) {
      ((DesktopEditorImpl)editor).throwEditorNotDisposedError("Editor of " + editor.getClass() + " hasn't been released:");
    }
    else {
      throw new RuntimeException("Editor of " + editor.getClass() +
                                 " and the following text hasn't been released:\n" + editor.getDocument().getText());
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
    return createEditor(document, false, null, EditorKind.MAIN_EDITOR);
  }

  @Override
  public Editor createViewer(@Nonnull Document document) {
    return createEditor(document, true, null, EditorKind.MAIN_EDITOR);
  }

  @Override
  public Editor createEditor(@Nonnull Document document, Project project) {
    return createEditor(document, false, project, EditorKind.MAIN_EDITOR);
  }

  @Override
  public Editor createViewer(@Nonnull Document document, Project project) {
    return createEditor(document, true, project, EditorKind.MAIN_EDITOR);
  }

  @Override
  public Editor createViewer(@Nonnull Document document, @Nullable Project project, @Nonnull EditorKind editorKind) {
    return createEditor(document, true, project, editorKind);
  }

  @Override
  public Editor createEditor(@Nonnull final Document document, final Project project, @Nonnull final FileType fileType, final boolean isViewer) {
    Editor editor = createEditor(document, isViewer, project, EditorKind.MAIN_EDITOR);
    ((EditorEx)editor).setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType));
    return editor;
  }

  @Override
  public Editor createEditor(@Nonnull Document document, Project project, @Nonnull VirtualFile file, boolean isViewer) {
    Editor editor = createEditor(document, isViewer, project, EditorKind.MAIN_EDITOR);
    ((EditorEx)editor).setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file));
    return editor;
  }

  private Editor createEditor(@Nonnull Document document, boolean isViewer, Project project, EditorKind editorKind) {
    Document hostDocument = document instanceof DocumentWindow ? ((DocumentWindow)document).getDelegate() : document;
    DesktopEditorImpl editor = new DesktopEditorImpl(hostDocument, isViewer, project, editorKind);
    myEditors.add(editor);
    myEditorEventMulticaster.registerEditor(editor);
    myEditorFactoryEventDispatcher.getMulticaster().editorCreated(new EditorFactoryEvent(this, editor));

    if (LOG.isDebugEnabled()) {
      LOG.debug("number of Editors after create: " + myEditors.size());
    }

    return editor;
  }

  @Override
  public void releaseEditor(@Nonnull Editor editor) {
    try {
      myEditorFactoryEventDispatcher.getMulticaster().editorReleased(new EditorFactoryEvent(this, editor));
    }
    finally {
      try {
        ((DesktopEditorImpl)editor).release();
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
      Project project1 = editor.getProject();
      if (editor.getDocument().equals(document) && (project == null || project1 == null || project1.equals(project))) {
        if (list == null) list = new SmartList<>();
        list.add(editor);
      }
    }
    return list == null ? Editor.EMPTY_ARRAY : list.toArray(new Editor[list.size()]);
  }

  @Override
  @Nonnull
  public Editor[] getEditors(@Nonnull Document document) {
    return getEditors(document, null);
  }

  @Override
  @Nonnull
  public Editor[] getAllEditors() {
    return ArrayUtil.stripTrailingNulls(myEditors.toArray(new Editor[myEditors.size()]));
  }

  @Override
  @Deprecated
  public void addEditorFactoryListener(@Nonnull EditorFactoryListener listener) {
    myEditorFactoryEventDispatcher.addListener(listener);
  }

  @Override
  public void addEditorFactoryListener(@Nonnull EditorFactoryListener listener, @Nonnull Disposable parentDisposable) {
    myEditorFactoryEventDispatcher.addListener(listener,parentDisposable);
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

  private static class MyTypedHandler implements TypedActionHandlerEx {
    private final TypedActionHandler myDelegate;

    private MyTypedHandler(TypedActionHandler delegate) {
      myDelegate = delegate;
    }

    @Override
    public void execute(@Nonnull Editor editor, char charTyped, @Nonnull DataContext dataContext) {
      editor.putUserData(DesktopEditorImpl.DISABLE_CARET_SHIFT_ON_WHITESPACE_INSERTION, Boolean.TRUE);
      try {
        myDelegate.execute(editor, charTyped, dataContext);
      }
      finally {
        editor.putUserData(DesktopEditorImpl.DISABLE_CARET_SHIFT_ON_WHITESPACE_INSERTION, null);
      }
    }

    @Override
    public void beforeExecute(@Nonnull Editor editor, char c, @Nonnull DataContext context, @Nonnull ActionPlan plan) {
      if (myDelegate instanceof TypedActionHandlerEx) ((TypedActionHandlerEx)myDelegate).beforeExecute(editor, c, context, plan);
    }
  }
}
