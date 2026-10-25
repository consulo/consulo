// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff;

import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.fileEditor.FileEditorState;
import consulo.fileEditor.FileEditorStateLevel;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface DiffEditorViewer extends Disposable {
    boolean isDisposed();

    DiffContext getContext();

    Component getComponent();

    @Nullable Component getPreferredFocusedComponent();

    default List<VirtualFile> getFilesToRefresh() {
        return List.of();
    }

    default List<? extends Editor> getEmbeddedEditors() {
        return List.of();
    }

    @RequiredUIAccess
    void fireProcessorActivated();

    default FileEditorState getState(FileEditorStateLevel level) {
        return FileEditorState.INSTANCE;
    }

    @RequiredUIAccess
    default void setState(FileEditorState state) {
    }
}
