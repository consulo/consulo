/*
 * Copyright 2013-2023 consulo.io
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
package consulo.language.codeStyle.impl.internal;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.HighlighterIterator;
import consulo.language.ast.IElementType;
import consulo.language.codeStyle.lineIndent.SemanticEditorPosition;
import consulo.language.codeStyle.lineIndent.SemanticEditorPositionFactory;
import jakarta.annotation.Nonnull;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 25/06/2023
 */
public class SemanticEditorPositionFactoryImpl implements SemanticEditorPositionFactory {
  private final Editor myEditor;

  public SemanticEditorPositionFactoryImpl(Editor editor) {
    myEditor = editor;
  }

  @Nonnull
  @Override
  public SemanticEditorPosition create(int offset, Function<IElementType, SemanticEditorPosition.SyntaxElement> mapper) {
    return SemanticEditorPositionImpl.createEditorPosition((EditorEx)myEditor,
                                                           offset,
                                                           this::getIteratorAtPosition,
                                                           mapper::apply);
  }

  @Nonnull
  protected HighlighterIterator getIteratorAtPosition(@Nonnull EditorEx editor, int offset) {
    return editor.getHighlighter().createIterator(offset);
  }
}
