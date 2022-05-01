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

package consulo.ide.impl.idea.codeInsight.highlighting;

import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionManagerEx;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.TextAttributes;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.document.util.TextRange;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.inject.InjectedEditorManager;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.project.Project;
import consulo.project.ProjectComponent;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.util.ColorValueUtil;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderEx;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@Singleton
public class HighlightManagerImpl extends HighlightManager implements ProjectComponent {
  private final Project myProject;

  @Inject
  public HighlightManagerImpl(Project project) {
    myProject = project;
  }

  @Override
  public void projectOpened() {
    AnActionListener anActionListener = new MyAnActionListener();
    ActionManagerEx.getInstanceEx().addAnActionListener(anActionListener, myProject);

    DocumentListener documentListener = new DocumentAdapter() {
      @Override
      public void documentChanged(DocumentEvent event) {
        Document document = event.getDocument();
        Editor[] editors = EditorFactory.getInstance().getEditors(document);
        for (Editor editor : editors) {
          Map<RangeHighlighter, HighlightInfo> map = getHighlightInfoMap(editor, false);
          if (map == null) return;

          ArrayList<RangeHighlighter> highlightersToRemove = new ArrayList<RangeHighlighter>();
          for (RangeHighlighter highlighter : map.keySet()) {
            HighlightInfo info = map.get(highlighter);
            if (!info.editor.getDocument().equals(document)) continue;
            if ((info.flags & HIDE_BY_TEXT_CHANGE) != 0) {
              highlightersToRemove.add(highlighter);
            }
          }

          for (RangeHighlighter highlighter : highlightersToRemove) {
            removeSegmentHighlighter(editor, highlighter);
          }
        }
      }
    };
    EditorFactory.getInstance().getEventMulticaster().addDocumentListener(documentListener, myProject);
  }

  @Nullable
  public Map<RangeHighlighter, HighlightInfo> getHighlightInfoMap(@Nonnull Editor editor, boolean toCreate) {
    if (editor instanceof EditorWindow) return getHighlightInfoMap(((EditorWindow)editor).getDelegate(), toCreate);
    Map<RangeHighlighter, HighlightInfo> map = editor.getUserData(HIGHLIGHT_INFO_MAP_KEY);
    if (map == null && toCreate) {
      map = ((UserDataHolderEx)editor).putUserDataIfAbsent(HIGHLIGHT_INFO_MAP_KEY, new HashMap<RangeHighlighter, HighlightInfo>());
    }
    return map;
  }

  @Nonnull
  public RangeHighlighter[] getHighlighters(@Nonnull Editor editor) {
    Map<RangeHighlighter, HighlightInfo> highlightersMap = getHighlightInfoMap(editor, false);
    if (highlightersMap == null) return RangeHighlighter.EMPTY_ARRAY;
    Set<RangeHighlighter> set = new HashSet<RangeHighlighter>();
    for (Map.Entry<RangeHighlighter, HighlightInfo> entry : highlightersMap.entrySet()) {
      HighlightInfo info = entry.getValue();
      if (info.editor.equals(editor)) set.add(entry.getKey());
    }
    return set.toArray(new RangeHighlighter[set.size()]);
  }

  private RangeHighlighter addSegmentHighlighter(@Nonnull Editor editor, int startOffset, int endOffset, TextAttributes attributes, @HideFlags int flags) {
    RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(startOffset, endOffset, HighlighterLayer.SELECTION - 1, attributes, HighlighterTargetArea.EXACT_RANGE);
    HighlightInfo info = new HighlightInfo(editor instanceof EditorWindow ? ((EditorWindow)editor).getDelegate() : editor, flags);
    Map<RangeHighlighter, HighlightInfo> map = getHighlightInfoMap(editor, true);
    map.put(highlighter, info);
    return highlighter;
  }

  @Override
  public boolean removeSegmentHighlighter(@Nonnull Editor editor, @Nonnull RangeHighlighter highlighter) {
    Map<RangeHighlighter, HighlightInfo> map = getHighlightInfoMap(editor, false);
    if (map == null) return false;
    HighlightInfo info = map.get(highlighter);
    if (info == null) return false;
    MarkupModel markupModel = info.editor.getMarkupModel();
    if (((MarkupModelEx)markupModel).containsHighlighter(highlighter)) {
      highlighter.dispose();
    }
    map.remove(highlighter);
    return true;
  }

  @Override
  public void addOccurrenceHighlights(@Nonnull Editor editor,
                                      @Nonnull PsiReference[] occurrences,
                                      @Nonnull TextAttributes attributes,
                                      boolean hideByTextChange,
                                      Collection<RangeHighlighter> outHighlighters) {
    if (occurrences.length == 0) return;
    int flags = HIDE_BY_ESCAPE;
    if (hideByTextChange) {
      flags |= HIDE_BY_TEXT_CHANGE;
    }
    ColorValue scrollmarkColor = getScrollMarkColor(attributes);

    int oldOffset = editor.getCaretModel().getOffset();
    int horizontalScrollOffset = editor.getScrollingModel().getHorizontalScrollOffset();
    int verticalScrollOffset = editor.getScrollingModel().getVerticalScrollOffset();
    for (PsiReference occurrence : occurrences) {
      PsiElement element = occurrence.getElement();
      int startOffset = element.getTextRange().getStartOffset();
      int start = startOffset + occurrence.getRangeInElement().getStartOffset();
      int end = startOffset + occurrence.getRangeInElement().getEndOffset();
      PsiFile containingFile = element.getContainingFile();
      Project project = element.getProject();
      // each reference can reside in its own injected editor
      Editor textEditor = InjectedEditorManager.getInstance(project).openEditorFor(containingFile);
      if (textEditor != null) {
        addOccurrenceHighlight(textEditor, start, end, attributes, flags, outHighlighters, scrollmarkColor);
      }
    }
    editor.getCaretModel().moveToOffset(oldOffset);
    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    editor.getScrollingModel().scrollHorizontally(horizontalScrollOffset);
    editor.getScrollingModel().scrollVertically(verticalScrollOffset);
  }

  @Override
  public void addElementsOccurrenceHighlights(@Nonnull Editor editor,
                                              @Nonnull PsiElement[] elements,
                                              @Nonnull TextAttributes attributes,
                                              boolean hideByTextChange,
                                              Collection<RangeHighlighter> outHighlighters) {
    addOccurrenceHighlights(editor, elements, attributes, hideByTextChange, outHighlighters);
  }

  @Override
  public void addOccurrenceHighlight(@Nonnull Editor editor, int start, int end, TextAttributes attributes, int flags, Collection<RangeHighlighter> outHighlighters, ColorValue scrollmarkColor) {
    RangeHighlighter highlighter = addSegmentHighlighter(editor, start, end, attributes, flags);
    if (outHighlighters != null) {
      outHighlighters.add(highlighter);
    }
    if (scrollmarkColor != null) {
      highlighter.setErrorStripeMarkColor(scrollmarkColor);
    }
  }

  @Override
  public void addRangeHighlight(@Nonnull Editor editor,
                                int startOffset,
                                int endOffset,
                                @Nonnull TextAttributes attributes,
                                boolean hideByTextChange,
                                @Nullable Collection<RangeHighlighter> highlighters) {
    addRangeHighlight(editor, startOffset, endOffset, attributes, hideByTextChange, false, highlighters);
  }

  @Override
  public void addRangeHighlight(@Nonnull Editor editor,
                                int startOffset,
                                int endOffset,
                                @Nonnull TextAttributes attributes,
                                boolean hideByTextChange,
                                boolean hideByAnyKey,
                                @Nullable Collection<RangeHighlighter> highlighters) {
    int flags = HIDE_BY_ESCAPE;
    if (hideByTextChange) {
      flags |= HIDE_BY_TEXT_CHANGE;
    }
    if (hideByAnyKey) {
      flags |= HIDE_BY_ANY_KEY;
    }

    ColorValue scrollmarkColor = getScrollMarkColor(attributes);

    addOccurrenceHighlight(editor, startOffset, endOffset, attributes, flags, highlighters, scrollmarkColor);
  }

  @Override
  public void addOccurrenceHighlights(@Nonnull Editor editor,
                                      @Nonnull PsiElement[] elements,
                                      @Nonnull TextAttributes attributes,
                                      boolean hideByTextChange,
                                      Collection<RangeHighlighter> outHighlighters) {
    if (elements.length == 0) return;
    int flags = HIDE_BY_ESCAPE;
    if (hideByTextChange) {
      flags |= HIDE_BY_TEXT_CHANGE;
    }

    ColorValue scrollmarkColor = getScrollMarkColor(attributes);
    if (editor instanceof EditorWindow) {
      editor = ((EditorWindow)editor).getDelegate();
    }

    for (PsiElement element : elements) {
      TextRange range = element.getTextRange();
      range = InjectedLanguageManager.getInstance(myProject).injectedToHost(element, range);
      addOccurrenceHighlight(editor, range.getStartOffset(), range.getEndOffset(), attributes, flags, outHighlighters, scrollmarkColor);
    }
  }

  @Nullable
  private static ColorValue getScrollMarkColor(@Nonnull TextAttributes attributes) {
    if (attributes.getErrorStripeColor() != null) return attributes.getErrorStripeColor();
    if (attributes.getBackgroundColor() != null) return ColorValueUtil.darker(attributes.getBackgroundColor());
    return null;
  }

  public boolean hideHighlights(@Nonnull Editor editor, @HideFlags int mask) {
    Map<RangeHighlighter, HighlightInfo> map = getHighlightInfoMap(editor, false);
    if (map == null) return false;

    boolean done = false;
    ArrayList<RangeHighlighter> highlightersToRemove = new ArrayList<RangeHighlighter>();
    for (RangeHighlighter highlighter : map.keySet()) {
      HighlightInfo info = map.get(highlighter);
      if (!info.editor.equals(editor)) continue;
      if ((info.flags & mask) != 0) {
        highlightersToRemove.add(highlighter);
        done = true;
      }
    }

    for (RangeHighlighter highlighter : highlightersToRemove) {
      removeSegmentHighlighter(editor, highlighter);
    }

    return done;
  }

  private class MyAnActionListener implements AnActionListener {
    @Override
    public void beforeActionPerformed(AnAction action, final DataContext dataContext, AnActionEvent event) {
      requestHideHighlights(dataContext);
    }


    @Override
    public void afterActionPerformed(final AnAction action, final DataContext dataContext, AnActionEvent event) {
    }

    @Override
    public void beforeEditorTyping(char c, DataContext dataContext) {
      requestHideHighlights(dataContext);
    }

    private void requestHideHighlights(final DataContext dataContext) {
      final Editor editor = dataContext.getData(PlatformDataKeys.EDITOR);
      if (editor == null) return;
      hideHighlights(editor, HIDE_BY_ANY_KEY);
    }
  }


  private final Key<Map<RangeHighlighter, HighlightInfo>> HIGHLIGHT_INFO_MAP_KEY = Key.create("HIGHLIGHT_INFO_MAP_KEY");

  static class HighlightInfo {
    final Editor editor;
    @HideFlags
    final int flags;

    public HighlightInfo(Editor editor, @HideFlags int flags) {
      this.editor = editor;
      this.flags = flags;
    }
  }


}
