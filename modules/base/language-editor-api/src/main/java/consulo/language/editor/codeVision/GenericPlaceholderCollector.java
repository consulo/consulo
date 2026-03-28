// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.codeVision;

import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;

import java.util.List;

public interface GenericPlaceholderCollector extends CodeVisionPlaceholderCollector {
    List<TextRange> collectPlaceholders(Editor editor);
}
