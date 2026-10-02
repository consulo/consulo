/*
 * Copyright 2013-2025 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.editor.inlay;

import consulo.language.psi.SmartPsiElementPointer;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2025-05-27
 */
public sealed interface InlayActionPayload {
    public final class PsiPointerInlayActionPayload implements InlayActionPayload {
        private final SmartPsiElementPointer<?> pointer;
        private final @Nullable String tag;

        public PsiPointerInlayActionPayload(SmartPsiElementPointer<?> pointer) {
            this(pointer, null);
        }

        public PsiPointerInlayActionPayload(SmartPsiElementPointer<?> pointer, @Nullable String tag) {
            this.pointer = pointer;
            this.tag = tag;
        }

        public SmartPsiElementPointer<?> getPointer() {
            return pointer;
        }

        public @Nullable String getTag() {
            return tag;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return this == other
                || other instanceof PsiPointerInlayActionPayload that && pointer.equals(that.pointer) && Objects.equals(tag, that.tag);
        }

        @Override
        public int hashCode() {
            return 31 * pointer.hashCode() + Objects.hashCode(tag);
        }
    }

    public final class StringInlayActionPayload implements InlayActionPayload {
        private final String text;

        public StringInlayActionPayload(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return this == other
                || other instanceof StringInlayActionPayload that && text.equals(that.text);
        }

        @Override
        public int hashCode() {
            return text.hashCode();
        }

        @Override
        public String toString() {
            return "StringInlayActionPayload(text='" + text + "')";
        }
    }
}
