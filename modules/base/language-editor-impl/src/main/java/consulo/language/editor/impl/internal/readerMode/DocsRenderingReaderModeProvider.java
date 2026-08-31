// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.readerMode;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.language.editor.impl.internal.documentation.render.DocRenderManager;
import consulo.language.editor.readerMode.ReaderModeProvider;
import consulo.language.editor.readerMode.ReaderModeSettings;
import consulo.project.Project;

@ExtensionImpl
public class DocsRenderingReaderModeProvider implements ReaderModeProvider {
    @Override
    public void applyModeChanged(Project project, Editor editor, boolean readerMode, boolean fileIsOpenAlready) {
        DocRenderManager.setDocRenderingEnabled(
            editor,
            readerMode
                ? ReaderModeSettings.getInstance(project).isShowRenderedDocs()
                : EditorSettingsExternalizable.getInstance().isDocCommentRenderingEnabled()
        );
    }
}
