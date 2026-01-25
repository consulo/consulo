// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inject;

import consulo.util.dataholder.Key;

public final class InjectionMeta {

    private static final Key<String> INJECTION_INDENT = Key.create("INJECTION_INDENT");

    public static Key<String> getInjectionIndent() {
        return INJECTION_INDENT;
    }

    private InjectionMeta() {
    }
}
