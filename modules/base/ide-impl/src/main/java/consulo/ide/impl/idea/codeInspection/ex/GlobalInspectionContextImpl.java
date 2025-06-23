/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInspection.ex;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.internal.SensitiveProgressWrapper;
import consulo.application.progress.*;
import consulo.application.util.NotNullLazyValue;
import consulo.application.util.concurrent.JobLauncher;
import consulo.application.util.function.Computable;
import consulo.component.ProcessCanceledException;
import consulo.content.FileIndex;
import consulo.content.scope.SearchScope;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.analysis.PerformAnalysisInBackgroundOption;
import consulo.ide.impl.idea.codeInsight.daemon.impl.LocalInspectionsPass;
import consulo.ide.impl.idea.codeInspection.ui.DefaultInspectionToolPresentation;
import consulo.ide.impl.idea.codeInspection.ui.InspectionResultsView;
import consulo.ide.impl.idea.codeInspection.ui.InspectionToolPresentation;
import consulo.ide.impl.idea.concurrency.JobLauncherImpl;
import consulo.ide.impl.idea.openapi.util.JDOMUtil;
import consulo.ide.impl.idea.util.ConcurrencyUtil;
import consulo.application.progress.SequentialModalProgressTask;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.annotation.ProblemGroup;
import consulo.language.editor.impl.highlight.HighlightInfoProcessor;
import consulo.language.editor.impl.inspection.GlobalInspectionContextBase;
import consulo.language.editor.impl.inspection.reference.RefManagerImpl;
import consulo.language.editor.impl.inspection.scheme.GlobalInspectionToolWrapper;
import consulo.language.editor.impl.inspection.scheme.LocalInspectionToolWrapper;
import consulo.language.editor.impl.internal.inspection.ProblemsHolderImpl;
import consulo.language.editor.impl.internal.inspection.scheme.ToolsImpl;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.reference.RefVisitor;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.inspection.scheme.Tools;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.ui.scope.AnalysisUIOptions;
import consulo.language.impl.file.SingleRootFileViewProvider;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.util.ProjectUtilCore;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.ProjectCoreUtil;
import consulo.project.macro.ProjectPathMacroManager;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerAdapter;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.EmptyRunnable;
import consulo.util.lang.function.TripleFunction;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;

public class GlobalInspectionContextImpl extends GlobalInspectionContextBase implements GlobalInspectionContext {
    private static final Logger LOG = Logger.getInstance(GlobalInspectionContextImpl.class);
    static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.toolWindowGroup("Inspection Results", ToolWindowId.INSPECTION);
    private final NotNullLazyValue<ContentManager> myContentManager;
    private InspectionResultsView myView;
    private Content myContent;

    @Nonnull
    private AnalysisUIOptions myUIOptions;

    public GlobalInspectionContextImpl(@Nonnull Project project, @Nonnull NotNullLazyValue<ContentManager> contentManager) {
        super(project);

        myUIOptions = AnalysisUIOptions.getInstance(project).copy();
        myContentManager = contentManager;
    }

    @Nonnull
    private ContentManager getContentManager() {
        return myContentManager.getValue();
    }

    @RequiredUIAccess
    public synchronized void addView(@Nonnull InspectionResultsView view, @Nonnull String title) {
        if (myContent != null) {
            return;
        }
        myContentManager.getValue().addContentManagerListener(new ContentManagerAdapter() {
            @Override
            public void contentRemoved(ContentManagerEvent event) {
                if (event.getContent() == myContent) {
                    if (myView != null) {
                        close(false);
                    }
                    myContent = null;
                }
            }
        });

        myView = view;
        myContent = ContentFactory.getInstance().createContent(view, title, false);

        myContent.setDisposer(myView);

        ContentManager contentManager = getContentManager();
        contentManager.addContent(myContent);
        contentManager.setSelectedContent(myContent);

        ToolWindowManager.getInstance(getProject()).getToolWindow(ToolWindowId.INSPECTION).activate(null);
    }

    @RequiredUIAccess
    public void addView(@Nonnull InspectionResultsView view) {
        addView(
            view,
            view.getCurrentProfileName() == null
                ? InspectionLocalize.inspectionResultsTitle().get()
                : InspectionLocalize.inspectionResultsForProfileToolwindowTitle(view.getCurrentProfileName()).get()
        );
    }

    @Override
    public void doInspections(@Nonnull final AnalysisScope scope) {
        if (myContent != null) {
            getContentManager().removeContent(myContent, true);
        }
        super.doInspections(scope);
    }

    public void launchInspectionsOffline(
        @Nonnull final AnalysisScope scope,
        @Nullable final String outputPath,
        final boolean runGlobalToolsOnly,
        @Nonnull final List<File> inspectionsResults
    ) {
        performInspectionsWithProgressAndExportResults(scope, runGlobalToolsOnly, true, outputPath, inspectionsResults);
    }

    public void performInspectionsWithProgressAndExportResults(
        @Nonnull final AnalysisScope scope,
        final boolean runGlobalToolsOnly,
        final boolean isOfflineInspections,
        @Nullable final String outputPath,
        @Nonnull final List<File> inspectionsResults
    ) {
        cleanupTools();
        setCurrentScope(scope);

        final Runnable action = () -> {
            DefaultInspectionToolPresentation.setOutputPath(outputPath);
            try {
                performInspectionsWithProgress(scope, runGlobalToolsOnly, isOfflineInspections);
                exportResults(inspectionsResults, outputPath);
            }
            finally {
                DefaultInspectionToolPresentation.setOutputPath(null);
            }
        };
        if (isOfflineInspections) {
            Application.get().runReadAction(action);
        }
        else {
            action.run();
        }
    }

    private void exportResults(@Nonnull List<File> inspectionsResults, @Nullable String outputPath) {
        final String ext = ".xml";
        final Map<Element, Tools> globalTools = new HashMap<>();
        for (Map.Entry<String, Tools> entry : myTools.entrySet()) {
            final Tools sameTools = entry.getValue();
            boolean hasProblems = false;
            String toolName = entry.getKey();
            if (sameTools != null) {
                for (ScopeToolState toolDescr : sameTools.getTools()) {
                    InspectionToolWrapper toolWrapper = toolDescr.getTool();
                    if (toolWrapper instanceof LocalInspectionToolWrapper) {
                        hasProblems = new File(outputPath, toolName + ext).exists();
                    }
                    else {
                        InspectionToolPresentation presentation = getPresentation(toolWrapper);
                        presentation.updateContent();
                        if (presentation.hasReportedProblems()) {
                            final Element root = new Element(InspectionLocalize.inspectionProblems().get());
                            globalTools.put(root, sameTools);
                            LOG.assertTrue(!hasProblems, toolName);
                            break;
                        }
                    }
                }
            }
            if (hasProblems) {
                try {
                    new File(outputPath).mkdirs();
                    final File file = new File(outputPath, toolName + ext);
                    inspectionsResults.add(file);
                    FileUtil.writeToFile(
                        file,
                        ("</" + InspectionLocalize.inspectionProblems() + ">").getBytes(StandardCharsets.UTF_8),
                        true
                    );
                }
                catch (IOException e) {
                    LOG.error(e);
                }
            }
        }

        getRefManager().iterate(new RefVisitor() {
            @Override
            public void visitElement(@Nonnull final RefEntity refEntity) {
                for (Map.Entry<Element, Tools> entry : globalTools.entrySet()) {
                    Tools tools = entry.getValue();
                    Element element = entry.getKey();
                    for (ScopeToolState state : tools.getTools()) {
                        try {
                            InspectionToolWrapper toolWrapper = state.getTool();
                            InspectionToolPresentation presentation = getPresentation(toolWrapper);
                            presentation.exportResults(element, refEntity);
                        }
                        catch (Throwable e) {
                            LOG.error("Problem when exporting: " + refEntity.getExternalName(), e);
                        }
                    }
                }
            }
        });

        for (Map.Entry<Element, Tools> entry : globalTools.entrySet()) {
            final String toolName = entry.getValue().getShortName();
            Element element = entry.getKey();
            element.setAttribute(LOCAL_TOOL_ATTRIBUTE, Boolean.toString(false));
            final org.jdom.Document doc = new org.jdom.Document(element);
            ProjectPathMacroManager.getInstance(getProject()).collapsePaths(doc.getRootElement());
            try {
                new File(outputPath).mkdirs();
                final File file = new File(outputPath, toolName + ext);
                inspectionsResults.add(file);

                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                    JDOMUtil.writeDocument(doc, writer, "\n");
                }
            }
            catch (IOException e) {
                LOG.error(e);
            }
        }
    }

    public void ignoreElement(@Nonnull InspectionTool tool, @Nonnull PsiElement element) {
        final RefElement refElement = getRefManager().getReference(element);
        final Tools tools = myTools.get(tool.getShortName());
        if (tools != null) {
            for (ScopeToolState state : tools.getTools()) {
                InspectionToolWrapper toolWrapper = state.getTool();
                ignoreElementRecursively(toolWrapper, refElement);
            }
        }
    }

    public InspectionResultsView getView() {
        return myView;
    }

    private void ignoreElementRecursively(@Nonnull InspectionToolWrapper toolWrapper, final RefEntity refElement) {
        if (refElement != null) {
            InspectionToolPresentation presentation = getPresentation(toolWrapper);
            presentation.ignoreCurrentElement(refElement);
            final List<RefEntity> children = refElement.getChildren();
            if (children != null) {
                for (RefEntity child : children) {
                    ignoreElementRecursively(toolWrapper, child);
                }
            }
        }
    }

    @Nonnull
    public AnalysisUIOptions getUIOptions() {
        return myUIOptions;
    }

    public void setSplitterProportion(final float proportion) {
        myUIOptions.SPLITTER_PROPORTION = proportion;
    }

    @Nonnull
    public ToggleAction createToggleAutoscrollAction() {
        return myUIOptions.getAutoScrollToSourceHandler().createToggleAction();
    }

    @Override
    protected void launchInspections(@Nonnull final AnalysisScope scope) {
        myUIOptions = AnalysisUIOptions.getInstance(getProject()).copy();
        myView =
            new InspectionResultsView(getProject(), getCurrentProfile(), scope, this, new InspectionRVContentProviderImpl(getProject()));
        super.launchInspections(scope);
    }

    @Nonnull
    @Override
    protected PerformInBackgroundOption createOption() {
        return new PerformAnalysisInBackgroundOption(getProject());
    }

    @Override
    protected void notifyInspectionsFinished() {
        if (Application.get().isUnitTestMode()) {
            return;
        }
        UIUtil.invokeLaterIfNeeded(() -> {
            LOG.info("Code inspection finished");

            if (myView != null) {
                if (!myView.update() && !getUIOptions().SHOW_ONLY_DIFF) {
                    NOTIFICATION_GROUP.buildInfo()
                        .content(InspectionLocalize.inspectionNoProblemsMessage())
                        .notify(getProject());
                    close(true);
                }
                else {
                    addView(myView);
                }
            }
        });
    }

    @Override
    protected void runTools(@Nonnull final AnalysisScope scope, boolean runGlobalToolsOnly, boolean isOfflineInspections) {
        final ProgressIndicator progressIndicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
        if (progressIndicator == null) {
            throw new IncorrectOperationException("Must be run under progress");
        }
        final Application application = Application.get();
        if (!isOfflineInspections && application.isDispatchThread()) {
            throw new IncorrectOperationException("Must not start inspections from within EDT");
        }
        if (application.isWriteAccessAllowed()) {
            throw new IncorrectOperationException("Must not start inspections from within write action");
        }
        // in offline inspection application we don't care about global read action
        if (!isOfflineInspections && application.isReadAccessAllowed()) {
            throw new IncorrectOperationException("Must not start inspections from within global read action");
        }
        final InspectionManager inspectionManager = InspectionManager.getInstance(getProject());
        final List<Tools> globalTools = new ArrayList<>();
        final List<Tools> localTools = new ArrayList<>();
        final List<Tools> globalSimpleTools = new ArrayList<>();
        initializeTools(globalTools, localTools, globalSimpleTools);
        appendPairedInspectionsForUnfairTools(globalTools, globalSimpleTools, localTools);

        ((RefManagerImpl)getRefManager()).initializeAnnotators();
        runGlobalTools(scope, inspectionManager, globalTools, isOfflineInspections);

        if (runGlobalToolsOnly) {
            return;
        }

        final Set<VirtualFile> localScopeFiles = scope.toSearchScope() instanceof LocalSearchScope ? new HashSet<>() : null;
        for (Tools tools : globalSimpleTools) {
            GlobalInspectionToolWrapper toolWrapper = (GlobalInspectionToolWrapper)tools.getTool();
            GlobalSimpleInspectionTool tool = (GlobalSimpleInspectionTool)toolWrapper.getTool();
            tool.inspectionStarted(inspectionManager, this, getPresentation(toolWrapper), toolWrapper.getToolState().getState());
        }

        final boolean headlessEnvironment = application.isHeadlessEnvironment();
        final Map<String, InspectionToolWrapper> map = getInspectionWrappersMap(localTools);

        final BlockingQueue<PsiFile> filesToInspect = new ArrayBlockingQueue<>(1000);
        final Queue<PsiFile> filesFailedToInspect = new LinkedBlockingQueue<>();
        // use original progress indicator here since we don't want it to cancel on write action start
        startIterateScope(scope, localScopeFiles, headlessEnvironment, filesToInspect, progressIndicator);

        Predicate<PsiFile> processor = file -> {
            ProgressManager.checkCanceled();
            if (!application.tryRunReadAction(() -> {
                if (!file.isValid()) {
                    return;
                }
                inspectFile(file, inspectionManager, localTools, globalSimpleTools, map, scope.isAnalyzeInjectedCode());
            })) {
                throw new ProcessCanceledException();
            }

            return true;
        };
        while (true) {
            Disposable disposable = Disposable.newDisposable();
            ProgressIndicator wrapper = new SensitiveProgressWrapper(progressIndicator);
            wrapper.start();
            ProgressIndicatorUtils.forceWriteActionPriority(wrapper, disposable);

            try {
                // use wrapper here to cancel early when write action start but do not affect the original indicator
                ((JobLauncherImpl)JobLauncher.getInstance()).processQueue(
                    filesToInspect,
                    filesFailedToInspect,
                    wrapper,
                    TOMBSTONE,
                    processor
                );
                break;
            }
            catch (ProcessCanceledException ignored) {
                progressIndicator.checkCanceled();
                // PCE may be thrown from inside wrapper when write action started
                // go on with the write and then resume processing the rest of the queue
                assert !application.isReadAccessAllowed();
                assert !application.isDispatchThread();

                // wait for write action to complete
                application.runReadAction(EmptyRunnable.getInstance());
            }
            finally {
                Disposer.dispose(disposable);
            }
        }
        progressIndicator.checkCanceled();

        for (Tools tools : globalSimpleTools) {
            GlobalInspectionToolWrapper toolWrapper = (GlobalInspectionToolWrapper)tools.getTool();
            GlobalSimpleInspectionTool tool = (GlobalSimpleInspectionTool)toolWrapper.getTool();
            ProblemDescriptionsProcessor problemDescriptionProcessor = getProblemDescriptionProcessor(toolWrapper, map);
            tool.inspectionFinished(inspectionManager, this, problemDescriptionProcessor, toolWrapper.getState());
        }
    }

    @RequiredReadAction
    private boolean inspectFile(
        @Nonnull final PsiFile file,
        @Nonnull final InspectionManager inspectionManager,
        @Nonnull List<Tools> localTools,
        @Nonnull List<Tools> globalSimpleTools,
        @Nonnull final Map<String, InspectionToolWrapper> wrappersMap,
        boolean inspectInjectedPsi
    ) {
        Document document = PsiDocumentManager.getInstance(getProject()).getDocument(file);
        if (document == null) {
            return true;
        }

        VirtualFile virtualFile = file.getVirtualFile();
        String url = ProjectUtilCore.displayUrlRelativeToProject(
            virtualFile,
            virtualFile.getPresentableUrl(),
            getProject(),
            true,
            false
        );
        incrementJobDoneAmount(getStdJobDescriptors().LOCAL_ANALYSIS, url);

        final LocalInspectionsPass pass = new LocalInspectionsPass(
            file,
            document,
            0,
            file.getTextLength(),
            LocalInspectionsPass.EMPTY_PRIORITY_RANGE,
            true,
            HighlightInfoProcessor.getEmpty()
        );
        try {
            final List<LocalInspectionToolWrapper> lTools = getWrappersFromTools(localTools, file);
            pass.doInspectInBatch(this, inspectionManager, lTools);

            final List<GlobalInspectionToolWrapper> tools = getWrappersFromTools(globalSimpleTools, file);
            JobLauncher.getInstance().invokeConcurrentlyUnderProgress(
                tools,
                myProgressIndicator,
                false,
                new Predicate<>() {
                    @Override
                    @RequiredReadAction
                    public boolean test(GlobalInspectionToolWrapper toolWrapper) {
                        GlobalSimpleInspectionTool tool = (GlobalSimpleInspectionTool)toolWrapper.getTool();
                        ProblemsHolder holder = new ProblemsHolderImpl(inspectionManager, file, false);
                        ProblemDescriptionsProcessor problemDescriptionProcessor = getProblemDescriptionProcessor(toolWrapper, wrappersMap);
                        tool.checkFile(
                            file,
                            inspectionManager,
                            holder,
                            GlobalInspectionContextImpl.this,
                            problemDescriptionProcessor,
                            toolWrapper.getState()
                        );
                        InspectionToolPresentation toolPresentation = getPresentation(toolWrapper);
                        LocalDescriptorsUtil.addProblemDescriptors(
                            holder.getResults(),
                            false,
                            GlobalInspectionContextImpl.this,
                            null,
                            CONVERT,
                            toolPresentation
                        );
                        return true;
                    }
                }
            );
        }
        catch (ProcessCanceledException e) {
            final Throwable cause = e.getCause();
            if (cause == null) {
                throw e;
            }
            LOG.error("In file: " + file, cause);
        }
        catch (IndexNotReadyException e) {
            throw e;
        }
        catch (Throwable e) {
            LOG.error("In file: " + file.getName(), e);
        }
        finally {
            InjectedLanguageManager.getInstance(getProject()).dropFileCaches(file);
        }
        return true;
    }

    private static final PsiFile TOMBSTONE = PsiUtilCore.NULL_PSI_FILE;

    private void startIterateScope(
        @Nonnull final AnalysisScope scope,
        @Nullable final Collection<VirtualFile> localScopeFiles,
        final boolean headlessEnvironment,
        @Nonnull final BlockingQueue<PsiFile> outFilesToInspect,
        @Nonnull final ProgressIndicator progressIndicator
    ) {
        Application.get().executeOnPooledThread((Runnable)() -> {
            try {
                final FileIndex fileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
                scope.accept(file -> {
                    progressIndicator.checkCanceled();
                    if (ProjectCoreUtil.isProjectOrWorkspaceFile(file) || !fileIndex.isInContent(file)) {
                        return true;
                    }
                    final PsiFile[] psiFile = new PsiFile[1];

                    Document document = Application.get().runReadAction((Computable<Document>)() -> {
                        if (getProject().isDisposed()) {
                            throw new ProcessCanceledException();
                        }
                        PsiFile psi = PsiManager.getInstance(getProject()).findFile(file);
                        Document document1 = psi == null ? null : shouldProcess(psi, headlessEnvironment, localScopeFiles);
                        if (document1 != null) {
                            psiFile[0] = psi;
                        }
                        return document1;
                    });
                    //do not inspect binary files
                    if (document != null && psiFile[0] != null) {
                        try {
                            LOG.assertTrue(!Application.get().isReadAccessAllowed());
                            outFilesToInspect.put(psiFile[0]);
                        }
                        catch (InterruptedException e) {
                            LOG.error(e);
                        }
                    }
                    return true;
                });
            }
            catch (ProcessCanceledException e) {
                // ignore, but put tombstone
            }
            finally {
                try {
                    outFilesToInspect.put(TOMBSTONE);
                }
                catch (InterruptedException e) {
                    LOG.error(e);
                }
            }
        });
    }

    private Document shouldProcess(@Nonnull PsiFile file, boolean headlessEnvironment, @Nullable Collection<VirtualFile> localScopeFiles) {
        final VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }
        if (isBinary(file)) {
            return null; //do not inspect binary files
        }

        if (myView == null && !headlessEnvironment) {
            throw new ProcessCanceledException();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Running local inspections on " + virtualFile.getPath());
        }

        if (SingleRootFileViewProvider.isTooLargeForIntelligence(virtualFile)) {
            return null;
        }
        if (localScopeFiles != null && !localScopeFiles.add(virtualFile)) {
            return null;
        }

        return PsiDocumentManager.getInstance(getProject()).getDocument(file);
    }

    private void runGlobalTools(
        @Nonnull final AnalysisScope scope,
        @Nonnull final InspectionManager inspectionManager,
        @Nonnull List<Tools> globalTools,
        boolean isOfflineInspections
    ) {
        LOG.assertTrue(
            !Application.get().isReadAccessAllowed() || isOfflineInspections,
            "Must not run under read action, too unresponsive"
        );
        final List<InspectionToolWrapper> needRepeatSearchRequest = new ArrayList<>();

        final boolean canBeExternalUsages = scope.getScopeType() != AnalysisScope.PROJECT;
        for (Tools tools : globalTools) {
            for (ScopeToolState state : tools.getTools()) {
                final InspectionToolWrapper toolWrapper = state.getTool();
                final GlobalInspectionTool tool = (GlobalInspectionTool)toolWrapper.getTool();
                final InspectionToolPresentation toolPresentation = getPresentation(toolWrapper);
                try {
                    if (tool.isGraphNeeded()) {
                        try {
                            ((RefManagerImpl)getRefManager()).findAllDeclarations();
                        }
                        catch (Throwable e) {
                            getStdJobDescriptors().BUILD_GRAPH.setDoneAmount(0);
                            throw e;
                        }
                    }
                    Runnable action = () -> {
                        tool.runInspection(
                            scope,
                            inspectionManager,
                            GlobalInspectionContextImpl.this,
                            toolPresentation,
                            toolWrapper.getState()
                        );
                        //skip phase when we are sure that scope already contains everything
                        if (canBeExternalUsages && tool.queryExternalUsagesRequests(
                            inspectionManager,
                            GlobalInspectionContextImpl.this,
                            toolPresentation,
                            toolWrapper.getState()
                        )) {
                            needRepeatSearchRequest.add(toolWrapper);
                        }
                    };

                    if (tool.isReadActionNeeded()) {
                        Application.get().runReadAction(action);
                    }
                    else {
                        action.run();
                    }
                }
                catch (ProcessCanceledException | IndexNotReadyException e) {
                    throw e;
                }
                catch (Throwable e) {
                    LOG.error(e);
                }
            }
        }
        for (GlobalInspectionContextExtension extension : myExtensions.values()) {
            try {
                extension.performPostRunActivities(needRepeatSearchRequest, this);
            }
            catch (ProcessCanceledException | IndexNotReadyException e) {
                throw e;
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }
    }

    private void appendPairedInspectionsForUnfairTools(
        @Nonnull List<Tools> globalTools,
        @Nonnull List<Tools> globalSimpleTools,
        @Nonnull List<Tools> localTools
    ) {
        Tools[] larray = localTools.toArray(new Tools[localTools.size()]);
        for (Tools tool : larray) {
            LocalInspectionToolWrapper toolWrapper = (LocalInspectionToolWrapper)tool.getTool();
            LocalInspectionTool localTool = toolWrapper.getTool();
            if (localTool instanceof PairedUnfairLocalInspectionTool) {
                String batchShortName = ((PairedUnfairLocalInspectionTool)localTool).getInspectionForBatchShortName();
                InspectionProfile currentProfile = getCurrentProfile();
                InspectionToolWrapper batchInspection;
                if (currentProfile == null) {
                    batchInspection = null;
                }
                else {
                    final InspectionToolWrapper pairedWrapper = currentProfile.getInspectionTool(batchShortName, getProject());
                    batchInspection = pairedWrapper != null ? pairedWrapper.createCopy() : null;
                }
                if (batchInspection != null && !myTools.containsKey(batchShortName)) {
                    // add to existing inspections to run
                    InspectionTool batchTool = batchInspection.getTool();
                    Tools newTool = new ToolsImpl(batchInspection, batchInspection.getDefaultLevel(), true, true);
                    if (batchTool instanceof LocalInspectionTool) {
                        localTools.add(newTool);
                    }
                    else if (batchTool instanceof GlobalSimpleInspectionTool) {
                        globalSimpleTools.add(newTool);
                    }
                    else if (batchTool instanceof GlobalInspectionTool) {
                        globalTools.add(newTool);
                    }
                    else {
                        throw new AssertionError(batchTool);
                    }
                    myTools.put(batchShortName, newTool);
                    batchInspection.initialize(this);
                }
            }
        }
    }

    @Nonnull
    private static <T extends InspectionToolWrapper> List<T> getWrappersFromTools(@Nonnull List<Tools> localTools, @Nonnull PsiFile file) {
        final List<T> lTools = new ArrayList<>();
        for (Tools tool : localTools) {
            //noinspection unchecked
            final T enabledTool = (T)tool.getEnabledTool(file);
            if (enabledTool != null) {
                lTools.add(enabledTool);
            }
        }
        return lTools;
    }

    @Nonnull
    private ProblemDescriptionsProcessor getProblemDescriptionProcessor(
        @Nonnull final GlobalInspectionToolWrapper toolWrapper,
        @Nonnull final Map<String, InspectionToolWrapper> wrappersMap
    ) {
        return new ProblemDescriptionsProcessor() {
            @Nullable
            @Override
            public CommonProblemDescriptor[] getDescriptions(@Nonnull RefEntity refEntity) {
                return new CommonProblemDescriptor[0];
            }

            @Override
            public void ignoreElement(@Nonnull RefEntity refEntity) {
            }

            @Override
            public void addProblemElement(@Nullable RefEntity refEntity, @Nonnull CommonProblemDescriptor... commonProblemDescriptors) {
                for (CommonProblemDescriptor problemDescriptor : commonProblemDescriptors) {
                    if (!(problemDescriptor instanceof ProblemDescriptor)) {
                        continue;
                    }
                    ProblemGroup problemGroup = ((ProblemDescriptor)problemDescriptor).getProblemGroup();

                    InspectionToolWrapper targetWrapper =
                        problemGroup == null ? toolWrapper : wrappersMap.get(problemGroup.getProblemName());
                    if (targetWrapper != null) { // Else it's switched off
                        InspectionToolPresentation toolPresentation = getPresentation(targetWrapper);
                        toolPresentation.addProblemElement(refEntity, problemDescriptor);
                    }
                }
            }

            @Override
            public RefEntity getElement(@Nonnull CommonProblemDescriptor descriptor) {
                return null;
            }
        };
    }

    @Nonnull
    private static Map<String, InspectionToolWrapper> getInspectionWrappersMap(@Nonnull List<Tools> tools) {
        Map<String, InspectionToolWrapper> name2Inspection = new HashMap<>(tools.size());
        for (Tools tool : tools) {
            InspectionToolWrapper toolWrapper = tool.getTool();
            name2Inspection.put(toolWrapper.getShortName(), toolWrapper);
        }

        return name2Inspection;
    }

    private static final TripleFunction<LocalInspectionTool, PsiElement, GlobalInspectionContext, RefElement> CONVERT =
        (tool, elt, context) -> {
            final PsiNamedElement problemElement = PsiTreeUtil.getNonStrictParentOfType(elt, PsiFile.class);

            RefElement refElement = context.getRefManager().getReference(problemElement);
            if (refElement == null && problemElement != null) {  // no need to lose collected results
                refElement = GlobalInspectionContextUtil.retrieveRefElement(elt, context);
            }
            return refElement;
        };


    @Override
    public void close(boolean noSuspisiousCodeFound) {
        if (!noSuspisiousCodeFound && (myView == null || myView.isRerun())) {
            return;
        }
        AnalysisUIOptions.getInstance(getProject()).save(myUIOptions);
        if (myContent != null) {
            final ContentManager contentManager = getContentManager();
            contentManager.removeContent(myContent, true);
        }
        myView = null;
        super.close(noSuspisiousCodeFound);
    }

    @Override
    public void cleanup() {
        ((InspectionManagerImpl)InspectionManager.getInstance(getProject())).closeRunningContext(this);
        for (Tools tools : myTools.values()) {
            for (ScopeToolState state : tools.getTools()) {
                InspectionToolWrapper toolWrapper = state.getTool();
                getPresentation(toolWrapper).finalCleanup();
            }
        }
        super.cleanup();
    }

    public void refreshViews() {
        if (myView != null) {
            myView.updateView(false);
        }
    }

    private final ConcurrentMap<InspectionToolWrapper, InspectionToolPresentation> myPresentationMap = ContainerUtil.newConcurrentMap();

    @Nonnull
    public InspectionToolPresentation getPresentation(@Nonnull InspectionToolWrapper toolWrapper) {
        InspectionToolPresentation presentation = myPresentationMap.get(toolWrapper);
        if (presentation == null) {
            Class<?> presentationClass = DefaultInspectionToolPresentation.class;
            try {
                Constructor<?> constructor =
                    presentationClass.getConstructor(InspectionToolWrapper.class, GlobalInspectionContextImpl.class);
                presentation = (InspectionToolPresentation)constructor.newInstance(toolWrapper, this);
            }
            catch (Exception e) {
                LOG.error(e);

                presentation = new DefaultInspectionToolPresentation(toolWrapper, this);
            }

            presentation = ConcurrencyUtil.cacheOrGet(myPresentationMap, toolWrapper, presentation);
        }
        return presentation;
    }

    @Override
    public void codeCleanup(
        @Nonnull final Project project,
        @Nonnull final AnalysisScope scope,
        @Nonnull final InspectionProfile profile,
        @Nullable final String commandName,
        @Nullable final Runnable postRunnable,
        final boolean modal
    ) {
        Task task = modal
            ? new Task.Modal(project, ActionLocalize.actionInspectcodeText().map(Presentation.NO_MNEMONIC), true) {
                @Override
                public void run(@Nonnull ProgressIndicator indicator) {
                    cleanup(scope, profile, project, postRunnable, commandName);
                }
            }
            : new Task.Backgroundable(project, ActionLocalize.actionInspectcodeText().map(Presentation.NO_MNEMONIC), true) {
                @Override
                public void run(@Nonnull ProgressIndicator indicator) {
                    cleanup(scope, profile, project, postRunnable, commandName);
                }
            };
        ProgressManager.getInstance().run(task);
    }

    private void cleanup(
        @Nonnull AnalysisScope scope,
        @Nonnull InspectionProfile profile,
        @Nonnull final Project project,
        @Nullable final Runnable postRunnable,
        @Nullable final String commandName
    ) {
        final int fileCount = scope.getFileCount();
        final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
        final List<LocalInspectionToolWrapper> lTools = new ArrayList<>();

        final LinkedHashMap<PsiFile, List<HighlightInfo>> results = new LinkedHashMap<>();

        final SearchScope searchScope = scope.toSearchScope();
        final TextRange range;
        if (searchScope instanceof LocalSearchScope localSearchScope) {
            final PsiElement[] elements = localSearchScope.getScope();
            range = elements.length == 1 ? Application.get().runReadAction((Computable<TextRange>)() -> elements[0].getTextRange()) : null;
        }
        else {
            range = null;
        }
        final Iterable<Tools> inspectionTools = ContainerUtil.filter(
            profile.getAllEnabledInspectionTools(project),
            tools -> {
                assert tools != null;
                return tools.getTool().getTool() instanceof CleanupLocalInspectionTool;
            }
        );
        scope.accept(new PsiElementVisitor() {
            private int myCount;

            @Override
            @RequiredReadAction
            public void visitFile(PsiFile file) {
                if (progressIndicator != null) {
                    progressIndicator.setFraction(((double)++myCount) / fileCount);
                }
                if (isBinary(file)) {
                    return;
                }
                for (final Tools tools : inspectionTools) {
                    final InspectionToolWrapper tool = tools.getEnabledTool(file);
                    if (tool instanceof LocalInspectionToolWrapper) {
                        lTools.add((LocalInspectionToolWrapper)tool);
                        tool.initialize(GlobalInspectionContextImpl.this);
                    }
                }

                if (!lTools.isEmpty()) {
                    final LocalInspectionsPass pass = new LocalInspectionsPass(
                        file,
                        PsiDocumentManager.getInstance(project).getDocument(file),
                        range != null ? range.getStartOffset() : 0,
                        range != null ? range.getEndOffset() : file.getTextLength(),
                        LocalInspectionsPass.EMPTY_PRIORITY_RANGE,
                        true,
                        HighlightInfoProcessor.getEmpty()
                    );
                    Runnable runnable =
                        () -> pass.doInspectInBatch(GlobalInspectionContextImpl.this, InspectionManager.getInstance(project), lTools);
                    Application.get().runReadAction(runnable);
                    final List<HighlightInfo> infos = pass.getInfos();
                    if (searchScope instanceof LocalSearchScope localSearchScope) {
                        for (Iterator<HighlightInfo> iterator = infos.iterator(); iterator.hasNext(); ) {
                            final HighlightInfo info = iterator.next();
                            final TextRange infoRange = new TextRange(info.getStartOffset(), info.getEndOffset());
                            if (!localSearchScope.containsRange(file, infoRange)) {
                                iterator.remove();
                            }
                        }
                    }
                    if (!infos.isEmpty()) {
                        results.put(file, infos);
                    }
                }
            }
        });

        if (results.isEmpty()) {
            UIUtil.invokeLaterIfNeeded(() -> {
                if (commandName != null) {
                    NOTIFICATION_GROUP.buildInfo()
                        .content(InspectionLocalize.inspectionNoProblemsMessage())
                        .notify(getProject());
                }
                if (postRunnable != null) {
                    postRunnable.run();
                }
            });
            return;
        }
        Runnable runnable = () -> {
            if (!FileModificationService.getInstance().preparePsiElementsForWrite(results.keySet())) {
                return;
            }

            final SequentialModalProgressTask progressTask = new SequentialModalProgressTask(project, "Code Cleanup", true);
            progressTask.setMinIterationTime(200);
            progressTask.setTask(new SequentialCleanupTask(project, results, progressTask));
            CommandProcessor.getInstance().newCommand()
                .project(project)
                .name(LocalizeValue.ofNullable(commandName))
                .inGlobalUndoActionIf(commandName != null)
                .inLater()
                .run(() -> {
                    Application.get().runWriteAction(() -> ProgressManager.getInstance().run(progressTask));
                    if (postRunnable != null) {
                        Application.get().invokeLater(postRunnable);
                    }
                });
        };

        getProject().getUIAccess().give(runnable);
    }

    private static boolean isBinary(@Nonnull PsiFile file) {
        return file instanceof PsiBinaryFile || file.getFileType().isBinary();
    }
}
