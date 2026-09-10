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
package consulo.diff.impl.internal;

import com.dslplatform.json.CompiledJson;
import consulo.diff.impl.internal.util.HighlightingLevel;
import consulo.diff.internal.HighlightPolicy;
import consulo.diff.internal.IgnorePolicy;

@CompiledJson
class TextDiffPlaceSettings {
    // Diff settings
    public HighlightPolicy HIGHLIGHT_POLICY = HighlightPolicy.BY_WORD;
    public IgnorePolicy IGNORE_POLICY = IgnorePolicy.DEFAULT;

    // Presentation settings
    public boolean ENABLE_SYNC_SCROLL = true;

    // Editor settings
    public boolean SHOW_WHITESPACES = false;
    public boolean SHOW_LINE_NUMBERS = true;
    public boolean SHOW_INDENT_LINES = false;
    public boolean USE_SOFT_WRAPS = false;
    public HighlightingLevel HIGHLIGHTING_LEVEL = HighlightingLevel.INSPECTIONS;
    public boolean READ_ONLY_LOCK = true;

    // Fragments settings
    public boolean EXPAND_BY_DEFAULT = true;
}
