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

package consulo.language.editor.impl.internal.template;

import consulo.codeEditor.*;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributesKey;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.editor.highlight.*;
import consulo.language.editor.template.TemplateColors;
import consulo.language.editor.template.context.TemplateContext;
import consulo.language.editor.template.context.TemplateContextType;
import consulo.language.lexer.CompositeLexer;
import consulo.language.lexer.Lexer;
import consulo.language.lexer.MergingLexerAdapter;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

public class TemplateEditorUtil {
  private TemplateEditorUtil() {}

  public static Editor createEditor(boolean isReadOnly, CharSequence text) {
    return createEditor(isReadOnly, text, null);
  }

  public static Editor createEditor(boolean isReadOnly, CharSequence text, @Nullable Map<TemplateContextType, Boolean> context) {
    final Project project = DataManager.getInstance().getDataContext().getData(Project.KEY);
    return createEditor(isReadOnly, createDocument(text, context, project), project);
  }

  private static Document createDocument(CharSequence text, @Nullable Map<TemplateContextType, Boolean> context, Project project) {
    if (context != null) {
      for (Map.Entry<TemplateContextType, Boolean> entry : context.entrySet()) {
        if (entry.getValue()) {
          return entry.getKey().createDocument(text, project);
        }
      }
    }

    return EditorFactory.getInstance().createDocument(text);
  }

  private static Editor createEditor(boolean isReadOnly, final Document document, final Project project) {
    EditorFactory editorFactory = EditorFactory.getInstance();
    Editor editor = (isReadOnly ? editorFactory.createViewer(document, project) : editorFactory.createEditor(document, project));

    EditorSettings editorSettings = editor.getSettings();
    editorSettings.setVirtualSpace(false);
    editorSettings.setLineMarkerAreaShown(false);
    editorSettings.setIndentGuidesShown(false);
    editorSettings.setLineNumbersShown(false);
    editorSettings.setFoldingOutlineShown(false);

    EditorColorsScheme scheme = editor.getColorsScheme();
    scheme.setColor(EditorColors.CARET_ROW_COLOR, null);

    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file != null) {
      EditorHighlighter highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(file, scheme, project);
      ((EditorEx) editor).setHighlighter(highlighter);
    }

    return editor;
  }

  public static void setHighlighter(Editor editor, TemplateContext templateContext) {
    SyntaxHighlighter baseHighlighter = null;
    for(TemplateContextType type: TemplateContextType.EP_NAME.getExtensionList()) {
      if (templateContext.isEnabled(type)) {
        baseHighlighter = type.createHighlighter();
        if (baseHighlighter != null) break;
      }
    }
    if (baseHighlighter == null) {
      baseHighlighter = new DefaultSyntaxHighlighter();
    }

    SyntaxHighlighter highlighter = createTemplateTextHighlighter(baseHighlighter);
    ((EditorEx)editor).setHighlighter(new LexerEditorHighlighter(highlighter, EditorColorsManager.getInstance().getGlobalScheme()));
  }

  private final static TokenSet TOKENS_TO_MERGE = TokenSet.create(TemplateTokenType.TEXT);

  private static SyntaxHighlighter createTemplateTextHighlighter(final SyntaxHighlighter original) {
    return new TemplateHighlighter(original);
  }

  private static class TemplateHighlighter extends SyntaxHighlighterBase {
    private final Lexer myLexer;
    private final SyntaxHighlighter myOriginalHighlighter;

    public TemplateHighlighter(SyntaxHighlighter original) {
      myOriginalHighlighter = original;
      Lexer originalLexer = original.getHighlightingLexer();
      Lexer templateLexer = new TemplateTextLexer();
      templateLexer = new MergingLexerAdapter(templateLexer, TOKENS_TO_MERGE);

      myLexer = new CompositeLexer(originalLexer, templateLexer) {
        @Override
        protected IElementType getCompositeTokenType(IElementType type1, IElementType type2) {
          if (type2 == TemplateTokenType.VARIABLE) {
            return type2;
          }
          else {
            return type1;
          }
        }
      };
    }

    @Override
    @Nonnull
    public Lexer getHighlightingLexer() {
      return myLexer;
    }

    @Override
    @Nonnull
    public TextAttributesKey[] getTokenHighlights(@Nonnull IElementType tokenType) {
      if (tokenType == TemplateTokenType.VARIABLE) {
        return SyntaxHighlighterBase.pack(myOriginalHighlighter.getTokenHighlights(tokenType), TemplateColors.TEMPLATE_VARIABLE_ATTRIBUTES);
      }

      return myOriginalHighlighter.getTokenHighlights(tokenType);
    }
  }
}
