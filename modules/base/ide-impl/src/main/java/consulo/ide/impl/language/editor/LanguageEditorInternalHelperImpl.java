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
package consulo.ide.impl.language.editor;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.editorActions.EnterHandler;
import consulo.ide.impl.idea.codeInsight.highlighting.HighlightUsagesHandler;
import consulo.ide.impl.idea.profile.codeInspection.ui.ErrorsConfigurable;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.codeStyle.impl.internal.CodeFormatterFacade;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.AnnotationSession;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.editor.impl.internal.highlight.AnnotationHolderImpl;
import consulo.language.editor.impl.internal.highlight.UpdateHighlightersUtilImpl;
import consulo.language.editor.internal.DaemonCodeAnalyzerInternal;
import consulo.language.editor.internal.LanguageEditorInternalHelper;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.refactoring.rename.inplace.InplaceRefactoring;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.SelectionAwareListCellRenderer;
import consulo.ui.ex.awt.speedSearch.SpeedSearchUtil;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 04-Aug-22
 */
@Singleton
@ServiceImpl
public class LanguageEditorInternalHelperImpl implements LanguageEditorInternalHelper {
    @Override
    public void doWrapLongLinesIfNecessary(@Nonnull Editor editor,
                                           @Nonnull Project project,
                                           @Nonnull Language language,
                                           @Nonnull Document document,
                                           int startOffset,
                                           int endOffset,
                                           List<? extends TextRange> enabledRanges) {
        CodeFormatterFacade codeFormatter = new CodeFormatterFacade(CodeStyleSettingsManager.getSettings(project), language);

        codeFormatter.doWrapLongLinesIfNecessary(editor, project, document, startOffset, endOffset, enabledRanges);
    }

    @Override
    public Editor getEditorForInjectedLanguageNoCommit(@Nullable Editor editor, @Nullable PsiFile file, int offset) {
        return InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file, offset);
    }

    @Override
    public void appendFragmentsForSpeedSearch(@Nonnull JComponent speedSearchEnabledComponent,
                                              @Nonnull String text,
                                              @Nonnull SimpleTextAttributes attributes,
                                              boolean selected,
                                              @Nonnull ColoredTextContainer simpleColoredComponent) {
        SpeedSearchUtil.appendFragmentsForSpeedSearch(speedSearchEnabledComponent, text, attributes, selected, simpleColoredComponent);
    }

    @Override
    public ListCellRenderer<LineMarkerInfo> createMergeableLineMarkerRender(Function<LineMarkerInfo, Pair<String, Image>> function) {
        return new SelectionAwareListCellRenderer<>(lineMarkerInfo -> {
            Pair<String, Image> info = function.apply(lineMarkerInfo);

            JBLabel label = new JBLabel(info.getFirst(), info.getSecond(), SwingConstants.LEFT);
            label.setBorder(JBUI.Borders.empty(2));
            return label;
        });
    }

    @RequiredUIAccess
    @Override
    public void showInspectionsSettings(@Nonnull Project project) {
        ShowSettingsUtil.getInstance().showAndSelect(project, ErrorsConfigurable.class);
    }

    @Override
    @Nonnull
    public List<Annotation> runAnnotator(Annotator annotator, PsiFile file, PsiElement context, boolean batchMode) {
        AnnotationHolderImpl holder = new AnnotationHolderImpl(new AnnotationSession(file), batchMode);
        holder.runAnnotatorWithContext(context, annotator);
        return holder;
    }

    @Override
    public int adjustLineIndentNoCommit(Language language, @Nonnull Document document, @Nonnull Editor editor, int offset) {
        return EnterHandler.adjustLineIndentNoCommit(language, document, editor, offset);
    }

    @Override
    public boolean isInlineRefactoringActive(@Nonnull Editor editor) {
        return editor.getUserData(InplaceRefactoring.INPLACE_RENAMER) != null;
    }

    @Override
    @RequiredUIAccess
    public void setHighlightersToEditor(@Nonnull Project project,
                                        @Nonnull Document document,
                                        int startOffset,
                                        int endOffset,
                                        @Nonnull Collection<HighlightInfo> highlights,
                                        // if null global scheme will be used
                                        @Nullable EditorColorsScheme colorsScheme,
                                        int group) {
        TextRange range = new TextRange(startOffset, endOffset);
        UIAccess.assertIsUIThread();

        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        DaemonCodeAnalyzerInternal codeAnalyzer = DaemonCodeAnalyzerInternal.getInstanceEx(project);
        codeAnalyzer.cleanFileLevelHighlights(project, group, psiFile);

        MarkupModel markup = DocumentMarkupModel.forDocument(document, project, true);
        UpdateHighlightersUtilImpl.assertMarkupConsistent(markup, project);

        UpdateHighlightersUtilImpl.setHighlightersInRange(project, document, range, colorsScheme, new ArrayList<>(highlights), (MarkupModelEx) markup, group);
    }

    @RequiredUIAccess
    @Override
    public void highlightRanges(Project project, Editor editor, TextAttributesKey attributesKey, boolean clearHighlights, List<TextRange> textRanges) {
        HighlightManager manager = HighlightManager.getInstance(project);
        HighlightUsagesHandler.highlightRanges(manager, editor, EditorColors.SEARCH_RESULT_ATTRIBUTES, false, textRanges);
    }
}
