// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url;

import org.jspecify.annotations.Nullable;

public sealed abstract class Authority permits Authority.Exact, Authority.Placeholder {
    private Authority() {
    }

    public static final class Exact extends Authority {
        private final String myText;

        public Exact(String text) {
            myText = text;
        }

        public String getText() {
            return myText;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Exact exact)) {
                return false;
            }
            return myText.equals(exact.myText);
        }

        @Override
        public int hashCode() {
            return myText.hashCode();
        }

        @Override
        public String toString() {
            return "Exact(text=" + myText + ")";
        }
    }

    public static non-sealed class Placeholder extends Authority {
        public Placeholder() {
        }
    }
}
