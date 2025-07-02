/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.debug.ui;

import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.TextAttributesKey;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.ui.color.RGBColor;
import consulo.util.dataholder.Key;

/**
 * @author Yura Cangea
 */
public interface DebuggerColors {
  TextAttributesKey BREAKPOINT_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("BREAKPOINT_ATTRIBUTES");
  TextAttributesKey EXECUTIONPOINT_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("EXECUTIONPOINT_ATTRIBUTES");
  TextAttributesKey NOT_TOP_FRAME_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("NOT_TOP_FRAME_ATTRIBUTES");
  EditorColorKey RECURSIVE_CALL_ATTRIBUTES = EditorColorKey.createColorKey("RECURSIVE_CALL_ATTRIBUTES", new RGBColor(255, 255, 215));

  int BREAKPOINT_HIGHLIGHTER_LAYER = HighlighterLayer.CARET_ROW + 1;

  Key<Boolean> BREAKPOINT_HIGHLIGHTER_KEY = Key.create("BREAKPOINT_HIGHLIGHTER_KEY");
  int EXECUTION_LINE_HIGHLIGHTERLAYER = HighlighterLayer.SELECTION - 1;
  TextAttributesKey INLINED_VALUES = TextAttributesKey.createTextAttributesKey("DEBUGGER_INLINED_VALUES");
  TextAttributesKey INLINED_VALUES_MODIFIED = TextAttributesKey.createTextAttributesKey("DEBUGGER_INLINED_VALUES_MODIFIED");
  TextAttributesKey INLINED_VALUES_EXECUTION_LINE = TextAttributesKey.createTextAttributesKey("DEBUGGER_INLINED_VALUES_EXECUTION_LINE");
}
