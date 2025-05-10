// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.function.CommonProcessors;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.RangeMarker;
import consulo.document.util.Segment;
import consulo.ide.impl.idea.codeInsight.intention.impl.CachedIntentions;
import consulo.ide.impl.idea.codeInsight.intention.impl.EditIntentionSettingsAction;
import consulo.ide.impl.idea.codeInsight.intention.impl.EnableDisableIntentionAction;
import consulo.ide.impl.idea.codeInsight.intention.impl.ShowIntentionActionsHandler;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPass;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.impl.internal.template.TemplateManagerImpl;
import consulo.language.editor.impl.internal.template.TemplateStateImpl;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionActionFilter;
import consulo.language.editor.intention.IntentionManager;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.HasFocus;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ShowIntentionsPass extends TextEditorHighlightingPass {
    private final Editor myEditor;

    private final PsiFile myFile;
    private final int myPassIdToShowIntentionsFor;
    private final IntentionsInfo myIntentionsInfo = new IntentionsInfo();
    private final boolean myQueryIntentionActions;
    private volatile CachedIntentions myCachedIntentions;
    private volatile boolean myActionsChanged;

    /**
     * @param queryIntentionActions true if {@link IntentionManager} must be asked for all registered {@link IntentionAction} and {@link IntentionAction#isAvailable(Project, Editor, PsiFile)} must be called on each
     *                              Usually, this expensive process should be executed only once per highlighting session
     */
    @RequiredUIAccess
    ShowIntentionsPass(@Nonnull Project project, @Nonnull Editor editor, boolean queryIntentionActions) {
        super(project, editor.getDocument(), false);
        myQueryIntentionActions = queryIntentionActions;
        myPassIdToShowIntentionsFor = -1;
        UIAccess.assertIsUIThread();

        myEditor = editor;

        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);

        myFile = documentManager.getPsiFile(myEditor.getDocument());
        assert myFile != null : FileDocumentManager.getInstance().getFile(myEditor.getDocument());
    }

    @Nonnull
    public static List<HighlightInfoImpl.IntentionActionDescriptor> getAvailableFixes(
        @Nonnull Editor editor,
        @Nonnull PsiFile file,
        int passId,
        int offset
    ) {
        Project project = file.getProject();

        List<HighlightInfo> infos = new ArrayList<>();
        DaemonCodeAnalyzerImpl.processHighlightsNearOffset(
            editor.getDocument(),
            project,
            HighlightSeverity.INFORMATION,
            offset,
            true,
            new CommonProcessors.CollectProcessor<>(infos)
        );
        List<HighlightInfoImpl.IntentionActionDescriptor> result = new ArrayList<>();
        infos.forEach(info -> addAvailableFixesForGroups(info, editor, file, result, passId, offset));
        return result;
    }

    public static boolean markActionInvoked(@Nonnull Project project, @Nonnull Editor editor, @Nonnull IntentionAction action) {
        int offset = editor.getExpectedCaretOffset();

        List<HighlightInfo> infos = new ArrayList<>();
        DaemonCodeAnalyzerImpl.processHighlightsNearOffset(
            editor.getDocument(),
            project,
            HighlightSeverity.INFORMATION,
            offset,
            true,
            new CommonProcessors.CollectProcessor<>(infos)
        );
        boolean removed = false;
        for (HighlightInfo info : infos) {
            List<Pair<HighlightInfoImpl.IntentionActionDescriptor, RangeMarker>> list = ((HighlightInfoImpl)info).quickFixActionMarkers;
            if (list != null) {
                for (Pair<HighlightInfoImpl.IntentionActionDescriptor, RangeMarker> pair : list) {
                    HighlightInfoImpl.IntentionActionDescriptor actionInGroup = pair.first;
                    if (actionInGroup.getAction() == action) {
                        // no CME because the list is concurrent
                        removed |= list.remove(pair);
                    }
                }
            }
        }
        return removed;
    }

    private static void addAvailableFixesForGroups(
        @Nonnull HighlightInfo i,
        @Nonnull Editor editor,
        @Nonnull PsiFile file,
        @Nonnull List<? super HighlightInfoImpl.IntentionActionDescriptor> outList,
        int group,
        int offset
    ) {
        HighlightInfoImpl info = (HighlightInfoImpl)i;
        if (info.quickFixActionMarkers == null) {
            return;
        }
        if (group != -1 && group != info.getGroup()) {
            return;
        }
        boolean fixRangeIsNotEmpty = !info.getFixTextRange().isEmpty();
        Editor injectedEditor = null;
        PsiFile injectedFile = null;
        for (Pair<HighlightInfoImpl.IntentionActionDescriptor, RangeMarker> pair : info.quickFixActionMarkers) {
            HighlightInfoImpl.IntentionActionDescriptor actionInGroup = pair.first;
            RangeMarker range = pair.second;
            if (!range.isValid() || fixRangeIsNotEmpty && isEmpty(range)) {
                continue;
            }

            if (DumbService.isDumb(file.getProject()) && !DumbService.isDumbAware(actionInGroup.getAction())) {
                continue;
            }

            int start = range.getStartOffset();
            int end = range.getEndOffset();
            Project project = file.getProject();
            if (start > offset || offset > end) {
                continue;
            }
            Editor editorToUse;
            PsiFile fileToUse;
            if (info.isFromInjection()) {
                if (injectedEditor == null) {
                    injectedFile = InjectedLanguageUtil.findInjectedPsiNoCommit(file, offset);
                    injectedEditor = InjectedLanguageUtil.getInjectedEditorForInjectedFile(editor, injectedFile);
                }
                editorToUse = injectedFile == null ? editor : injectedEditor;
                fileToUse = injectedFile == null ? file : injectedFile;
            }
            else {
                editorToUse = editor;
                fileToUse = file;
            }
            if (actionInGroup.getAction().isAvailable(project, editorToUse, fileToUse)) {
                outList.add(actionInGroup);
            }
        }
    }

    private static boolean isEmpty(@Nonnull Segment segment) {
        return segment.getEndOffset() <= segment.getStartOffset();
    }

    public static class IntentionsInfo {
        public final List<HighlightInfoImpl.IntentionActionDescriptor> intentionsToShow = ContainerUtil.createLockFreeCopyOnWriteList();
        public final List<HighlightInfoImpl.IntentionActionDescriptor> errorFixesToShow = ContainerUtil.createLockFreeCopyOnWriteList();
        public final List<HighlightInfoImpl.IntentionActionDescriptor> inspectionFixesToShow =
            ContainerUtil.createLockFreeCopyOnWriteList();
        public final List<HighlightInfoImpl.IntentionActionDescriptor> guttersToShow = ContainerUtil.createLockFreeCopyOnWriteList();
        public final List<HighlightInfoImpl.IntentionActionDescriptor> notificationActionsToShow =
            ContainerUtil.createLockFreeCopyOnWriteList();
        private int myOffset;

        public void filterActions(@Nullable PsiFile psiFile) {
            List<IntentionActionFilter> filters = IntentionActionFilter.EXTENSION_POINT_NAME.getExtensionList();
            filter(intentionsToShow, psiFile, filters);
            filter(errorFixesToShow, psiFile, filters);
            filter(inspectionFixesToShow, psiFile, filters);
            filter(guttersToShow, psiFile, filters);
            filter(notificationActionsToShow, psiFile, filters);
        }

        public void setOffset(int offset) {
            myOffset = offset;
        }

        public int getOffset() {
            return myOffset;
        }

        private static void filter(
            @Nonnull List<HighlightInfoImpl.IntentionActionDescriptor> descriptors,
            @Nullable PsiFile psiFile,
            @Nonnull List<IntentionActionFilter> filters
        ) {
            for (Iterator<HighlightInfoImpl.IntentionActionDescriptor> it = descriptors.iterator(); it.hasNext(); ) {
                HighlightInfoImpl.IntentionActionDescriptor actionDescriptor = it.next();
                for (IntentionActionFilter filter : filters) {
                    if (!filter.accept(actionDescriptor.getAction(), psiFile)) {
                        it.remove();
                        break;
                    }
                }
            }
        }

        public boolean isEmpty() {
            return intentionsToShow.isEmpty() && errorFixesToShow.isEmpty() && inspectionFixesToShow.isEmpty() && guttersToShow.isEmpty() && notificationActionsToShow.isEmpty();
        }

        @Override
        public String toString() {
            return "Errors: " + errorFixesToShow + "; " +
                "Inspection fixes: " + inspectionFixesToShow + "; " +
                "Intentions: " + intentionsToShow + "; " +
                "Gutters: " + guttersToShow + "; " +
                "Notifications: " + notificationActionsToShow;
        }
    }

    @Override
    @RequiredReadAction
    public void doCollectInformation(@Nonnull ProgressIndicator progress) {
        if (!Application.get().isHeadlessEnvironment() && !HasFocus.hasFocus(myEditor.getContentUIComponent())) {
            return;
        }
        TemplateStateImpl state = TemplateManagerImpl.getTemplateStateImpl(myEditor);
        if (state != null && !state.isFinished()) {
            return;
        }
        getActionsToShow(myEditor, myFile, myIntentionsInfo, myPassIdToShowIntentionsFor, myQueryIntentionActions);
        myCachedIntentions = IntentionsUI.getInstance(myProject).getCachedIntentions(myEditor, myFile);
        myActionsChanged = myCachedIntentions.wrapAndUpdateActions(myIntentionsInfo, false);
    }

    @Override
    @RequiredUIAccess
    public void doApplyInformationToEditor() {
        UIAccess.assertIsUIThread();

        CachedIntentions cachedIntentions = myCachedIntentions;
        boolean actionsChanged = myActionsChanged;
        TemplateStateImpl state = TemplateManagerImpl.getTemplateStateImpl(myEditor);
        if ((state == null || state.isFinished()) && cachedIntentions != null) {
            IntentionsInfo syncInfo = new IntentionsInfo();
            getActionsToShowSync(myEditor, myFile, syncInfo, myPassIdToShowIntentionsFor);
            actionsChanged |= cachedIntentions.addActions(syncInfo);

            IntentionsUI.getInstance(myProject).update(cachedIntentions, actionsChanged);
        }
    }


    /**
     * Returns the list of actions to show in the Alt-Enter popup at the caret offset in the given editor.
     *
     * @param includeSyncActions whether EDT-only providers should be queried, if {@code true}, this method should be invoked in EDT
     */
    @Nonnull
    @RequiredUIAccess
    public static IntentionsInfo getActionsToShow(@Nonnull Editor hostEditor, @Nonnull PsiFile hostFile, boolean includeSyncActions) {
        IntentionsInfo result = new IntentionsInfo();
        getActionsToShow(hostEditor, hostFile, result, -1);
        if (includeSyncActions) {
            getActionsToShowSync(hostEditor, hostFile, result, -1);
        }
        return result;
    }

    /**
     * Collects intention actions from providers intended to be invoked in EDT.
     */
    @RequiredUIAccess
    private static void getActionsToShowSync(
        @Nonnull Editor hostEditor,
        @Nonnull PsiFile hostFile,
        @Nonnull IntentionsInfo intentions,
        int passIdToShowIntentionsFor
    ) {
        UIAccess.assertIsUIThread();
        EditorNotificationActions.collectActions(
            hostEditor,
            hostFile,
            intentions,
            passIdToShowIntentionsFor,
            hostEditor.getCaretModel().getOffset()
        );
        intentions.filterActions(hostFile);
    }

    /**
     * Collects intention actions from providers intended to be invoked in a background thread.
     */
    @RequiredUIAccess
    public static void getActionsToShow(
        @Nonnull Editor hostEditor,
        @Nonnull PsiFile hostFile,
        @Nonnull IntentionsInfo intentions,
        int passIdToShowIntentionsFor
    ) {
        getActionsToShow(hostEditor, hostFile, intentions, passIdToShowIntentionsFor, true);
    }

    @RequiredReadAction
    private static void getActionsToShow(
        @Nonnull Editor hostEditor,
        @Nonnull PsiFile hostFile,
        @Nonnull IntentionsInfo intentions,
        int passIdToShowIntentionsFor,
        boolean queryIntentionActions
    ) {
        int offset = hostEditor.getCaretModel().getOffset();
        PsiElement psiElement = hostFile.findElementAt(offset);
        if (psiElement != null) {
            PsiUtilCore.ensureValid(psiElement);
        }

        intentions.setOffset(offset);
        Project project = hostFile.getProject();

        List<HighlightInfoImpl.IntentionActionDescriptor> fixes =
            getAvailableFixes(hostEditor, hostFile, passIdToShowIntentionsFor, offset);
        DaemonCodeAnalyzer codeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
        Document hostDocument = hostEditor.getDocument();
        HighlightInfoImpl infoAtCursor = ((DaemonCodeAnalyzerImpl)codeAnalyzer).findHighlightByOffset(hostDocument, offset, true);
        if (infoAtCursor == null) {
            intentions.errorFixesToShow.addAll(fixes);
        }
        else {
            fillIntentionsInfoForHighlightInfo(infoAtCursor, intentions, fixes);
        }

        if (queryIntentionActions) {
            PsiFile injectedFile = InjectedLanguageUtil.findInjectedPsiNoCommit(hostFile, offset);
            for (IntentionAction action : IntentionManager.getInstance().getAvailableIntentionActions()) {
                Pair<PsiFile, Editor> place =
                    ShowIntentionActionsHandler.chooseBetweenHostAndInjected(
                        hostFile,
                        hostEditor,
                        injectedFile,
                        (psiFile, editor) -> ShowIntentionActionsHandler.availableFor(psiFile, editor, action)
                    );

                if (place != null) {
                    List<IntentionAction> enableDisableIntentionAction = new ArrayList<>();
                    enableDisableIntentionAction.add(new EnableDisableIntentionAction(action));
                    enableDisableIntentionAction.add(new EditIntentionSettingsAction(action));
                    HighlightInfoImpl.IntentionActionDescriptor descriptor =
                        new HighlightInfoImpl.IntentionActionDescriptor(action, enableDisableIntentionAction, null);
                    if (!fixes.contains(descriptor)) {
                        intentions.intentionsToShow.add(descriptor);
                    }
                }
            }

            for (IntentionMenuContributor extension : IntentionMenuContributor.EP_NAME.getExtensionList()) {
                extension.collectActions(hostEditor, hostFile, intentions, passIdToShowIntentionsFor, offset);
            }
        }

        intentions.filterActions(hostFile);
    }

    public static void fillIntentionsInfoForHighlightInfo(
        @Nonnull HighlightInfoImpl infoAtCursor,
        @Nonnull IntentionsInfo intentions,
        @Nonnull List<? extends HighlightInfoImpl.IntentionActionDescriptor> fixes
    ) {
        boolean isError = infoAtCursor.getSeverity() == HighlightSeverity.ERROR;
        for (HighlightInfoImpl.IntentionActionDescriptor fix : fixes) {
            if (fix.isError() && isError) {
                intentions.errorFixesToShow.add(fix);
            }
            else if (fix.isInformation()) {
                intentions.intentionsToShow.add(fix);
            }
            else {
                intentions.inspectionFixesToShow.add(fix);
            }
        }
    }
}

