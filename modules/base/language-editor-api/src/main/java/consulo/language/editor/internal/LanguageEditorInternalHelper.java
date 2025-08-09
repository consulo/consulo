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
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

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
    @Nonnull
    static LanguageEditorInternalHelper getInstance() {
        return Application.get().getInstance(LanguageEditorInternalHelper.class);
    }

    void doWrapLongLinesIfNecessary(@Nonnull Editor editor,
                                    @Nonnull Project project,
                                    @Nonnull Language language,
                                    @Nonnull Document document,
                                    int startOffset,
                                    int endOffset,
                                    List<? extends TextRange> enabledRanges);

    @Contract("null,_,_->null;!null,_,_->!null")
    default Editor getEditorForInjectedLanguageNoCommit(@Nullable Editor editor, @Nullable PsiFile file, int offset) {
        return editor;
    }

    default void appendFragmentsForSpeedSearch(@Nonnull JComponent speedSearchEnabledComponent,
                                               @Nonnull String text,
                                               @Nonnull SimpleTextAttributes attributes,
                                               boolean selected,
                                               @Nonnull ColoredTextContainer simpleColoredComponent) {
        // [VISTALL] hack due we don't have hard dependency to AWT impl
    }

    default ListCellRenderer<LineMarkerInfo> createMergeableLineMarkerRender(Function<LineMarkerInfo, Pair<String, Image>> function) {
        throw new UnsupportedOperationException();
    }

    @RequiredUIAccess
    default void showInspectionsSettings(@Nonnull Project project) {
    }

    @Nonnull
    default List<Annotation> runAnnotator(Annotator annotator, PsiFile file, PsiElement context, boolean batchMode) {
        return List.of();
    }

    default int adjustLineIndentNoCommit(Language language, @Nonnull Document document, @Nonnull Editor editor, int offset) {
        return -1;
    }

    default boolean isInlineRefactoringActive(@Nonnull Editor editor) {
        return false;
    }

    @RequiredUIAccess
    default void setHighlightersToEditor(@Nonnull Project project,
                                         @Nonnull Document document,
                                         int startOffset,
                                         int endOffset,
                                         @Nonnull Collection<HighlightInfo> highlights,
                                         // if null global scheme will be used
                                         @Nullable EditorColorsScheme colorsScheme,
                                         int group) {
    }
}
