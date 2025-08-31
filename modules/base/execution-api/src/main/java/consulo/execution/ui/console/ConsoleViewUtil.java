// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.ui.console;

import consulo.application.ApplicationManager;
import consulo.application.ui.UISettings;
import consulo.application.util.ConcurrentFactoryMap;
import consulo.codeEditor.*;
import consulo.codeEditor.internal.InternalEditorFactory;
import consulo.colorScheme.*;
import consulo.content.scope.SearchScope;
import consulo.document.Document;
import consulo.language.ast.IElementType;
import consulo.language.editor.highlight.EmptyEditorHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.lexer.Lexer;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.style.StyleManager;
import consulo.undoRedo.util.UndoUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.text.StringTokenizer;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author peter
 */
public class ConsoleViewUtil {

  public static final Key<Boolean> EDITOR_IS_CONSOLE_HISTORY_VIEW = Key.create("EDITOR_IS_CONSOLE_HISTORY_VIEW");

  private static final Key<Boolean> REPLACE_ACTION_ENABLED = Key.create("REPLACE_ACTION_ENABLED");

  @Nonnull
  public static EditorEx setupConsoleEditor(Project project, boolean foldingOutlineShown, boolean lineMarkerAreaShown) {
    EditorFactory editorFactory = EditorFactory.getInstance();
    Document document = ((InternalEditorFactory)editorFactory).createDocument(true);
    UndoUtil.disableUndoFor(document);
    EditorEx editor = (EditorEx)editorFactory.createViewer(document, project, EditorKind.CONSOLE);
    setupConsoleEditor(editor, foldingOutlineShown, lineMarkerAreaShown);
    return editor;
  }

  public static void setupConsoleEditor(@Nonnull EditorEx editor, boolean foldingOutlineShown, boolean lineMarkerAreaShown) {
    ApplicationManager.getApplication().runReadAction(() -> {

      EditorSettings editorSettings = editor.getSettings();
      editorSettings.setLineMarkerAreaShown(lineMarkerAreaShown);
      editorSettings.setIndentGuidesShown(false);
      editorSettings.setLineNumbersShown(false);
      editorSettings.setFoldingOutlineShown(foldingOutlineShown);
      editorSettings.setAdditionalPageAtBottom(false);
      editorSettings.setAdditionalColumnsCount(0);
      editorSettings.setAdditionalLinesCount(0);
      editorSettings.setRightMarginShown(false);
      editorSettings.setCaretRowShown(false);
      editor.getGutterComponentEx().setPaintBackground(false);

      DelegateColorScheme scheme = updateConsoleColorScheme(editor.getColorsScheme());
      if (UISettings.getInstance().getPresentationMode()) {
        scheme.setEditorFontSize(UISettings.getInstance().getPresentationModeFontSize());
      }
      editor.setColorsScheme(scheme);
      editor.setHighlighter(new NullEditorHighlighter());
    });
  }

  private static class NullEditorHighlighter extends EmptyEditorHighlighter {
    private static final TextAttributes NULL_ATTRIBUTES = new TextAttributes();

    NullEditorHighlighter() {
      super(NULL_ATTRIBUTES);
    }

    @Override
    public void setColorScheme(@Nonnull EditorColorsScheme scheme) {
    }
  }

  @Nonnull
  public static DelegateColorScheme updateConsoleColorScheme(@Nonnull EditorColorsScheme scheme) {
    return new DelegateColorScheme(scheme) {
      @Nonnull
      @Override
      public ColorValue getDefaultBackground() {
        ColorValue color = getColor(ConsoleViewContentType.CONSOLE_BACKGROUND_KEY);
        return color == null ? super.getDefaultBackground() : color;
      }

      @Nonnull
      @Override
      public FontPreferences getFontPreferences() {
        return getConsoleFontPreferences();
      }

      @Override
      public int getEditorFontSize() {
        return getConsoleFontSize();
      }

      @Override
      public String getEditorFontName() {
        return getConsoleFontName();
      }

      @Override
      public float getLineSpacing() {
        return getConsoleLineSpacing();
      }

      @Nonnull
      @Override
      public Font getFont(EditorFontType key) {
        return super.getFont(EditorFontType.getConsoleType(key));
      }

      @Override
      public void setEditorFontSize(int fontSize) {
        setConsoleFontSize(fontSize);
      }
    };
  }

  public static boolean isConsoleViewEditor(@Nonnull Editor editor) {
    return editor.getEditorKind() == (EditorKind.CONSOLE);
  }

  public static boolean isReplaceActionEnabledForConsoleViewEditor(@Nonnull Editor editor) {
    return Objects.equals(editor.getUserData(REPLACE_ACTION_ENABLED), Boolean.TRUE);
  }

  public static void enableReplaceActionForConsoleViewEditor(@Nonnull Editor editor) {
    editor.putUserData(REPLACE_ACTION_ENABLED, true);
  }

  private static class ColorCache {
    static final Map<Key, List<TextAttributesKey>> textAttributeKeys = new ConcurrentHashMap<>();
    static final Map<Key, TextAttributes> mergedTextAttributes = ConcurrentFactoryMap.createMap(contentKey -> {
      EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
      TextAttributes result = scheme.getAttributes(HighlighterColors.TEXT);
      for (TextAttributesKey key : textAttributeKeys.get(contentKey)) {
        TextAttributes attributes = scheme.getAttributes(key);
        if (attributes != null) {
          result = TextAttributes.merge(result, attributes);
        }
      }
      return result;
    });

    static final Map<List<TextAttributesKey>, Key> keys = ConcurrentFactoryMap.createMap(keys -> {
      StringBuilder keyName = new StringBuilder("ConsoleViewUtil_");
      for (TextAttributesKey key : keys) {
        keyName.append("_").append(key.getExternalName());
      }
      final Key newKey = new Key(keyName.toString());
      textAttributeKeys.put(newKey, keys);
      ConsoleViewContentType contentType = new ConsoleViewContentType(keyName.toString(), HighlighterColors.TEXT) {
        @Override
        public TextAttributes getAttributes() {
          return mergedTextAttributes.get(newKey);
        }
      };

      ConsoleViewContentType.registerNewConsoleViewType(newKey, contentType);
      return newKey;
    });

    static {
      StyleManager.get().addChangeListener((oldStyle, newStyle) -> {
        mergedTextAttributes.clear();
      });
    }
  }

  public static void printWithHighlighting(@Nonnull ConsoleView console, @Nonnull String text, @Nonnull SyntaxHighlighter highlighter) {
    printWithHighlighting(console, text, highlighter, null);
  }

  public static void printWithHighlighting(@Nonnull ConsoleView console, @Nonnull String text, @Nonnull SyntaxHighlighter highlighter, Runnable doOnNewLine) {
    Lexer lexer = highlighter.getHighlightingLexer();
    lexer.start(text, 0, text.length(), 0);

    IElementType tokenType;
    while ((tokenType = lexer.getTokenType()) != null) {
      ConsoleViewContentType contentType = getContentTypeForToken(tokenType, highlighter);
      StringTokenizer eolTokenizer = new StringTokenizer(lexer.getTokenText(), "\n", true);
      while (eolTokenizer.hasMoreTokens()) {
        String tok = eolTokenizer.nextToken();
        console.print(tok, contentType);
        if (doOnNewLine != null && "\n".equals(tok)) {
          doOnNewLine.run();
        }
      }

      lexer.advance();
    }
  }

  @Nonnull
  public static ConsoleViewContentType getContentTypeForToken(@Nonnull IElementType tokenType, @Nonnull SyntaxHighlighter highlighter) {
    TextAttributesKey[] keys = highlighter.getTokenHighlights(tokenType);
    if (keys.length == 0) {
      return ConsoleViewContentType.NORMAL_OUTPUT;
    }
    return ConsoleViewContentType.getConsoleViewType(ColorCache.keys.get(Arrays.asList(keys)));
  }

  public static void printAsFileType(@Nonnull ConsoleView console, @Nonnull String text, @Nonnull FileType fileType) {
    SyntaxHighlighter highlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(fileType, null, null);
    if (highlighter != null) {
      printWithHighlighting(console, text, highlighter);
    }
    else {
      console.print(text, ConsoleViewContentType.NORMAL_OUTPUT);
    }
  }

  @Nonnull
  public static List<Filter> computeConsoleFilters(@Nonnull Project project, @Nullable ConsoleView consoleView, @Nonnull SearchScope searchScope) {
    List<Filter> result = new ArrayList<>();
    ConsoleFilterProvider.FILTER_PROVIDERS.forEachExtensionSafe(eachProvider -> {
      Filter[] filters;
      if (consoleView != null && eachProvider instanceof ConsoleDependentFilterProvider) {
        filters = ((ConsoleDependentFilterProvider)eachProvider).getDefaultFilters(consoleView, project, searchScope);
      }
      else if (eachProvider instanceof ConsoleFilterProviderEx) {
        filters = ((ConsoleFilterProviderEx)eachProvider).getDefaultFilters(project, searchScope);
      }
      else {
        filters = eachProvider.getDefaultFilters(project);
      }
      ContainerUtil.addAll(result, filters);
    });
    return result;
  }
}
