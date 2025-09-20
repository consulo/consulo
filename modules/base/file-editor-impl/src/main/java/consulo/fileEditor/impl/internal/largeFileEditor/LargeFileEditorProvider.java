// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorPolicy;
import consulo.fileEditor.FileEditorProvider;
import consulo.fileEditor.FileEditorState;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.project.Project;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.RawFileLoaderHelper;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

@ExtensionImpl(id = LargeFileEditorProvider.PROVIDER_ID)
public final class LargeFileEditorProvider implements FileEditorProvider, DumbAware {
    public static final String PROVIDER_ID = "LargeFileEditorProvider";

    private static final @NonNls String CARET_PAGE_NUMBER_ATTR = "caret-page-number";
    private static final @NonNls String CARET_PAGE_SYMBOL_OFFSET_ATTR = "caret-page-symbol-offset";

    @Override
    public boolean accept(@Nonnull Project project, @Nonnull VirtualFile file) {
        return TextEditorProvider.isTextFile(file)
            && RawFileLoaderHelper.isTooLargeForContentLoading(file)
            && !file.getFileType().isBinary()
            && file.isInLocalFileSystem();
    }

    @Override
    public @Nonnull FileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile file) {
        return new LargeFileEditorImpl(project, file);
    }

    @Override
    public @Nonnull String getEditorTypeId() {
        return PROVIDER_ID;
    }

    @Override
    public @Nonnull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }

    @Override
    public void writeState(@Nonnull FileEditorState state, @Nonnull Project project, @Nonnull Element targetElement) {
        if (state instanceof LargeFileEditorState) {
            targetElement.setAttribute(CARET_PAGE_NUMBER_ATTR,
                Long.toString(((LargeFileEditorState) state).caretPageNumber));
            targetElement.setAttribute(CARET_PAGE_SYMBOL_OFFSET_ATTR,
                Integer.toString(((LargeFileEditorState) state).caretSymbolOffsetInPage));
        }
    }

    @Override
    public @Nonnull FileEditorState readState(@Nonnull Element sourceElement, @Nonnull Project project, @Nonnull VirtualFile file) {
        LargeFileEditorState state = new LargeFileEditorState();
        if (JDOMUtil.isEmpty(sourceElement)) {
            return state;
        }
        state.caretPageNumber = StringUtil.parseLong(sourceElement.getAttributeValue(CARET_PAGE_NUMBER_ATTR), 0);
        state.caretSymbolOffsetInPage = StringUtil.parseInt(sourceElement.getAttributeValue(CARET_PAGE_SYMBOL_OFFSET_ATTR), 0);
        return state;
    }
}
