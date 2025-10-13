/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.codeEditor;

import consulo.annotation.UsedInPlugin;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.TextAttributesKey;

@UsedInPlugin
public interface CodeInsightColors {
    TextAttributesKey WRONG_REFERENCES_ATTRIBUTES = TextAttributesKey.of("WRONG_REFERENCES_ATTRIBUTES");
    TextAttributesKey ERRORS_ATTRIBUTES = TextAttributesKey.of("ERRORS_ATTRIBUTES");
    TextAttributesKey WARNINGS_ATTRIBUTES = TextAttributesKey.of("WARNING_ATTRIBUTES");
    TextAttributesKey GENERIC_SERVER_ERROR_OR_WARNING = TextAttributesKey.of("GENERIC_SERVER_ERROR_OR_WARNING");
    TextAttributesKey DUPLICATE_FROM_SERVER = TextAttributesKey.of("DUPLICATE_FROM_SERVER");
    /**
     * use #WEAK_WARNING_ATTRIBUTES instead
     */
    @Deprecated
    TextAttributesKey INFO_ATTRIBUTES = TextAttributesKey.of("INFO_ATTRIBUTES");
    TextAttributesKey WEAK_WARNING_ATTRIBUTES = TextAttributesKey.of("INFO_ATTRIBUTES");
    TextAttributesKey INFORMATION_ATTRIBUTES = TextAttributesKey.of("INFORMATION_ATTRIBUTES");
    TextAttributesKey NOT_USED_ELEMENT_ATTRIBUTES = TextAttributesKey.of("NOT_USED_ELEMENT_ATTRIBUTES");
    TextAttributesKey DEPRECATED_ATTRIBUTES = TextAttributesKey.of("DEPRECATED_ATTRIBUTES");
    TextAttributesKey MARKED_FOR_REMOVAL_ATTRIBUTES = TextAttributesKey.of("MARKED_FOR_REMOVAL_ATTRIBUTES");

    TextAttributesKey MATCHED_BRACE_ATTRIBUTES = TextAttributesKey.of("MATCHED_BRACE_ATTRIBUTES");
    TextAttributesKey UNMATCHED_BRACE_ATTRIBUTES = TextAttributesKey.of("UNMATCHED_BRACE_ATTRIBUTES");

    TextAttributesKey JOIN_POINT = TextAttributesKey.of("JOIN_POINT");
    TextAttributesKey BLINKING_HIGHLIGHTS_ATTRIBUTES = TextAttributesKey.of("BLINKING_HIGHLIGHTS_ATTRIBUTES");
    TextAttributesKey HYPERLINK_ATTRIBUTES = TextAttributesKey.of("HYPERLINK_ATTRIBUTES");
    TextAttributesKey FOLLOWED_HYPERLINK_ATTRIBUTES = TextAttributesKey.of("FOLLOWED_HYPERLINK_ATTRIBUTES");

    TextAttributesKey TODO_DEFAULT_ATTRIBUTES = TextAttributesKey.of("TODO_DEFAULT_ATTRIBUTES");
    TextAttributesKey BOOKMARKS_ATTRIBUTES = TextAttributesKey.of("BOOKMARKS_ATTRIBUTES");

    // Colors
    EditorColorKey METHOD_SEPARATORS_COLOR = EditorColorKey.createColorKey("METHOD_SEPARATORS_COLOR");
    TextAttributesKey LINE_FULL_COVERAGE = TextAttributesKey.of("LINE_FULL_COVERAGE");
    TextAttributesKey LINE_PARTIAL_COVERAGE = TextAttributesKey.of("LINE_PARTIAL_COVERAGE");
    TextAttributesKey LINE_NONE_COVERAGE = TextAttributesKey.of("LINE_NONE_COVERAGE");
}
