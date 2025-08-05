// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.internal;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.RealEditor;
import consulo.codeEditor.event.EditorEventMulticaster;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.codeEditor.internal.InternalEditorFactory;
import consulo.colorScheme.event.EditorColorsListener;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.DocumentWindow;
import consulo.document.internal.DocumentEx;
import consulo.document.internal.DocumentFactory;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.ProjectManagerListener;
import consulo.proxy.EventDispatcher;
import consulo.ui.UIAccess;
import consulo.util.collection.Lists;
import consulo.util.collection.SmartList;
import consulo.util.lang.CharArrayCharSequence;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.List;

public abstract class EditorFactoryImpl extends InternalEditorFactory {
    private static final Logger LOG = Logger.getInstance(EditorFactoryImpl.class);

    private final EditorEventMulticasterImpl myEditorEventMulticaster = new EditorEventMulticasterImpl();
    private final EventDispatcher<EditorFactoryListener> myEditorFactoryEventDispatcher = EventDispatcher.create(EditorFactoryListener.class);
    protected final List<Editor> myEditors = Lists.newLockFreeCopyOnWriteList();

    @Nonnull
    private final Application myApplication;
    private final DocumentFactory myDocumentFactory;

    @Inject
    public EditorFactoryImpl(Application application, DocumentFactory documentFactory) {
        myApplication = application;
        myDocumentFactory = documentFactory;
        MessageBusConnection busConnection = application.getMessageBus().connect();
        busConnection.subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
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
        busConnection.subscribe(EditorColorsListener.class, scheme -> refreshAllEditors());
    }

    @Nonnull
    protected abstract RealEditor createEditorImpl(@Nonnull Document document, boolean isViewer, Project project, @Nonnull EditorKind kind);

    protected final Editor createEditor(@Nonnull Document document, boolean isViewer, Project project, @Nonnull EditorKind kind) {
        Document hostDocument = document instanceof DocumentWindow ? ((DocumentWindow) document).getDelegate() : document;
        RealEditor editor = createEditorImpl(hostDocument, isViewer, project, kind);
        myEditors.add(editor);
        myEditorEventMulticaster.registerEditor(editor);

        EditorFactoryEvent event = new EditorFactoryEvent(this, editor);
        myEditorFactoryEventDispatcher.getMulticaster().editorCreated(event);

        myApplication.getExtensionPoint(EditorFactoryListener.class).forEach(it -> it.editorCreated(event));

        if (LOG.isDebugEnabled()) {
            LOG.debug("number of Editors after create: " + myEditors.size());
        }

        return editor;
    }

    public void validateEditorsAreReleased(Project project, boolean isLastProjectClosed) {
        for (Editor editor : myEditors) {
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
        if (editor instanceof RealEditor) {
            ((RealEditor) editor).throwEditorNotDisposedError("Editor of " + editor.getClass() + " hasn't been released:");
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
        DocumentEx document = myDocumentFactory.createDocument(text);
        myEditorEventMulticaster.registerDocument(document);
        return document;
    }

    @Override
    @Nonnull
    public Document createDocument(boolean allowUpdatesWithoutWriteAction) {
        DocumentEx document = myDocumentFactory.createDocument("", allowUpdatesWithoutWriteAction);
        myEditorEventMulticaster.registerDocument(document);
        return document;
    }

    @Nonnull
    public Document createDocument(@Nonnull CharSequence text, boolean acceptsSlashR, boolean allowUpdatesWithoutWriteAction) {
        DocumentEx document = myDocumentFactory.createDocument(text, acceptsSlashR, allowUpdatesWithoutWriteAction);
        myEditorEventMulticaster.registerDocument(document);
        return document;
    }

    @Override
    public void refreshAllEditors() {
        for (Editor editor : myEditors) {
            ((EditorEx) editor).reinitSettings();
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
    public Editor createEditor(@Nonnull Document document, Project project, @Nonnull FileType fileType, boolean isViewer) {
        Editor editor = createEditor(document, isViewer, project, EditorKind.UNTYPED);
        ((EditorEx) editor).setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType));
        return editor;
    }

    @Override
    public Editor createEditor(@Nonnull Document document, Project project, @Nonnull VirtualFile file, boolean isViewer) {
        Editor editor = createEditor(document, isViewer, project, EditorKind.UNTYPED);
        ((EditorEx) editor).setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file));
        return editor;
    }

    @Override
    public Editor createEditor(@Nonnull Document document, Project project, @Nonnull VirtualFile file, boolean isViewer, @Nonnull EditorKind kind) {
        Editor editor = createEditor(document, isViewer, project, kind);
        ((EditorEx) editor).setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file));
        return editor;
    }


    @Override
    public void releaseEditor(@Nonnull Editor editor) {
        try {
            EditorFactoryEvent event = new EditorFactoryEvent(this, editor);
            myEditorFactoryEventDispatcher.getMulticaster().editorReleased(event);
            myApplication.getExtensionPoint(EditorFactoryListener.class).forEach(it -> it.editorReleased(event));
        }
        finally {
            try {
                ((RealEditor) editor).release();
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
                if (list == null) {
                    list = new SmartList<>();
                }
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
}
