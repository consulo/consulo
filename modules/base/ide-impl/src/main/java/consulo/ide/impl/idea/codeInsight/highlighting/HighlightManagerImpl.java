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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.TextAttributesKey;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.document.util.TextRange;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.inject.InjectedEditorManager;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;

@Singleton
@ServiceImpl
public class HighlightManagerImpl extends HighlightManager {
    private static final Key<Map<RangeHighlighter, HighlightInfoFlags>> HIGHLIGHT_INFO_MAP_KEY = Key.create("HIGHLIGHT_INFO_MAP_KEY");

    static record HighlightInfoFlags(Editor editor, @HideFlags int flags) {
    }

    public static final int OCCURRENCE_LAYER = HighlighterLayer.SELECTION - 1;

    private final Project myProject;

    @Inject
    public HighlightManagerImpl(Project project, EditorFactory editorFactory) {
        myProject = project;

        editorFactory.getEventMulticaster().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(DocumentEvent event) {
                Document document = event.getDocument();
                Editor[] editors = EditorFactory.getInstance().getEditors(document);
                for (Editor editor : editors) {
                    Map<RangeHighlighter, HighlightInfoFlags> map = getHighlightInfoMap(editor, false);
                    if (map == null) {
                        return;
                    }

                    ArrayList<RangeHighlighter> highlightersToRemove = new ArrayList<>();
                    for (RangeHighlighter highlighter : map.keySet()) {
                        HighlightInfoFlags info = map.get(highlighter);
                        if (!info.editor.getDocument().equals(document)) {
                            continue;
                        }
                        if ((info.flags & HighlightManager.HIDE_BY_TEXT_CHANGE) != 0) {
                            highlightersToRemove.add(highlighter);
                        }
                    }

                    for (RangeHighlighter highlighter : highlightersToRemove) {
                        removeSegmentHighlighter(editor, highlighter);
                    }
                }
            }
        }, myProject);

        project.getMessageBus().connect().subscribe(AnActionListener.class, new AnActionListener() {
            @Override
            public void beforeActionPerformed(AnAction action, final DataContext dataContext, AnActionEvent event) {
                requestHideHighlights(dataContext);
            }

            @Override
            public void beforeEditorTyping(char c, DataContext dataContext) {
                requestHideHighlights(dataContext);
            }

            private void requestHideHighlights(final DataContext dataContext) {
                final Editor editor = dataContext.getData(Editor.KEY);
                if (editor == null) {
                    return;
                }

                hideHighlights(editor, HighlightManager.HIDE_BY_ANY_KEY);
            }
        });
    }

    @Nullable
    public Map<RangeHighlighter, HighlightInfoFlags> getHighlightInfoMap(@Nonnull Editor editor, boolean toCreate) {
        if (editor instanceof EditorWindow) {
            return getHighlightInfoMap(((EditorWindow) editor).getDelegate(), toCreate);
        }
        Map<RangeHighlighter, HighlightInfoFlags> map = editor.getUserData(HIGHLIGHT_INFO_MAP_KEY);
        if (map == null && toCreate) {
            map = editor.putUserDataIfAbsent(HIGHLIGHT_INFO_MAP_KEY, new HashMap<>());
        }
        return map;
    }

    @Nonnull
    public RangeHighlighter[] getHighlighters(@Nonnull Editor editor) {
        Map<RangeHighlighter, HighlightInfoFlags> highlightersMap = getHighlightInfoMap(editor, false);
        if (highlightersMap == null) {
            return RangeHighlighter.EMPTY_ARRAY;
        }
        Set<RangeHighlighter> set = new HashSet<>();
        for (Map.Entry<RangeHighlighter, HighlightInfoFlags> entry : highlightersMap.entrySet()) {
            HighlightInfoFlags info = entry.getValue();
            if (info.editor.equals(editor)) {
                set.add(entry.getKey());
            }
        }
        return set.toArray(new RangeHighlighter[set.size()]);
    }

    private RangeHighlighter addSegmentHighlighter(@Nonnull Editor editor,
                                                   int startOffset,
                                                   int endOffset,
                                                   TextAttributesKey attributesKey,
                                                   @HideFlags int flags) {
        RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(attributesKey, startOffset, endOffset, OCCURRENCE_LAYER, HighlighterTargetArea.EXACT_RANGE);
        HighlightInfoFlags info = new HighlightInfoFlags(editor instanceof EditorWindow ? ((EditorWindow) editor).getDelegate() : editor, flags);
        Map<RangeHighlighter, HighlightInfoFlags> map = getHighlightInfoMap(editor, true);
        assert map != null;
        map.put(highlighter, info);
        return highlighter;
    }

    @Override
    public boolean removeSegmentHighlighter(@Nonnull Editor editor, @Nonnull RangeHighlighter highlighter) {
        Map<RangeHighlighter, HighlightInfoFlags> map = getHighlightInfoMap(editor, false);
        if (map == null) {
            return false;
        }
        HighlightInfoFlags info = map.get(highlighter);
        if (info == null) {
            return false;
        }
        MarkupModel markupModel = info.editor.getMarkupModel();
        if (((MarkupModelEx) markupModel).containsHighlighter(highlighter)) {
            highlighter.dispose();
        }
        map.remove(highlighter);
        return true;
    }

    @Override
    @RequiredReadAction
    public void addOccurrenceHighlights(@Nonnull Editor editor,
                                        @Nonnull PsiReference[] occurrences,
                                        @Nonnull TextAttributesKey attributesKey,
                                        boolean hideByTextChange,
                                        @Nullable Collection<RangeHighlighter> outHighlighters) {
        if (occurrences.length == 0) {
            return;
        }
        int flags = HIDE_BY_ESCAPE;
        if (hideByTextChange) {
            flags |= HIDE_BY_TEXT_CHANGE;
        }

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
                addOccurrenceHighlight(textEditor, start, end, attributesKey, flags, outHighlighters);
            }
        }
        editor.getCaretModel().moveToOffset(oldOffset);
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        editor.getScrollingModel().scrollHorizontally(horizontalScrollOffset);
        editor.getScrollingModel().scrollVertically(verticalScrollOffset);
    }

    @RequiredReadAction
    @Override
    public void addOccurrenceHighlights(@Nonnull Editor editor,
                                        @Nonnull PsiElement[] elements,
                                        @Nonnull TextAttributesKey attributesKey,
                                        boolean hideByTextChange,
                                        @Nullable Collection<RangeHighlighter> outHighlighters) {
        if (elements.length == 0) {
            return;
        }
        int flags = HIDE_BY_ESCAPE;
        if (hideByTextChange) {
            flags |= HIDE_BY_TEXT_CHANGE;
        }

        if (editor instanceof EditorWindow) {
            editor = ((EditorWindow) editor).getDelegate();
        }

        for (PsiElement element : elements) {
            TextRange range = element.getTextRange();
            range = InjectedLanguageManager.getInstance(myProject).injectedToHost(element, range);
            addOccurrenceHighlight(editor, range.getStartOffset(), range.getEndOffset(), attributesKey, flags, outHighlighters);
        }
    }

    @Override
    public void addOccurrenceHighlight(@Nonnull Editor editor,
                                       int start,
                                       int end,
                                       TextAttributesKey attributesKey,
                                       @HideFlags int flags,
                                       @Nullable Collection<RangeHighlighter> outHighlighters) {
        RangeHighlighter highlighter = addSegmentHighlighter(editor, start, end, attributesKey, flags);
        if (outHighlighters != null) {
            outHighlighters.add(highlighter);
        }
    }

    @Override
    public void addRangeHighlight(@Nonnull Editor editor,
                                  int startOffset,
                                  int endOffset,
                                  @Nonnull TextAttributesKey attributesKey,
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

        addOccurrenceHighlight(editor, startOffset, endOffset, attributesKey, flags, highlighters);
    }

    public boolean hideHighlights(@Nonnull Editor editor, @HideFlags int mask) {
        Map<RangeHighlighter, HighlightInfoFlags> map = getHighlightInfoMap(editor, false);
        if (map == null) {
            return false;
        }

        boolean done = false;
        ArrayList<RangeHighlighter> highlightersToRemove = new ArrayList<>();
        for (RangeHighlighter highlighter : map.keySet()) {
            HighlightInfoFlags info = map.get(highlighter);
            if (!info.editor.equals(editor)) {
                continue;
            }
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
}
