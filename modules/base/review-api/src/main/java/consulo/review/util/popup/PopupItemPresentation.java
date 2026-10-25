// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.review.util.popup;

import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public interface PopupItemPresentation {
    String getShortText();

    @Nullable
    Image getIcon();

    @Nullable
    String getFullText();

    record Simple(String shortText, @Nullable Image icon, @Nullable String fullText) implements PopupItemPresentation {
        public Simple(String shortText) {
            this(shortText, null, null);
        }

        public Simple(String shortText, @Nullable Image icon) {
            this(shortText, icon, null);
        }

        @Override
        public String getShortText() {
            return shortText;
        }

        @Override
        public @Nullable Image getIcon() {
            return icon;
        }

        @Override
        public @Nullable String getFullText() {
            return fullText;
        }
    }

    final class ToString implements PopupItemPresentation {
        private final String myShortText;

        public ToString(Object value) {
            myShortText = value.toString();
        }

        @Override
        public String getShortText() {
            return myShortText;
        }

        @Override
        public @Nullable Image getIcon() {
            return null;
        }

        @Override
        public @Nullable String getFullText() {
            return null;
        }
    }
}
