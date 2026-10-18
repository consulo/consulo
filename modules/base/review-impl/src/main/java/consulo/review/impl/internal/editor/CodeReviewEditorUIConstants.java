// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.impl.internal.editor;

import consulo.ui.Length;

/**
 * Sizing constants shared between the review-in-editor renderers and the chat item UI.
 * <p>
 * They live here rather than in {@code CodeReviewChatItemUIUtil} because the gutter and inlay renderers must be
 * loadable on a split-mode frontend, while the chat item UI is not.
 */
public final class CodeReviewEditorUIConstants {
    /**
     * Maximum width for textual content for it to be readable
     * Equals to 42em
     */
    public static final Length TEXT_CONTENT_WIDTH = Length.ofFont(36);

    public static final int THREAD_TOP_MARGIN = 8;

    private CodeReviewEditorUIConstants() {
    }
}
