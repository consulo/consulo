// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inlay;

public enum HintColorKind {
    Default,
    Parameter,
    TextWithoutBackground;

    public boolean hasBackground() {
        return this != TextWithoutBackground;
    }
}
