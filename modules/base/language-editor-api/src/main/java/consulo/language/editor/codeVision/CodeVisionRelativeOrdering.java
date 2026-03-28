// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.codeVision;

public abstract class CodeVisionRelativeOrdering {
    private CodeVisionRelativeOrdering() {
    }

    public static final class CodeVisionRelativeOrderingAfter extends CodeVisionRelativeOrdering {
        private final String id;

        public CodeVisionRelativeOrderingAfter(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static final class CodeVisionRelativeOrderingBefore extends CodeVisionRelativeOrdering {
        private final String id;

        public CodeVisionRelativeOrderingBefore(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static final class CodeVisionRelativeOrderingFirst extends CodeVisionRelativeOrdering {
        public static final CodeVisionRelativeOrderingFirst INSTANCE = new CodeVisionRelativeOrderingFirst();

        private CodeVisionRelativeOrderingFirst() {
        }
    }

    public static final class CodeVisionRelativeOrderingLast extends CodeVisionRelativeOrdering {
        public static final CodeVisionRelativeOrderingLast INSTANCE = new CodeVisionRelativeOrderingLast();

        private CodeVisionRelativeOrderingLast() {
        }
    }
}
