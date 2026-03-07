// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview.timeline.comment;

import consulo.codeEditor.EditorEx;
import consulo.dataContext.DataSink;
import consulo.document.Document;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

final class CommentTextField extends EditorTextField {
    CommentTextField(@Nullable Project project, @Nonnull Document document) {
        super(document, project, FileTypes.PLAIN_TEXT);
        setOneLineMode(false);
    }

    @Override
    protected void updateBorder(@Nonnull EditorEx editor) {
        setupBorder(editor);
    }

    @Override
    protected @Nonnull EditorEx createEditor() {
        EditorEx editor = super.createEditor();
        editor.getComponent().setOpaque(false);
        editor.getScrollPane().setOpaque(false);
        return editor;
    }

    @Override
    public void uiDataSnapshot(@Nonnull DataSink sink) {
        super.uiDataSnapshot(sink);
        var editor = getEditor();
        if (editor == null) return;
        sink.set(PlatformCoreDataKeys.FILE_EDITOR, TextEditorProvider.getInstance().getTextEditor(editor));
    }
}
