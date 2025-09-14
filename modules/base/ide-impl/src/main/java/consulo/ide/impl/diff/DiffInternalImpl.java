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
package consulo.ide.impl.diff;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.diff.internal.DiffInternal;
import consulo.diff.util.TextDiffType;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * @author VISTALL
 * @since 2025-09-13
 */
@ServiceImpl
@Singleton
public class DiffInternalImpl implements DiffInternal {
    @Override
    public List<RangeHighlighter> createInlineHighlighter(@Nonnull Editor editor, int start, int end, @Nonnull TextDiffType type) {
        return DiffDrawUtil.createInlineHighlighter(editor, start, end, type);
    }
}
