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
package consulo.ide.impl.idea.codeInsight.highlighting;

import consulo.codeEditor.markup.RangeHighlighter;
import consulo.util.dataholder.Key;

import java.util.Collection;

/**
 * @author VISTALL
 * @since 2025-04-09
 */
public record SelectionHighlights(String text, Collection<RangeHighlighter> highlighters) {
    public static final Key<SelectionHighlights> KEY = Key.create(SelectionHighlights.class);
}
