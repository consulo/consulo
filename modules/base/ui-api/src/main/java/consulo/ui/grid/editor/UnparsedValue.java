// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid.editor;

import org.jspecify.annotations.Nullable;

public class UnparsedValue {
    private final String myText;
    private final @Nullable ParsingError myError;

    public UnparsedValue(String text, String n) {
        this(text);
    }

    public UnparsedValue(String text) {
        this(text, (ParsingError) null);
    }

    public UnparsedValue(String text, @Nullable ParsingError error) {
        myText = text;
        myError = error;
    }

    public String getText() {
        return myText;
    }

    public @Nullable ParsingError getError() {
        return myError;
    }

    @Override
    public String toString() {
        return myText;
    }

    public record ParsingError(String message, int offset) {
        public ParsingError(String message) {
            this(message, 0);
        }
    }
}
