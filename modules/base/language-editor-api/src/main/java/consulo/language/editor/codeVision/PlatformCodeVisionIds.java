// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.codeVision;

public enum PlatformCodeVisionIds {
    USAGES("references"),
    INHERITORS("inheritors"),
    PROBLEMS("problems"),
    RENAME("rename"),
    CHANGE_SIGNATURE("change.signature");

    private final String key;

    PlatformCodeVisionIds(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
