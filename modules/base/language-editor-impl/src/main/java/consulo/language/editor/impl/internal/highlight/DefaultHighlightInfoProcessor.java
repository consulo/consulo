// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.impl.internal.highlight;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.colorScheme.EditorColorsScheme;
import consulo.document.Document;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.language.editor.Pass;
import consulo.language.editor.impl.highlight.HighlightInfoProcessor;
import consulo.language.editor.impl.highlight.HighlightingSession;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.language.editor.highlight.TextEditorHighlightingPassFactory;
import consulo.language.editor.internal.DaemonCodeAnalyzerInternal;
import consulo.language.editor.impl.internal.daemon.ShowAutoImportPassFactory;
import consulo.language.editor.impl.internal.markup.EditorMarkupModel;
import consulo.language.editor.impl.internal.markup.ErrorStripeUpdateManager;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.util.Alarm;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DefaultHighlightInfoProcessor extends HighlightInfoProcessor {
    @Override
    public void highlightsInsideVisiblePartAreProduced(
        @Nonnull HighlightingSession session,
        @Nullable Editor editor,
        @Nonnull List<? extends HighlightInfo> infos,
        @Nonnull TextRange priorityRange,
        @Nonnull TextRange restrictRange,
        int groupId
    ) {
        PsiFile psiFile = session.getPsiFile();
        Project project = psiFile.getProject();
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            return;
        }
        long modificationStamp = document.getModificationStamp();
        TextRange priorityIntersection = priorityRange.intersection(restrictRange);
        List<? extends HighlightInfo> infoCopy = new ArrayList<>(infos);
        ((HighlightingSessionImpl)session).applyInEDT(() -> {
            if (modificationStamp != document.getModificationStamp()) {
                return;
            }
            if (priorityIntersection != null) {
                MarkupModel markupModel = DocumentMarkupModel.forDocument(document, project, true);

                EditorColorsScheme scheme = session.getColorsScheme();
                UpdateHighlightersUtilImpl.setHighlightersInRange(
                    project,
                    document,
                    priorityIntersection,
                    scheme,
                    infoCopy,
                    (MarkupModelEx)markupModel,
                    groupId
                );
            }
            if (editor != null && !editor.isDisposed()) {
                // usability: show auto import popup as soon as possible
                if (!DumbService.isDumb(project)) {
                    ShowAutoImportPassFactory siFactory =
                        TextEditorHighlightingPassFactory.EP_NAME.findExtensionOrFail(project, ShowAutoImportPassFactory.class);
                    TextEditorHighlightingPass highlightingPass = siFactory.createHighlightingPass(psiFile, editor);
                    if (highlightingPass != null) {
                        highlightingPass.doApplyInformationToEditor();
                    }
                }

                repaintErrorStripeAndIcon(editor, project);
            }
        });
    }

    @RequiredUIAccess
    public static void repaintErrorStripeAndIcon(@Nonnull Editor editor, @Nonnull Project project) {
        EditorMarkupModel markup = (EditorMarkupModel)editor.getMarkupModel();
        markup.repaintTrafficLightIcon();
        ErrorStripeUpdateManager.getInstance(project).repaintErrorStripePanel(editor);
    }

    @Override
    @RequiredUIAccess
    public void highlightsOutsideVisiblePartAreProduced(
        @Nonnull HighlightingSession session,
        @Nullable Editor editor,
        @Nonnull List<? extends HighlightInfo> infos,
        @Nonnull TextRange priorityRange,
        @Nonnull TextRange restrictedRange,
        int groupId
    ) {
        PsiFile psiFile = session.getPsiFile();
        Project project = psiFile.getProject();
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            return;
        }
        long modificationStamp = document.getModificationStamp();
        ((HighlightingSessionImpl)session).applyInEDT(() -> {
            if (project.isDisposed() || modificationStamp != document.getModificationStamp()) {
                return;
            }

            EditorColorsScheme scheme = session.getColorsScheme();

            UpdateHighlightersUtilImpl.setHighlightersOutsideRange(
                project,
                document,
                psiFile,
                infos,
                scheme,
                restrictedRange.getStartOffset(),
                restrictedRange.getEndOffset(),
                ProperTextRange.create(priorityRange),
                groupId
            );
            if (editor != null) {
                repaintErrorStripeAndIcon(editor, project);
            }
        });
    }

    @Override
    @RequiredReadAction
    public void allHighlightsForRangeAreProduced(
        @Nonnull HighlightingSession session,
        @Nonnull TextRange elementRange,
        @Nullable List<? extends HighlightInfo> infos
    ) {
        PsiFile psiFile = session.getPsiFile();
        killAbandonedHighlightsUnder(psiFile, elementRange, infos, session);
    }

    @RequiredReadAction
    private static void killAbandonedHighlightsUnder(
        @Nonnull PsiFile psiFile,
        @Nonnull TextRange range,
        @Nullable List<? extends HighlightInfo> infos,
        @Nonnull HighlightingSession highlightingSession
    ) {
        Project project = psiFile.getProject();
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            return;
        }
        DaemonCodeAnalyzerInternal.processHighlights(
            document,
            project,
            null,
            range.getStartOffset(),
            range.getEndOffset(),
            e -> {
                HighlightInfoImpl existing = (HighlightInfoImpl)e;
                if (existing.isBijective() && existing.getGroup() == Pass.UPDATE_ALL && range.equalsToRange(
                    existing.getActualStartOffset(),
                    existing.getActualEndOffset()
                )) {
                    if (infos != null) {
                        for (HighlightInfo created : infos) {
                            if (existing.equalsByActualOffset((HighlightInfoImpl)created)) {
                                return true;
                            }
                        }
                    }
                    // seems that highlight info "existing" is going to disappear
                    // remove it earlier
                    ((HighlightingSessionImpl)highlightingSession).queueDisposeHighlighterFor(existing);
                }
                return true;
            }
        );
    }

    @Override
    public void infoIsAvailable(
        @Nonnull HighlightingSession session,
        @Nonnull HighlightInfo info,
        @Nonnull TextRange priorityRange,
        @Nonnull TextRange restrictedRange,
        int groupId
    ) {
        ((HighlightingSessionImpl)session).queueHighlightInfo(info, restrictedRange, groupId);
    }

    @Override
    public void progressIsAdvanced(@Nonnull HighlightingSession highlightingSession, @Nullable Editor editor, double progress) {
        PsiFile file = highlightingSession.getPsiFile();
        repaintTrafficIcon(file, editor, progress);
    }

    private final Alarm repaintIconAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

    private void repaintTrafficIcon(@Nonnull PsiFile file, @Nullable Editor editor, double progress) {
        if (Application.get().isCommandLine()) {
            return;
        }

        if (repaintIconAlarm.isEmpty() || progress >= 1) {
            repaintIconAlarm.addRequest(
                () -> {
                    Project myProject = file.getProject();
                    if (myProject.isDisposed()) {
                        return;
                    }
                    Editor myeditor = editor;
                    if (myeditor == null) {
                        myeditor = PsiUtilBase.findEditor(file);
                    }
                    if (myeditor != null && !myeditor.isDisposed()) {
                        repaintErrorStripeAndIcon(myeditor, myProject);
                    }
                },
                50,
                null
            );
        }
    }
}
