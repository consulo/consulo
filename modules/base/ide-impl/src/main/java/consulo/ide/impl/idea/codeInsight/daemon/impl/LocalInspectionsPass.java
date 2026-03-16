/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.internal.ApplicationEx;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.JobLauncher;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.component.ProcessCanceledException;
import consulo.document.Document;
import consulo.document.DocumentWindow;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInspection.InspectionEngine;
import consulo.ide.impl.idea.codeInspection.ex.GlobalInspectionContextImpl;
import consulo.ide.impl.idea.codeInspection.ex.LocalDescriptorsUtil;
import consulo.ide.impl.idea.codeInspection.ex.ProblemDescriptorImpl;
import consulo.ide.impl.idea.codeInspection.ui.InspectionToolPresentation;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.language.Language;
import consulo.language.editor.Pass;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.highlight.HighlightingLevelManager;
import consulo.language.editor.highlight.UpdateHighlightersUtil;
import consulo.language.editor.impl.highlight.HighlightInfoProcessor;
import consulo.language.editor.impl.internal.highlight.Divider;
import consulo.language.editor.impl.internal.highlight.ProgressableTextEditorHighlightingPass;
import consulo.language.editor.impl.internal.highlight.UpdateHighlightersUtilImpl;
import consulo.language.editor.impl.internal.inspection.InspectionProjectProfileManager;
import consulo.language.editor.impl.internal.inspection.ProblemsHolderImpl;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.scheme.*;
import consulo.language.editor.intention.*;
import consulo.language.editor.localize.DaemonLocalize;
import consulo.language.editor.rawHighlight.*;
import consulo.language.file.FileViewProvider;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.style.StyleManager;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Maps;
import consulo.util.lang.Pair;
import consulo.util.lang.xml.XmlStringUtil;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author max
 */
public class LocalInspectionsPass extends ProgressableTextEditorHighlightingPass {
    private static final Logger LOG = Logger.getInstance(LocalInspectionsPass.class);
    public static final TextRange EMPTY_PRIORITY_RANGE = TextRange.EMPTY_RANGE;
    private static final Predicate<PsiFile> SHOULD_INSPECT_FILTER =
        file -> HighlightingLevelManager.getInstance(file.getProject()).shouldInspect(file);
    private final TextRange myPriorityRange;
    private final boolean myIgnoreSuppressed;
    private final ConcurrentMap<PsiFile, List<InspectionResult>> result = new ConcurrentHashMap<>();
    private static final String PRESENTABLE_NAME = DaemonLocalize.passInspection().get();
    private volatile List<HighlightInfo> myInfos = Collections.emptyList();
    private final String myShortcutText;
    private final SeverityRegistrarImpl mySeverityRegistrar;
    private final InspectionProfileWrapper myProfileWrapper;
    private boolean myFailFastOnAcquireReadAction;

    public LocalInspectionsPass(
        PsiFile file,
        @Nullable Document document,
        int startOffset,
        int endOffset,
        TextRange priorityRange,
        boolean ignoreSuppressed,
        HighlightInfoProcessor highlightInfoProcessor
    ) {
        super(
            file.getProject(),
            document,
            PRESENTABLE_NAME,
            file,
            null,
            new TextRange(startOffset, endOffset),
            true,
            highlightInfoProcessor
        );
        assert file.isPhysical() : "can't inspect non-physical file: " + file + "; " + file.getVirtualFile();
        myPriorityRange = priorityRange;
        myIgnoreSuppressed = ignoreSuppressed;
        setId(Pass.LOCAL_INSPECTIONS);

        KeymapManager keymapManager = KeymapManager.getInstance();

        Keymap keymap = keymapManager.getActiveKeymap();
        myShortcutText =
            keymap == null ? "" : "(" + KeymapUtil.getShortcutsText(keymap.getShortcuts(IdeActions.ACTION_SHOW_ERROR_DESCRIPTION)) + ")";

        InspectionProfile profile =
            consulo.language.editor.inspection.scheme.InspectionProjectProfileManager.getInstance(file.getProject()).getCurrentProfile();

        Function<InspectionProfile, InspectionProfileWrapper> custom = file.getUserData(InspectionProfileWrapper.CUSTOMIZATION_KEY);

        myProfileWrapper = custom == null ? new InspectionProfileWrapper(profile) : custom.apply(profile);
        assert myProfileWrapper != null;
        mySeverityRegistrar =
            (SeverityRegistrarImpl) ((SeverityProvider) myProfileWrapper.getInspectionProfile().getProfileManager()).getSeverityRegistrar();

        // initial guess
        setProgressLimit(300 * 2);
    }

    
    private PsiFile getFile() {
        //noinspection ConstantConditions
        return myFile;
    }

    @Override
    @RequiredReadAction
    protected void collectInformationWithProgress(ProgressIndicator progress) {
        try {
            if (!HighlightingLevelManager.getInstance(myProject).shouldInspect(getFile())) {
                return;
            }
            inspect(getInspectionTools(myProfileWrapper), InspectionManager.getInstance(myProject), true, true, progress);
        }
        finally {
            disposeDescriptors();
        }
    }

    private void disposeDescriptors() {
        result.clear();
    }

    @RequiredReadAction
    public void doInspectInBatch(
        GlobalInspectionContextImpl context,
        InspectionManager iManager,
        List<LocalInspectionToolWrapper> toolWrappers
    ) {
        ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();
        inspect(new ArrayList<>(toolWrappers), iManager, false, false, progress);
        addDescriptorsFromInjectedResults(iManager, context);
        List<InspectionResult> resultList = result.get(getFile());
        if (resultList == null) {
            return;
        }
        for (InspectionResult inspectionResult : resultList) {
            LocalInspectionToolWrapper toolWrapper = inspectionResult.tool;
            for (ProblemDescriptor descriptor : inspectionResult.foundProblems) {
                addDescriptors(toolWrapper, descriptor, context);
            }
        }
    }

    private void addDescriptors(
        LocalInspectionToolWrapper toolWrapper,
        ProblemDescriptor descriptor,
        GlobalInspectionContextImpl context
    ) {
        InspectionToolPresentation toolPresentation = context.getPresentation(toolWrapper);
        LocalDescriptorsUtil.addProblemDescriptors(
            Collections.singletonList(descriptor),
            toolPresentation,
            myIgnoreSuppressed,
            context,
            toolWrapper.getTool()
        );
    }

    @RequiredReadAction
    private void addDescriptorsFromInjectedResults(InspectionManager iManager, GlobalInspectionContextImpl context) {
        InjectedLanguageManager ilManager = InjectedLanguageManager.getInstance(myProject);
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);

        for (Map.Entry<PsiFile, List<InspectionResult>> entry : result.entrySet()) {
            PsiFile file = entry.getKey();
            if (file == getFile()) {
                continue; // not injected
            }
            DocumentWindow documentRange = (DocumentWindow) documentManager.getDocument(file);
            List<InspectionResult> resultList = entry.getValue();
            for (InspectionResult inspectionResult : resultList) {
                LocalInspectionToolWrapper toolWrapper = inspectionResult.tool;
                for (ProblemDescriptor descriptor : inspectionResult.foundProblems) {
                    PsiElement psiElement = descriptor.getPsiElement();
                    if (psiElement == null) {
                        continue;
                    }
                    if (SuppressionUtil.inspectionResultSuppressed(psiElement, toolWrapper.getTool())) {
                        continue;
                    }
                    List<TextRange> editables =
                        ilManager.intersectWithAllEditableFragments(file, ((ProblemDescriptorBase) descriptor).getTextRange());
                    for (TextRange editable : editables) {
                        TextRange hostRange = documentRange.injectedToHost(editable);
                        QuickFix[] fixes = descriptor.getFixes();
                        LocalQuickFix[] localFixes = null;
                        if (fixes != null) {
                            localFixes = new LocalQuickFix[fixes.length];
                            for (int k = 0; k < fixes.length; k++) {
                                QuickFix fix = fixes[k];
                                localFixes[k] = (LocalQuickFix) fix;
                            }
                        }
                        ProblemDescriptor patchedDescriptor = iManager.newProblemDescriptor(descriptor.getDescriptionTemplate())
                            .range(getFile(), hostRange)
                            .highlightType(descriptor.getHighlightType())
                            .onTheFly(true)
                            .withFixes(localFixes)
                            .create();
                        addDescriptors(toolWrapper, patchedDescriptor, context);
                    }
                }
            }
        }
    }

    @RequiredReadAction
    private void inspect(
        List<LocalInspectionToolWrapper> toolWrappers,
        InspectionManager iManager,
        boolean isOnTheFly,
        boolean failFastOnAcquireReadAction,
        ProgressIndicator progress
    ) {
        myFailFastOnAcquireReadAction = failFastOnAcquireReadAction;
        if (toolWrappers.isEmpty()) {
            return;
        }

        List<Divider.DividedElements> allDivided = new ArrayList<>();
        Divider.divideInsideAndOutsideAllRoots(
            myFile,
            myRestrictRange,
            myPriorityRange,
            SHOULD_INSPECT_FILTER,
            allDivided::add
        );
        List<PsiElement> inside = ContainerUtil.concat((List<List<PsiElement>>) ContainerUtil.map(allDivided, d -> d.inside));
        List<PsiElement> outside =
            ContainerUtil.concat((List<List<PsiElement>>) ContainerUtil.map(allDivided, d -> ContainerUtil.concat(d.outside, d.parents)));

        Set<String> elementDialectIds = InspectionEngine.calcElementDialectIds(inside, outside);
        Map<LocalInspectionToolWrapper, Set<String>> toolToSpecifiedLanguageIds =
            InspectionEngine.getToolsToSpecifiedLanguages(toolWrappers);

        setProgressLimit(toolToSpecifiedLanguageIds.size() * 2L);
        LocalInspectionToolSession session =
            new LocalInspectionToolSession(getFile(), myRestrictRange.getStartOffset(), myRestrictRange.getEndOffset());

        List<InspectionContext> init = visitPriorityElementsAndInit(
            toolToSpecifiedLanguageIds,
            iManager,
            isOnTheFly,
            progress,
            inside,
            session,
            toolWrappers,
            elementDialectIds
        );
        inspectInjectedPsi(inside, isOnTheFly, progress, iManager, true, toolWrappers);
        visitRestElementsAndCleanup(progress, outside, session, init, elementDialectIds);
        inspectInjectedPsi(outside, isOnTheFly, progress, iManager, false, toolWrappers);

        progress.checkCanceled();

        myInfos = new ArrayList<>();
        addHighlightsFromResults(myInfos, progress);
    }

    
    private List<InspectionContext> visitPriorityElementsAndInit(
        Map<LocalInspectionToolWrapper, Set<String>> toolToSpecifiedLanguageIds,
        InspectionManager iManager,
        boolean isOnTheFly,
        ProgressIndicator indicator,
        List<PsiElement> elements,
        LocalInspectionToolSession session,
        List<LocalInspectionToolWrapper> wrappers,
        Set<String> elementDialectIds
    ) {
        List<InspectionContext> init = new ArrayList<>();
        List<Map.Entry<LocalInspectionToolWrapper, Set<String>>> entries = new ArrayList<>(toolToSpecifiedLanguageIds.entrySet());

        Predicate<Map.Entry<LocalInspectionToolWrapper, Set<String>>> processor = pair -> {
            LocalInspectionToolWrapper toolWrapper = pair.getKey();
            Set<String> dialectIdsSpecifiedForTool = pair.getValue();
            ((ApplicationEx) Application.get()).executeByImpatientReader(() -> runToolOnElements(
                toolWrapper,
                dialectIdsSpecifiedForTool,
                iManager,
                isOnTheFly,
                indicator,
                elements,
                session,
                init,
                elementDialectIds
            ));
            return true;
        };
        boolean result =
            JobLauncher.getInstance().invokeConcurrentlyUnderProgress(entries, indicator, myFailFastOnAcquireReadAction, processor);
        if (!result) {
            throw new ProcessCanceledException();
        }
        return init;
    }

    @RequiredReadAction
    private void runToolOnElements(
        final LocalInspectionToolWrapper toolWrapper,
        Set<String> dialectIdsSpecifiedForTool,
        final InspectionManager iManager,
        final boolean isOnTheFly,
        final ProgressIndicator indicator,
        List<PsiElement> elements,
        LocalInspectionToolSession session,
        List<InspectionContext> init,
        Set<String> elementDialectIds
    ) {
        indicator.checkCanceled();

        Application.get().assertReadAccessAllowed();
        LocalInspectionTool tool = toolWrapper.getTool();
        final boolean[] applyIncrementally = {isOnTheFly};
        ProblemsHolder holder = new ProblemsHolderImpl(iManager, getFile(), isOnTheFly) {
            @Override
            @RequiredReadAction
            public void registerProblem(ProblemDescriptor descriptor) {
                super.registerProblem(descriptor);
                if (applyIncrementally[0]) {
                    addDescriptorIncrementally(descriptor, toolWrapper, indicator);
                }
            }
        };

        Object state = toolWrapper.getToolState().getState();

        PsiElementVisitor visitor = InspectionEngine.createVisitorAndAcceptElements(
            tool,
            holder,
            isOnTheFly,
            session,
            elements,
            elementDialectIds,
            dialectIdsSpecifiedForTool,
            state
        );

        synchronized (init) {
            init.add(new InspectionContext(toolWrapper, holder, holder.getResultCount(), visitor, dialectIdsSpecifiedForTool));
        }
        advanceProgress(1);

        if (holder.hasResults()) {
            appendDescriptors(getFile(), holder.getResults(), toolWrapper);
        }
        applyIncrementally[0] = false; // do not apply incrementally outside visible range
    }

    private void visitRestElementsAndCleanup(
        ProgressIndicator indicator,
        List<PsiElement> elements,
        LocalInspectionToolSession session,
        List<InspectionContext> init,
        Set<String> elementDialectIds
    ) {
        @RequiredReadAction
        Predicate<InspectionContext> processor = context -> {
            indicator.checkCanceled();
            Application.get().assertReadAccessAllowed();
            InspectionEngine.acceptElements(elements, context.visitor, elementDialectIds, context.dialectIdsSpecifiedForTool);
            advanceProgress(1);

            Object state = context.tool.getToolState().getState();
            context.tool.getTool().inspectionFinished(session, context.holder, state);

            if (context.holder.hasResults()) {
                List<ProblemDescriptor> allProblems = context.holder.getResults();
                List<ProblemDescriptor> restProblems = allProblems.subList(context.problemsSize, allProblems.size());
                appendDescriptors(getFile(), restProblems, context.tool);
            }
            return true;
        };
        boolean result =
            JobLauncher.getInstance().invokeConcurrentlyUnderProgress(init, indicator, myFailFastOnAcquireReadAction, processor);
        if (!result) {
            throw new ProcessCanceledException();
        }
    }

    @RequiredReadAction
    void inspectInjectedPsi(
        List<PsiElement> elements,
        boolean onTheFly,
        ProgressIndicator indicator,
        InspectionManager iManager,
        boolean inVisibleRange,
        List<LocalInspectionToolWrapper> wrappers
    ) {
        Set<PsiFile> injected = new HashSet<>();
        for (PsiElement element : elements) {
            InjectedLanguageManager.getInstance(myProject)
                .enumerateEx(element, getFile(), false, (injectedPsi, places) -> injected.add(injectedPsi));
        }
        if (injected.isEmpty()) {
            return;
        }
        @RequiredReadAction
        Predicate<PsiFile> processor = injectedPsi -> {
            doInspectInjectedPsi(injectedPsi, onTheFly, indicator, iManager, inVisibleRange, wrappers);
            return true;
        };
        if (!JobLauncher.getInstance().invokeConcurrentlyUnderProgress(
            new ArrayList<>(injected),
            indicator,
            myFailFastOnAcquireReadAction,
            processor
        )) {
            throw new ProcessCanceledException();
        }
    }

    @Nullable
    @RequiredReadAction
    private HighlightInfoImpl highlightInfoFromDescriptor(
        ProblemDescriptor problemDescriptor,
        HighlightInfoType highlightInfoType,
        LocalizeValue message,
        LocalizeValue toolTip,
        PsiElement psiElement
    ) {
        TextRange textRange = ((ProblemDescriptorBase) problemDescriptor).getTextRange();
        if (textRange == null || psiElement == null) {
            return null;
        }
        boolean isFileLevel = psiElement instanceof PsiFile && textRange.equals(psiElement.getTextRange());

        HighlightSeverity severity = highlightInfoType.getSeverity(psiElement);
        TextAttributes attributes = mySeverityRegistrar.getTextAttributesBySeverity(severity);
        HighlightInfoImpl.Builder b = HighlightInfoImpl.newHighlightInfo(highlightInfoType)
            .range(psiElement, textRange.getStartOffset(), textRange.getEndOffset())
            .description(message)
            .severity(severity)
            .escapedToolTip(toolTip);
        if (attributes != null) {
            b.textAttributes(attributes);
        }
        if (problemDescriptor.isAfterEndOfLine()) {
            b.endOfLine();
        }
        if (isFileLevel) {
            b.fileLevelAnnotation();
        }
        if (problemDescriptor.getProblemGroup() != null) {
            b.problemGroup(problemDescriptor.getProblemGroup());
        }

        return (HighlightInfoImpl) b.create();
    }

    private final Map<TextRange, RangeMarker> ranges2markersCache = new HashMap<>();

    private final Set<Pair<TextRange, String>> emptyActionRegistered = Collections.synchronizedSet(new HashSet<Pair<TextRange, String>>());

    @RequiredReadAction
    private void addDescriptorIncrementally(
        ProblemDescriptor descriptor,
        LocalInspectionToolWrapper tool,
        ProgressIndicator indicator
    ) {
        if (myIgnoreSuppressed && SuppressionUtil.inspectionResultSuppressed(descriptor.getPsiElement(), tool.getTool())) {
            return;
        }

        PsiElement psiElement = descriptor.getPsiElement();
        if (psiElement == null) return;
        PsiFile file = psiElement.getContainingFile();

        Document thisDocument = PsiDocumentManager.getInstance(myProject).getDocument(file);

        InspectionProfile inspectionProfile =
            InspectionProjectProfileManager.getInstance(myProject).getInspectionProfile();
        HighlightSeverity severity =
            inspectionProfile.getErrorLevel(tool.getHighlightDisplayKey(), file).getSeverity();

        InjectedLanguageManager ilManager = InjectedLanguageManager.getInstance(myProject);

        List<HighlightInfo> infos = new ArrayList<>(2);
        createHighlightsForDescriptor(
            infos, emptyActionRegistered, ilManager, file,
            thisDocument, tool, severity, descriptor, psiElement
        );

        for (HighlightInfo info : infos) {
            EditorColorsScheme colorsScheme = getColorsScheme();
            UpdateHighlightersUtilImpl.addHighlighterToEditorIncrementally(
                myProject, myDocument, getFile(),
                myRestrictRange.getStartOffset(), myRestrictRange.getEndOffset(),
                (HighlightInfoImpl) info, colorsScheme, getId(), ranges2markersCache
            );
        }
    }

    private void appendDescriptors(
        PsiFile file,
        List<ProblemDescriptor> descriptors,
        LocalInspectionToolWrapper tool
    ) {
        for (ProblemDescriptor descriptor : descriptors) {
            if (descriptor == null) {
                LOG.error(
                    "null descriptor. all descriptors(" + descriptors.size() + "): " + descriptors +
                        "; file: " + file + " (" + file.getVirtualFile() + "); tool: " + tool
                );
            }
        }
        InspectionResult result = new InspectionResult(tool, descriptors);
        appendResult(file, result);
    }

    private void appendResult(PsiFile file, InspectionResult result) {
        List<InspectionResult> resultList = this.result.get(file);
        if (resultList == null) {
            resultList = Maps.cacheOrGet(this.result, file, new ArrayList<>());
        }
        synchronized (resultList) {
            resultList.add(result);
        }
    }

    @Override
    @RequiredUIAccess
    protected void applyInformationWithProgress() {
        UpdateHighlightersUtil.setHighlightersToEditor(
            myProject,
            myDocument,
            myRestrictRange.getStartOffset(),
            myRestrictRange.getEndOffset(),
            myInfos,
            getColorsScheme(),
            getId()
        );
    }

    @RequiredReadAction
    private void addHighlightsFromResults(List<HighlightInfo> outInfos, ProgressIndicator indicator) {
        InspectionProfile inspectionProfile = InspectionProjectProfileManager.getInstance(myProject).getInspectionProfile();
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
        InjectedLanguageManager ilManager = InjectedLanguageManager.getInstance(myProject);
        Set<Pair<TextRange, String>> emptyActionRegistered = new HashSet<>();

        for (Map.Entry<PsiFile, List<InspectionResult>> entry : result.entrySet()) {
            indicator.checkCanceled();
            PsiFile file = entry.getKey();
            Document documentRange = documentManager.getDocument(file);
            if (documentRange == null) {
                continue;
            }
            List<InspectionResult> resultList = entry.getValue();
            synchronized (resultList) {
                for (InspectionResult inspectionResult : resultList) {
                    indicator.checkCanceled();
                    LocalInspectionToolWrapper tool = inspectionResult.tool;
                    HighlightSeverity severity =
                        inspectionProfile.getErrorLevel(tool.getHighlightDisplayKey(), file).getSeverity();
                    for (ProblemDescriptor descriptor : inspectionResult.foundProblems) {
                        indicator.checkCanceled();
                        PsiElement element = descriptor.getPsiElement();
                        if (element != null) {
                            createHighlightsForDescriptor(
                                outInfos,
                                emptyActionRegistered,
                                ilManager,
                                file,
                                documentRange,
                                tool,
                                severity,
                                descriptor,
                                element
                            );
                        }
                    }
                }
            }
        }
    }

    @RequiredReadAction
    private void createHighlightsForDescriptor(
        List<HighlightInfo> outInfos,
        Set<Pair<TextRange, String>> emptyActionRegistered,
        InjectedLanguageManager ilManager,
        PsiFile file,
        Document documentRange,
        LocalInspectionToolWrapper toolWrapper,
        HighlightSeverity severity,
        ProblemDescriptor descriptor,
        PsiElement element
    ) {
        LocalInspectionTool tool = toolWrapper.getTool();
        if (myIgnoreSuppressed && SuppressionUtil.inspectionResultSuppressed(element, tool)) {
            return;
        }
        HighlightInfoType level = ProblemDescriptorUtil.highlightTypeFromDescriptor(descriptor, severity, mySeverityRegistrar);
        HighlightInfoImpl info = createHighlightInfo(descriptor, toolWrapper, level, emptyActionRegistered, element);
        if (info == null) {
            return;
        }

        PsiFile context = getTopLevelFileInBaseLanguage(element);
        PsiFile myContext = getTopLevelFileInBaseLanguage(getFile());
        if (context != getFile()) {
            LOG.error(
                "Reported element " + element +
                    " is not from the file '" + file +
                    "' the inspection '" + toolWrapper +
                    "' (" + tool.getClass() + ") " +
                    "was invoked for. ReflectionMessage: '" + descriptor + "'.\n" +
                    "Element' containing file: " + context + "\n" +
                    "Inspection invoked for file: " + myContext + "\n"
            );
        }
        boolean isInjected = file != getFile();
        if (!isInjected) {

            outInfos.add(info);
            return;
        }
        // todo we got to separate our "internal" prefixes/suffixes from user-defined ones
        // todo in the latter case the errors should be highlighted, otherwise not
        List<TextRange> editables =
            ilManager.intersectWithAllEditableFragments(file, new TextRange(info.getStartOffset(), info.getEndOffset()));
        for (TextRange editable : editables) {
            TextRange hostRange = ((DocumentWindow) documentRange).injectedToHost(editable);
            int start = hostRange.getStartOffset();
            int end = hostRange.getEndOffset();
            HighlightInfoImpl.Builder builder = HighlightInfoImpl.newHighlightInfo(info.getType()).range(element, start, end);
            LocalizeValue description = info.getDescription();
            if (description.isNotEmpty()) {
                builder.description(description);
            }
            LocalizeValue tooltip = info.getToolTip();
            if (tooltip.isNotEmpty()) {
                builder.escapedToolTip(tooltip);
            }
            HighlightInfoImpl patched = (HighlightInfoImpl) builder.createUnconditionally();
            if (patched.getStartOffset() != patched.getEndOffset() || info.getStartOffset() == info.getEndOffset()) {
                patched.setFromInjection(true);
                registerQuickFixes(toolWrapper, descriptor, patched, emptyActionRegistered);
                outInfos.add(patched);
            }
        }
    }

    private PsiFile getTopLevelFileInBaseLanguage(PsiElement element) {
        PsiFile file = InjectedLanguageManager.getInstance(myProject).getTopLevelFile(element);
        FileViewProvider viewProvider = file.getViewProvider();
        return viewProvider.getPsi(viewProvider.getBaseLanguage());
    }

    @Nullable
    @RequiredReadAction
    private HighlightInfoImpl createHighlightInfo(
        ProblemDescriptor descriptor,
        LocalInspectionToolWrapper tool,
        HighlightInfoType level,
        Set<Pair<TextRange, String>> emptyActionRegistered,
        PsiElement element
    ) {
        LocalizeValue message = ProblemDescriptorUtil.renderDescriptionMessage(descriptor, element);

        HighlightDisplayKey key = tool.getHighlightDisplayKey();
        InspectionProfile inspectionProfile = myProfileWrapper.getInspectionProfile();
        if (!inspectionProfile.isToolEnabled(key, getFile())) {
            return null;
        }

        HighlightInfoType type = new HighlightInfoTypeImpl(level.getSeverity(element), level.getAttributesKey());
        String link = " <a href=\"#inspection/" + tool.getShortName() + "\"" +
            (StyleManager.get().getCurrentStyle().isDark() ? " color=\"7AB4C9\" " : "") + ">" +
            DaemonLocalize.inspectionExtendedDescription() +
            "</a> " +
            myShortcutText;

        LocalizeValue tooltip = LocalizeValue.empty();
        if (descriptor.showTooltip()) {
            tooltip = message.map(text -> XmlStringUtil.wrapInHtml(
                (text.startsWith("<html>") ? XmlStringUtil.stripHtml(text) : XmlStringUtil.escapeText(text)) + link
            ));
        }
        HighlightInfoImpl highlightInfo = highlightInfoFromDescriptor(descriptor, type, message, tooltip, element);
        if (highlightInfo != null) {
            registerQuickFixes(tool, descriptor, highlightInfo, emptyActionRegistered);
        }
        return highlightInfo;
    }

    private static void registerQuickFixes(
        LocalInspectionToolWrapper tool,
        ProblemDescriptor descriptor,
        HighlightInfoImpl highlightInfo,
        Set<Pair<TextRange, String>> emptyActionRegistered
    ) {
        HighlightDisplayKey key = tool.getHighlightDisplayKey();
        boolean needEmptyAction = true;
        QuickFix[] fixes = descriptor.getFixes();
        if (fixes != null && fixes.length > 0) {
            for (int k = 0; k < fixes.length; k++) {
                if (fixes[k] != null) { // prevent null fixes from var args
                    QuickFixAction.registerQuickFixAction(highlightInfo, QuickFixWrapper.wrap(descriptor, k), key);
                    needEmptyAction = false;
                }
            }
        }
        HintAction hintAction = descriptor instanceof ProblemDescriptorImpl problemDescriptor ? problemDescriptor.getHintAction() : null;
        if (hintAction != null) {
            QuickFixAction.registerQuickFixAction(highlightInfo, hintAction, key);
            needEmptyAction = false;
        }
        if (((ProblemDescriptorBase) descriptor).getEnforcedTextAttributes() != null) {
            needEmptyAction = false;
        }
        if (needEmptyAction && emptyActionRegistered.add(Pair.create(highlightInfo.getFixTextRange(), tool.getShortName()))) {
            IntentionAction emptyIntentionAction = new EmptyIntentionAction(tool.getDisplayName());
            QuickFixAction.registerQuickFixAction(highlightInfo, emptyIntentionAction, key);
        }
    }

    
    private static List<PsiElement> getElementsFrom(PsiFile file) {
        FileViewProvider viewProvider = file.getViewProvider();
        final Set<PsiElement> result = new LinkedHashSet<>();
        PsiElementVisitor visitor = new PsiRecursiveElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitElement(PsiElement element) {
                ProgressManager.checkCanceled();
                PsiElement child = element.getFirstChild();
                if (child == null) {
                    // leaf element
                }
                else {
                    // composite element
                    while (child != null) {
                        child.accept(this);
                        result.add(child);

                        child = child.getNextSibling();
                    }
                }
            }
        };
        for (Language language : viewProvider.getLanguages()) {
            PsiFile psiRoot = viewProvider.getPsi(language);
            if (psiRoot == null || !HighlightingLevelManager.getInstance(file.getProject()).shouldInspect(psiRoot)) {
                continue;
            }
            psiRoot.accept(visitor);
            result.add(psiRoot);
        }
        return new ArrayList<>(result);
    }

    
    List<LocalInspectionToolWrapper> getInspectionTools(InspectionProfileWrapper profile) {
        InspectionToolWrapper[] toolWrappers = profile.getInspectionProfile().getInspectionTools(getFile());
        InspectionProfileWrapper.checkInspectionsDuplicates(toolWrappers);
        List<LocalInspectionToolWrapper> enabled = new ArrayList<>();
        for (InspectionToolWrapper toolWrapper : toolWrappers) {
            ProgressManager.checkCanceled();
            if (!profile.isToolEnabled(toolWrapper.getHighlightDisplayKey(), getFile())) {
                continue;
            }

            LocalInspectionToolWrapper wrapper = toolWrapper instanceof LocalInspectionToolWrapper liWrapper
                ? liWrapper
                : toolWrapper instanceof GlobalInspectionToolWrapper giWrapper
                ? giWrapper.getSharedLocalInspectionToolWrapper()
                : null;

            if (wrapper == null) {
                continue;
            }

            if (myIgnoreSuppressed && SuppressionUtil.inspectionResultSuppressed(getFile(), wrapper.getTool())) {
                continue;
            }
            enabled.add(wrapper);
        }
        return enabled;
    }

    @RequiredReadAction
    private void doInspectInjectedPsi(
        PsiFile injectedPsi,
        final boolean isOnTheFly,
        final ProgressIndicator indicator,
        InspectionManager iManager,
        final boolean inVisibleRange,
        List<LocalInspectionToolWrapper> wrappers
    ) {
        PsiElement host = InjectedLanguageManager.getInstance(injectedPsi.getProject()).getInjectionHost(injectedPsi);

        List<PsiElement> elements = getElementsFrom(injectedPsi);
        if (elements.isEmpty()) {
            return;
        }
        Set<String> elementDialectIds = InspectionEngine.calcElementDialectIds(elements);
        Map<LocalInspectionToolWrapper, Set<String>> toolToSpecifiedLanguageIds = InspectionEngine.getToolsToSpecifiedLanguages(wrappers);
        for (Map.Entry<LocalInspectionToolWrapper, Set<String>> pair : toolToSpecifiedLanguageIds.entrySet()) {
            indicator.checkCanceled();
            final LocalInspectionToolWrapper wrapper = pair.getKey();
            LocalInspectionTool tool = wrapper.getTool();
            Object state = wrapper.getToolState().getState();
            if (host != null && myIgnoreSuppressed && SuppressionUtil.inspectionResultSuppressed(host, tool)) {
                continue;
            }
            ProblemsHolder holder = new ProblemsHolderImpl(iManager, injectedPsi, isOnTheFly) {
                @Override
                @RequiredReadAction
                public void registerProblem(ProblemDescriptor descriptor) {
                    super.registerProblem(descriptor);
                    if (isOnTheFly && inVisibleRange) {
                        addDescriptorIncrementally(descriptor, wrapper, indicator);
                    }
                }
            };

            LocalInspectionToolSession injSession = new LocalInspectionToolSession(injectedPsi, 0, injectedPsi.getTextLength());
            Set<String> dialectIdsSpecifiedForTool = pair.getValue();
            InspectionEngine.createVisitorAndAcceptElements(
                tool,
                holder,
                isOnTheFly,
                injSession,
                elements,
                elementDialectIds,
                dialectIdsSpecifiedForTool,
                state
            );
            tool.inspectionFinished(injSession, holder, state);
            List<ProblemDescriptor> problems = holder.getResults();
            if (!problems.isEmpty()) {
                appendDescriptors(injectedPsi, problems, wrapper);
            }
        }
    }

    @Override
    
    public List<HighlightInfo> getInfos() {
        return myInfos;
    }

    private static class InspectionResult {
        
        private final LocalInspectionToolWrapper tool;
        
        private final List<ProblemDescriptor> foundProblems;

        private InspectionResult(LocalInspectionToolWrapper tool, List<ProblemDescriptor> foundProblems) {
            this.tool = tool;
            this.foundProblems = new ArrayList<>(foundProblems);
        }
    }

    private static class InspectionContext {
        private InspectionContext(
            LocalInspectionToolWrapper tool,
            ProblemsHolder holder,
            int problemsSize,
            // need this to diff between found problems in visible part and the rest
            PsiElementVisitor visitor,
            @Nullable Set<String> dialectIdsSpecifiedForTool
        ) {
            this.tool = tool;
            this.holder = holder;
            this.problemsSize = problemsSize;
            this.visitor = visitor;
            this.dialectIdsSpecifiedForTool = dialectIdsSpecifiedForTool;
        }

        
        private final LocalInspectionToolWrapper tool;
        
        private final ProblemsHolder holder;
        private final int problemsSize;
        
        private final PsiElementVisitor visitor;
        @Nullable
        private final Set<String> dialectIdsSpecifiedForTool;
    }
}
