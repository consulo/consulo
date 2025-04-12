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

package consulo.language.editor.impl.inspection;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.impl.internal.progress.ProgressWrapper;
import consulo.application.progress.*;
import consulo.component.ProcessCanceledException;
import consulo.content.scope.NamedScope;
import consulo.language.editor.impl.inspection.reference.RefElementImpl;
import consulo.language.editor.impl.inspection.reference.RefManagerImpl;
import consulo.language.editor.impl.inspection.scheme.GlobalInspectionToolWrapper;
import consulo.language.editor.impl.inspection.scheme.LocalInspectionToolWrapper;
import consulo.language.editor.impl.internal.daemon.DaemonProgressIndicator;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.impl.internal.inspection.scheme.ToolsImpl;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.reference.RefManager;
import consulo.language.editor.inspection.scheme.*;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.psi.*;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Sets;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.*;
import java.util.function.Supplier;

public class GlobalInspectionContextBase extends UserDataHolderBase implements GlobalInspectionContext {
    private static final Logger LOG = Logger.getInstance(GlobalInspectionContextBase.class);
    private static final HashingStrategy<Tools> TOOLS_HASHING_STRATEGY = new HashingStrategy<Tools>() {
        @Override
        public int hashCode(Tools object) {
            return object.getShortName().hashCode();
        }

        @Override
        public boolean equals(Tools o1, Tools o2) {
            return o1.getShortName().equals(o2.getShortName());
        }
    };

    private RefManager myRefManager;

    private AnalysisScope myCurrentScope;
    @Nonnull
    private final Project myProject;
    private final List<JobDescriptor> myJobDescriptors = new ArrayList<JobDescriptor>();

    private final StdJobDescriptors myStdJobDescriptors = new StdJobDescriptors();
    protected ProgressIndicator myProgressIndicator = new EmptyProgressIndicator();

    private InspectionProfile myExternalProfile;

    protected final Map<Key, GlobalInspectionContextExtension> myExtensions = new HashMap<Key, GlobalInspectionContextExtension>();

    protected final Map<String, Tools> myTools = new HashMap<String, Tools>();

    @NonNls
    public static final String LOCAL_TOOL_ATTRIBUTE = "is_local_tool";

    public GlobalInspectionContextBase(@Nonnull Project project) {
        myProject = project;

        for (InspectionExtensionsFactory factory : InspectionExtensionsFactory.EP_NAME.getExtensionList()) {
            final GlobalInspectionContextExtension extension = factory.createGlobalInspectionContextExtension();
            myExtensions.put(extension.getID(), extension);
        }
    }

    AnalysisScope getCurrentScope() {
        return myCurrentScope;
    }

    @Override
    @Nonnull
    public Project getProject() {
        return myProject;
    }

    @Override
    public <T> T getExtension(@Nonnull final Key<T> key) {
        //noinspection unchecked
        return (T)myExtensions.get(key);
    }

    public InspectionProfile getCurrentProfile() {
        if (myExternalProfile != null) {
            return myExternalProfile;
        }
        InspectionManager managerEx = InspectionManager.getInstance(myProject);
        String currentProfile = managerEx.getCurrentProfile();
        final InspectionProjectProfileManager inspectionProfileManager = InspectionProjectProfileManager.getInstance(myProject);
        Profile profile = inspectionProfileManager.getProfile(currentProfile, false);
        if (profile == null) {
            profile = InspectionProfileManager.getInstance().getProfile(currentProfile);
            if (profile != null) {
                return (InspectionProfile)profile;
            }

            final String[] availableProfileNames = inspectionProfileManager.getAvailableProfileNames();
            if (availableProfileNames.length == 0) {
                //can't be
                return null;
            }
            profile = inspectionProfileManager.getProfile(availableProfileNames[0]);
        }
        return (InspectionProfile)profile;
    }

    @Override
    public boolean isSuppressed(@Nonnull RefEntity entity, @Nonnull String id) {
        return entity instanceof RefElementImpl && ((RefElementImpl)entity).isSuppressed(id);
    }

    @Override
    public boolean shouldCheck(@Nonnull RefEntity entity, @Nonnull GlobalInspectionTool tool) {
        return !(entity instanceof RefElementImpl) || isToCheckMember((RefElementImpl)entity, tool);
    }

    @Override
    public boolean isSuppressed(@Nonnull PsiElement element, @Nonnull String id) {
        final RefManagerImpl refManager = (RefManagerImpl)getRefManager();
        if (refManager.isDeclarationsFound()) {
            final RefElement refElement = refManager.getReference(element);
            return refElement instanceof RefElementImpl && ((RefElementImpl)refElement).isSuppressed(id);
        }
        return SuppressionUtil.isSuppressed(element, id);
    }


    public void cleanupTools() {
        myProgressIndicator.cancel();
        for (GlobalInspectionContextExtension extension : myExtensions.values()) {
            extension.cleanup();
        }

        for (Tools tools : myTools.values()) {
            for (ScopeToolState state : tools.getTools()) {
                InspectionToolWrapper toolWrapper = state.getTool();
                toolWrapper.cleanup(myProject);
            }
        }
        myTools.clear();

  /*  EntryPointsManager entryPointsManager = EntryPointsManager.getInstance(getProject());
    if (entryPointsManager != null) {
      entryPointsManager.cleanup();
    }
       */
        if (myRefManager != null) {
            ((RefManagerImpl)myRefManager).cleanup();
            myRefManager = null;
            if (myCurrentScope != null) {
                myCurrentScope.invalidate();
                myCurrentScope = null;
            }
        }
        myJobDescriptors.clear();
    }

    public void setCurrentScope(@Nonnull AnalysisScope currentScope) {
        myCurrentScope = currentScope;
    }

    public void doInspections(@Nonnull final AnalysisScope scope) {
        if (!GlobalInspectionContextUtil.canRunInspections(myProject, true)) {
            return;
        }

        cleanup();

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                myCurrentScope = scope;
                launchInspections(scope);
            }
        }, ApplicationManager.getApplication().getDisposed());
    }


    @Override
    @Nonnull
    public RefManager getRefManager() {
        if (myRefManager == null) {
            myRefManager = ApplicationManager.getApplication().runReadAction((Supplier<RefManagerImpl>)() -> new RefManagerImpl(
                myProject, myCurrentScope, GlobalInspectionContextBase.this));
        }
        return myRefManager;
    }

    @Nullable
    @Override
    public Tools getTools(@Nonnull String shortName) {
        return myTools.get(shortName);
    }

    @Override
    public boolean isToCheckFile(PsiFile file, @Nonnull InspectionTool tool) {
        final Tools tools = myTools.get(tool.getShortName());
        if (tools != null && file != null) {
            for (ScopeToolState state : tools.getTools()) {
                final NamedScope namedScope = state.getScope(file.getProject());
                if (namedScope == null
                    || namedScope.getValue().contains(
                        file.getVirtualFile(),
                        file.getProject(),
                        getCurrentProfile().getProfileManager().getScopesManager()
                    )
                ) {
                    return state.isEnabled() && state.getTool().getTool() == tool;
                }
            }
        }
        return false;
    }

    protected void launchInspections(@Nonnull final AnalysisScope scope) {
        UIAccess.assertIsUIThread();
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();

        LOG.info("Code inspection started");
        ProgressManager.getInstance()
            .run(new Task.Backgroundable(getProject(), InspectionsBundle.message("inspection.progress.title"), true, createOption()) {
                @Override
                public void run(@Nonnull ProgressIndicator indicator) {
                    performInspectionsWithProgress(scope, false, false);
                }

                @RequiredUIAccess
                @Override
                public void onSuccess() {
                    notifyInspectionsFinished();
                }
            });
    }

    @Nonnull
    protected PerformInBackgroundOption createOption() {
        return new PerformInBackgroundOption() {
            @Override
            public boolean shouldStartInBackground() {
                return true;
            }

            @Override
            public void processSentToBackground() {

            }
        };
    }

    protected void notifyInspectionsFinished() {
    }

    public void performInspectionsWithProgress(
        @Nonnull final AnalysisScope scope,
        final boolean runGlobalToolsOnly,
        final boolean isOfflineInspections
    ) {
        myProgressIndicator = ProgressManager.getInstance().getProgressIndicator();
        if (myProgressIndicator == null) {
            throw new IllegalStateException("Inspections must be run under progress");
        }
        final PsiManager psiManager = PsiManager.getInstance(myProject);
        //init manager in read action
        RefManagerImpl refManager = (RefManagerImpl)getRefManager();
        try {
            psiManager.startBatchFilesProcessingMode();
            refManager.inspectionReadActionStarted();
            getStdJobDescriptors().BUILD_GRAPH.setTotalAmount(scope.getFileCount());
            getStdJobDescriptors().LOCAL_ANALYSIS.setTotalAmount(scope.getFileCount());
            getStdJobDescriptors().FIND_EXTERNAL_USAGES.setTotalAmount(0);
            //to override current progress in order to hide useless messages/%
            ProgressManager.getInstance().executeProcessUnderProgress(new Runnable() {
                @Override
                public void run() {
                    runTools(scope, runGlobalToolsOnly, isOfflineInspections);
                }
            }, ProgressWrapper.wrap(myProgressIndicator));
        }
        catch (ProcessCanceledException e) {
            cleanup();
            throw e;
        }
        catch (IndexNotReadyException e) {
            cleanup();
            DumbService.getInstance(myProject).showDumbModeNotification("Usage search is not available until indices are ready");
            throw new ProcessCanceledException();
        }
        catch (Throwable e) {
            LOG.error(e);
        }
        finally {
            refManager.inspectionReadActionFinished();
            psiManager.finishBatchFilesProcessingMode();
        }
    }

    protected void runTools(@Nonnull AnalysisScope scope, boolean runGlobalToolsOnly, boolean isOfflineInspections) {
    }


    public void initializeTools(
        @Nonnull List<Tools> outGlobalTools,
        @Nonnull List<Tools> outLocalTools,
        @Nonnull List<Tools> outGlobalSimpleTools
    ) {
        final List<Tools> usedTools = getUsedTools();
        for (Tools currentTools : usedTools) {
            final String shortName = currentTools.getShortName();
            myTools.put(shortName, currentTools);
            InspectionToolWrapper toolWrapper = currentTools.getTool();
            classifyTool(outGlobalTools, outLocalTools, outGlobalSimpleTools, currentTools, toolWrapper);

            for (ScopeToolState state : currentTools.getTools()) {
                state.getTool().initialize(this);
            }

            JobDescriptor[] jobDescriptors = toolWrapper.getJobDescriptors(this);
            for (JobDescriptor jobDescriptor : jobDescriptors) {
                appendJobDescriptor(jobDescriptor);
            }
        }
        for (GlobalInspectionContextExtension extension : myExtensions.values()) {
            extension.performPreRunActivities(outGlobalTools, outLocalTools, this);
        }
    }

    @Nonnull
    protected List<Tools> getUsedTools() {
        InspectionProfileImpl profile = new InspectionProfileImpl((InspectionProfileImpl)getCurrentProfile());
        List<Tools> tools = profile.getAllEnabledInspectionTools(myProject);
        Set<InspectionToolWrapper> dependentTools = new LinkedHashSet<InspectionToolWrapper>();
        for (Tools tool : tools) {
            profile.collectDependentInspections(tool.getTool(), dependentTools, getProject());
        }

        if (dependentTools.isEmpty()) {
            return tools;
        }
        Set<Tools> set = Sets.newHashSet(tools, TOOLS_HASHING_STRATEGY);
        set.addAll(ContainerUtil.map(dependentTools, it -> new ToolsImpl(it, it.getDefaultLevel(), true, true)));
        return new ArrayList<Tools>(set);
    }

    private static void classifyTool(
        @Nonnull List<Tools> outGlobalTools,
        @Nonnull List<Tools> outLocalTools,
        @Nonnull List<Tools> outGlobalSimpleTools,
        @Nonnull Tools currentTools,
        @Nonnull InspectionToolWrapper toolWrapper
    ) {
        if (toolWrapper instanceof LocalInspectionToolWrapper) {
            outLocalTools.add(currentTools);
        }
        else if (toolWrapper instanceof GlobalInspectionToolWrapper) {
            if (toolWrapper.getTool() instanceof GlobalSimpleInspectionTool) {
                outGlobalSimpleTools.add(currentTools);
            }
            else if (toolWrapper.getTool() instanceof GlobalInspectionTool) {
                outGlobalTools.add(currentTools);
            }
            else {
                throw new RuntimeException("unknown global tool " + toolWrapper);
            }
        }
        else {
            throw new RuntimeException("unknown tool " + toolWrapper);
        }
    }

    @Nonnull
    public Map<String, Tools> getTools() {
        return myTools;
    }

    private void appendJobDescriptor(@Nonnull JobDescriptor job) {
        if (!myJobDescriptors.contains(job)) {
            myJobDescriptors.add(job);
            job.setDoneAmount(0);
        }
    }

    public void codeCleanup(
        @Nonnull Project project,
        @Nonnull AnalysisScope scope,
        @Nonnull InspectionProfile profile,
        @jakarta.annotation.Nullable String commandName,
        @Nullable Runnable postRunnable,
        final boolean modal
    ) {
    }

    public static void codeCleanup(@Nonnull Project project, @Nonnull AnalysisScope scope, @Nullable Runnable runnable) {
        GlobalInspectionContextBase globalContext =
            (GlobalInspectionContextBase)InspectionManager.getInstance(project).createNewGlobalContext(false);
        final InspectionProfile profile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
        globalContext.codeCleanup(project, scope, profile, null, runnable, false);
    }

    public static void cleanupElements(@Nonnull final Project project, @Nullable final Runnable runnable, @Nonnull PsiElement... scope) {
        final List<SmartPsiElementPointer<PsiElement>> elements = new ArrayList<SmartPsiElementPointer<PsiElement>>();
        final SmartPointerManager manager = SmartPointerManager.getInstance(project);
        for (PsiElement element : scope) {
            elements.add(manager.createSmartPsiElementPointer(element));
        }

        Runnable cleanupRunnable = new Runnable() {
            @Override
            public void run() {
                final List<PsiElement> psiElements = new ArrayList<PsiElement>();
                for (SmartPsiElementPointer<PsiElement> element : elements) {
                    PsiElement psiElement = element.getElement();
                    if (psiElement != null && psiElement.isPhysical()) {
                        psiElements.add(psiElement);
                    }
                }
                if (psiElements.isEmpty()) {
                    return;
                }
                GlobalInspectionContextBase globalContext =
                    (GlobalInspectionContextBase)InspectionManager.getInstance(project).createNewGlobalContext(false);
                final InspectionProfile profile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
                AnalysisScope analysisScope =
                    new AnalysisScope(new LocalSearchScope(psiElements.toArray(new PsiElement[psiElements.size()])), project);
                globalContext.codeCleanup(project, analysisScope, profile, null, runnable, true);
            }
        };

        Application application = ApplicationManager.getApplication();
        if (application.isWriteAccessAllowed() && !application.isUnitTestMode()) {
            application.invokeLater(cleanupRunnable);
        }
        else {
            cleanupRunnable.run();
        }
    }

    public void close(boolean noSuspisiousCodeFound) {
        cleanup();
    }

    @Override
    public void cleanup() {
        cleanupTools();
    }

    @Override
    public void incrementJobDoneAmount(@Nonnull JobDescriptor job, @Nonnull String message) {
        if (myProgressIndicator == null) {
            return;
        }

        ProgressManager.checkCanceled();

        int old = job.getDoneAmount();
        job.setDoneAmount(old + 1);

        float totalProgress = getTotalProgress();

        myProgressIndicator.setFraction(totalProgress);
        myProgressIndicator.setText(job.getDisplayName() + " " + message);
    }

    private float getTotalProgress() {
        float totalDone = 0;
        int totalTotal = 0;
        for (JobDescriptor jobDescriptor : myJobDescriptors) {
            totalDone += jobDescriptor.getDoneAmount();
            totalTotal += jobDescriptor.getTotalAmount();
        }
        return totalTotal == 0 ? 1 : totalDone / totalTotal;
    }

    public void setExternalProfile(InspectionProfile profile) {
        myExternalProfile = profile;
    }

    @Override
    @Nonnull
    public StdJobDescriptors getStdJobDescriptors() {
        return myStdJobDescriptors;
    }

    public static void assertUnderDaemonProgress() {
        ProgressIndicator indicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
        ProgressIndicator original = indicator == null ? null : ProgressWrapper.unwrapAll(indicator);
        if (!(original instanceof DaemonProgressIndicator)) {
            throw new IllegalStateException("must be run under DaemonProgressIndicator, but got: " + (original == null ? "null" : ": " + original.getClass()) + ": " + original);
        }
    }
}
