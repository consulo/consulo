// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.documentation.render;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

public final class DocRenderManager {
    private static final Key<Boolean> DOC_RENDER_ENABLED = Key.create("doc.render.enabled");

    /**
     * Allows overriding global doc comments rendering setting for a specific editor. Passing {@code null} as {@code value} makes editor use
     * the global setting again.
     */
    @RequiredUIAccess
    public static void setDocRenderingEnabled(Editor editor, @Nullable Boolean value) {
        boolean enabledBefore = isDocRenderingEnabled(editor);
        editor.putUserData(DOC_RENDER_ENABLED, value);
        boolean enabledAfter = isDocRenderingEnabled(editor);
        if (enabledAfter != enabledBefore) {
            resetEditorToDefaultState(editor);
        }
    }

    /**
     * Tells whether doc comment rendering is enabled for a specific editor.
     *
     * @see #setDocRenderingEnabled(Editor, Boolean)
     */
    public static boolean isDocRenderingEnabled(Editor editor) {
        if (editor.getEditorKind() == EditorKind.DIFF) {
            return false;
        }
        Boolean value = editor.getUserData(DOC_RENDER_ENABLED);
        return value == null ? EditorSettingsExternalizable.getInstance().isDocCommentRenderingEnabled() : value;
    }

    /**
     * Sets all doc comments to their default state (rendered or not rendered) in the specified editor.
     *
     * @see #isDocRenderingEnabled(Editor)
     */
    @RequiredUIAccess
    public static void resetEditorToDefaultState(Editor editor) {
        DocRenderItemManager.getInstance().resetToDefaultState(editor);
        DocRenderPassFactory.forceRefreshOnNextPass(editor);
        Project project = editor.getProject();
        if (project != null) {
            DaemonCodeAnalyzer.getInstance(project).restart();
        }
    }
}
