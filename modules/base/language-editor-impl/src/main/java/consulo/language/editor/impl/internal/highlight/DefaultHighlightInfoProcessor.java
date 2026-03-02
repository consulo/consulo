// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.impl.internal.highlight;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
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
    @RequiredReadAction
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
        if (modificationStamp != document.getModificationStamp()) {
            return;
        }

        TextRange priorityIntersection = priorityRange.intersection(restrictRange);
        List<? extends HighlightInfo> infoCopy = new ArrayList<>(infos);

        // Apply highlights directly from background thread under read lock
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

        // EDT-only: auto-import popup + repaint
        if (editor != null) {
            project.getUIAccess().give(() -> {
                if (editor.isDisposed() || project.isDisposed()) {
                    return;
                }
                // usability: show auto import popup as soon as possible
                if (!DumbService.isDumb(project)) {
                    ShowAutoImportPassFactory siFactory =
                        TextEditorHighlightingPassFactory.EP_NAME.findExtensionOrFail(project, ShowAutoImportPassFactory.class);
                    TextEditorHighlightingPass highlightingPass = siFactory.createHighlightingPass(psiFile, editor);
                    if (highlightingPass != null) {
                        highlightingPass.doApplyInformationToEditor();
                    }
                }

                repaintErrorStripeAndIcon(editor, project, psiFile);
            });
        }
    }

    /**
     * Schedule error stripe and traffic icon repaint.
     * Reads PsiFile on background thread, then dispatches UI update to EDT.
     * Can be called from any thread.
     */
    public static void repaintErrorStripeAndIcon(@Nonnull Editor editor, @Nonnull Project project) {
        ReadAction.nonBlocking(() -> PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()))
            .expireWith(project)
            .expireWhen(() -> editor.isDisposed())
            .finishOnUiThread(Application::getDefaultModalityState, psiFile -> {
                repaintErrorStripeAndIcon(editor, project, psiFile);
            })
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    /**
     * Repaint error stripe and traffic icon with a pre-read PsiFile.
     * Must be called on EDT.
     */
    @RequiredUIAccess
    public static void repaintErrorStripeAndIcon(@Nonnull Editor editor, @Nonnull Project project, @Nullable PsiFile psiFile) {
        if (editor.isDisposed() || project.isDisposed()) return;
        EditorMarkupModel markup = (EditorMarkupModel)editor.getMarkupModel();
        markup.repaintTrafficLightIcon();
        ErrorStripeUpdateManager.getInstance(project).repaintErrorStripePanel(editor, psiFile);
    }

    @Override
    @RequiredReadAction
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
        if (project.isDisposed()) {
            return;
        }

        EditorColorsScheme scheme = session.getColorsScheme();

        // Apply highlights directly from background thread under read lock
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

        // EDT-only: repaint
        if (editor != null) {
            project.getUIAccess().give(() -> {
                if (!editor.isDisposed() && !project.isDisposed()) {
                    repaintErrorStripeAndIcon(editor, project, psiFile);
                }
            });
        }
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
                    ((HighlightingSessionImpl)highlightingSession).disposeHighlighterFor(existing);
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
        ((HighlightingSessionImpl)session).applyHighlightInfo(info, restrictedRange, groupId);
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
                        repaintErrorStripeAndIcon(myeditor, myProject, file);
                    }
                },
                50,
                null
            );
        }
    }
}
