/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 04-Aug-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface LanguageEditorInternalHelper {
    static LanguageEditorInternalHelper getInstance() {
        return Application.get().getInstance(LanguageEditorInternalHelper.class);
    }

    void doWrapLongLinesIfNecessary(Editor editor,
                                    Project project,
                                    Language language,
                                    Document document,
                                    int startOffset,
                                    int endOffset,
                                    List<? extends TextRange> enabledRanges);

    default void appendFragmentsForSpeedSearch(JComponent speedSearchEnabledComponent,
                                               String text,
                                               SimpleTextAttributes attributes,
                                               boolean selected,
                                               ColoredTextContainer simpleColoredComponent) {
        // [VISTALL] hack due we don't have hard dependency to AWT impl
    }

    default ListCellRenderer<LineMarkerInfo> createMergeableLineMarkerRender(Function<LineMarkerInfo, Pair<String, Image>> function) {
        throw new UnsupportedOperationException();
    }

    @RequiredUIAccess
    default void showInspectionsSettings(Project project) {
    }

    default List<Annotation> runAnnotator(Language language,
                                          Annotator annotator,
                                          PsiFile file,
                                          PsiElement context,
                                          boolean batchMode) {
        return List.of();
    }

    default int adjustLineIndentNoCommit(Language language, Document document, Editor editor, int offset) {
        return -1;
    }

    default boolean isInlineRefactoringActive(Editor editor) {
        return false;
    }

    @RequiredUIAccess
    default void setHighlightersToEditor(Project project,
                                         Document document,
                                         PsiFile psiFile,
                                         int startOffset,
                                         int endOffset,
                                         Collection<HighlightInfo> highlights,
                                         // if null global scheme will be used
                                         @Nullable EditorColorsScheme colorsScheme,
                                         int group) {
    }

    @RequiredUIAccess
    default void highlightRanges(Project project,
                                 Editor editor,
                                 TextAttributesKey attributesKey,
                                 boolean clearHighlights,
                                 List<TextRange> textRanges) {

    }
}
