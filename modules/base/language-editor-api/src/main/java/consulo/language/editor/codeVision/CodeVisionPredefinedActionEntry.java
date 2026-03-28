// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.codeVision;

import consulo.codeEditor.Editor;

/**
 * Implemented by {@link CodeVisionEntry} when click should be processed by CodeVisionEntry itself,
 * not by the provider.
 *
 * @see DaemonBoundCodeVisionProvider#handleClick(Editor, consulo.document.util.TextRange, CodeVisionEntry)
 */
public interface CodeVisionPredefinedActionEntry {
    void onClick(Editor editor);
}
