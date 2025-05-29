// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor;

import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.localize.LocalizeValue;

public enum TabCharacterPaintMode {
    LONG_ARROW(CodeEditorLocalize.radioEditorTabLongArrow()),
    ARROW(CodeEditorLocalize.radioEditorTabArrow()),
    HORIZONTAL_LINE(CodeEditorLocalize.radioEditorTabHorizontalLine());

    private final LocalizeValue myText;

    TabCharacterPaintMode(LocalizeValue text) {
        myText = text;
    }

    public LocalizeValue getText() {
        return myText;
    }
}
