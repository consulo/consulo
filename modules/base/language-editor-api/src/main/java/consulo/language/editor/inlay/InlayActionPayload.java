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

/**
 * @author VISTALL
 * @since 2025-05-27
 */
public sealed interface InlayActionPayload {
    public final class PsiPointerInlayActionPayload implements InlayActionPayload {
        private final SmartPsiElementPointer<?> pointer;

        public PsiPointerInlayActionPayload(SmartPsiElementPointer<?> pointer) {
            this.pointer = pointer;
        }

        public SmartPsiElementPointer<?> getPointer() {
            return pointer;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof PsiPointerInlayActionPayload)) {
                return false;
            }
            PsiPointerInlayActionPayload that = (PsiPointerInlayActionPayload) other;
            return pointer.equals(that.pointer);
        }

        @Override
        public int hashCode() {
            return pointer.hashCode();
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
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof StringInlayActionPayload)) {
                return false;
            }
            StringInlayActionPayload that = (StringInlayActionPayload) other;
            return text.equals(that.text);
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
