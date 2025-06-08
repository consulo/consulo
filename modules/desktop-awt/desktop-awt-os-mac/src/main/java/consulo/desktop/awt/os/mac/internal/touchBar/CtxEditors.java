// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.os.mac.internal.touchBar;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.logging.Logger;
import consulo.ui.ex.action.ActionGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

final class CtxEditors {
    private static Map<Editor, WeakReference<Component>> ourEditors = null;
    private static Map<Long, ActionGroup> ourEditorSearchActions = null;
    private static Customizer ourCustomizer = null;

    private static void initialize() {
        if (ourEditors != null)
            return;

        ourEditors = new WeakHashMap<>();

        ourEditorSearchActions = ActionsLoader.getActionGroup("EditorSearch");
        if (ourEditorSearchActions == null) {
            Logger.getInstance(CtxEditors.class).debug("null action group for editor-search");
            return;
        }

        EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
            @Override
            public void editorReleased(@Nonnull EditorFactoryEvent event) {
                final WeakReference<Component> cmpRef = ourEditors.remove(event.getEditor());
                final Component cmp = cmpRef != null ? cmpRef.get() : null;
                if (cmp != null) {
                    TouchBarsManager.unregister(cmp);
                }
            }
        }, ApplicationManager.getApplication());

        ourCustomizer = new Customizer(new TBPanel.CrossEscInfo(false, false)/*always replace esc for editor search*/, null);
    }

    static void onUpdateEditorHeader(@Nonnull Editor editor) {
        initialize();
        if (ourEditorSearchActions == null) {
            return;
        }

        // register editor
        final @Nullable Component newCmp = editor.getHeaderComponent();
        final @Nullable WeakReference<Component> oldCmpRef = ourEditors.put(editor, new WeakReference<>(newCmp));
        final @Nullable Component oldCmp = oldCmpRef != null ? oldCmpRef.get() : null;
        if (oldCmp != null) {
            TouchBarsManager.unregister(oldCmp);
        }
        if (newCmp != null) {
            TouchBarsManager.register(newCmp, ourEditorSearchActions, ourCustomizer);
        }
    }
}
