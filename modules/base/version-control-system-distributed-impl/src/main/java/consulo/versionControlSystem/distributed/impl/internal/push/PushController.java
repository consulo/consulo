/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.impl.internal.push;

import consulo.application.progress.ProgressIndicator;
import consulo.application.util.concurrent.SequentialTaskExecutor;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.distributed.push.CommitLoader;
import consulo.versionControlSystem.distributed.push.CustomPushOptionsPanelFactory;
import consulo.versionControlSystem.distributed.push.OutgoingCommitsProvider;
import consulo.versionControlSystem.distributed.push.OutgoingResult;
import consulo.versionControlSystem.distributed.push.PrePushHandler;
import consulo.versionControlSystem.distributed.push.PushInfo;
import consulo.versionControlSystem.distributed.push.PushSource;
import consulo.versionControlSystem.distributed.push.PushSpec;
import consulo.versionControlSystem.distributed.push.PushSupport;
import consulo.versionControlSystem.distributed.push.PushTarget;
import consulo.versionControlSystem.distributed.push.PushTargetEditorListener;
import consulo.versionControlSystem.distributed.push.PushTargetPanel;
import consulo.versionControlSystem.distributed.push.Pusher;
import consulo.versionControlSystem.distributed.push.VcsError;
import consulo.versionControlSystem.distributed.push.VcsPushOptionValue;
import consulo.versionControlSystem.distributed.push.VcsPushOptionsPanel;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public final class PushController implements Disposable {
    private static final Logger LOG = Logger.getInstance(PushController.class);

    private final Project myProject;
    private final @Nullable PushSource myPushSource;
    private final @Nullable PushTarget myPushTarget;
    private final Collection<? extends Repository> myAllRepos;
    private final List<? extends Repository> myPreselectedRepositories;
    private final List<PushSupport<Repository, PushSource, PushTarget>> myPushSupports;
    private final UnifiedPushDialog myDialog;
    private final @Nullable Repository myCurrentlyOpenedRepository;
    private final boolean mySingleRepoProject;
    private final ExecutorService myExecutorService = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("DVCS Push");

    private final List<PushRepoModel<Repository, PushSource, PushTarget>> myModels = new ArrayList<>();
    private final PushLog myPushLog;

    private List<PushRepoModel<?, ?, ?>> myPriorityRepositories = List.of();
    private List<PushRepoModel<?, ?, ?>> myOtherRepositories = List.of();

    private boolean myHasCommitWarning;

    @RequiredUIAccess
    public PushController(Project project,
                          UnifiedPushDialog dialog,
                          Collection<? extends Repository> allRepos,
                          List<? extends Repository> preselectedRepositories,
                          @Nullable Repository currentRepo,
                          @Nullable PushSource pushSource,
                          @Nullable PushTarget target,
                          Disposable uiDisposable) {
        myProject = project;
        myAllRepos = allRepos;
        myPreselectedRepositories = preselectedRepositories;
        myCurrentlyOpenedRepository = currentRepo;
        myPushSource = pushSource;
        myPushTarget = target;
        myPushSupports = getAffectedSupports();
        mySingleRepoProject = isSingleRepoProject();
        myDialog = dialog;
        Disposer.register(uiDisposable, this);
        createTreeModel();
        myPushLog = new PushLog(myProject, this, List.copyOf(myModels), isSyncStrategiesAllowed(), uiDisposable);
        processRepositories();
    }

    private boolean isSyncStrategiesAllowed() {
        if (mySingleRepoProject) {
            return false;
        }
        for (PushSupport<Repository, PushSource, PushTarget> support : myPushSupports) {
            if (!support.mayChangeTargetsSync()) {
                return false;
            }
        }
        return true;
    }

    private boolean isSingleRepoProject() {
        return myAllRepos.size() == 1;
    }

    @SuppressWarnings("unchecked")
    private List<PushSupport<Repository, PushSource, PushTarget>> getAffectedSupports() {
        Set<AbstractVcs> vcss = new LinkedHashSet<>();
        for (Repository repository : myAllRepos) {
            vcss.add(repository.getVcs());
        }
        List<PushSupport<Repository, PushSource, PushTarget>> supports = new ArrayList<>();
        for (AbstractVcs vcs : vcss) {
            PushSupport<Repository, PushSource, PushTarget> support = DvcsUtil.getPushSupport(vcs);
            if (support != null) {
                supports.add(support);
            }
        }
        return supports;
    }

    @RequiredUIAccess
    private void processRepositories() {
        Map<PushRepoModel<?, ?, ?>, Boolean> priorityLoading = new LinkedHashMap<>();
        List<PushRepoModel<?, ?, ?>> others = new ArrayList<>();
        PushRepoModel<Repository, PushSource, PushTarget> currentRepoModel = findModelByRepo(myCurrentlyOpenedRepository);
        if (currentRepoModel != null) {
            if (isPreChecked(currentRepoModel) || myPreselectedRepositories.isEmpty()) {
                priorityLoading.put(currentRepoModel, Boolean.TRUE);
                currentRepoModel.setChecked(true);
            }
        }

        for (PushRepoModel<Repository, PushSource, PushTarget> model : myModels) {
            if (isPreChecked(model)) {
                priorityLoading.putIfAbsent(model, Boolean.TRUE);
                model.setChecked(true);
            }
            else if (model.getSupport().shouldRequestIncomingChangesForNotCheckedRepositories()) {
                others.add(model);
            }
        }
        myPushLog.syncChecks();

        if (myPreselectedRepositories.isEmpty()) {
            myPushLog.highlightNodeOrFirst(currentRepoModel);
        }
        myPriorityRepositories = new ArrayList<>(priorityLoading.keySet());
        myOtherRepositories = others;
    }

    @RequiredUIAccess
    public void startLoadingCommits() {
        for (PushRepoModel<?, ?, ?> model : myPriorityRepositories) {
            loadCommits(model, true);
        }
        for (PushRepoModel<?, ?, ?> model : myOtherRepositories) {
            loadCommits(model, true);
        }
    }

    private boolean isPreChecked(PushRepoModel<?, ?, ?> model) {
        return model.getSupport().getRepositoryManager().isSyncEnabled() || preselectByUser(model.getRepository());
    }

    private @Nullable PushRepoModel<Repository, PushSource, PushTarget> findModelByRepo(@Nullable Repository repository) {
        if (repository == null) {
            return null;
        }
        for (PushRepoModel<Repository, PushSource, PushTarget> model : myModels) {
            if (model.getRepository().getRoot().equals(repository.getRoot())) {
                return model;
            }
        }
        return null;
    }

    @RequiredUIAccess
    private void createTreeModel() {
        for (Repository repository : DvcsUtil.sortRepositories(myAllRepos)) {
            PushSupport<Repository, PushSource, PushTarget> support = getPushSupportByRepository(repository);
            if (support != null) {
                PushSource source = myPushSource != null ? myPushSource : support.getSource(repository);
                if (source != null) {
                    PushTarget target = myPushTarget != null ? myPushTarget : getDefaultTarget(support, repository, source);
                    createRepoNode(repository, source, target, support);
                }
            }
        }
    }

    private static <R extends Repository, S extends PushSource, T extends PushTarget> @Nullable T getDefaultTarget(PushSupport<R, S, T> support,
                                                                                                                 R repository,
                                                                                                                 S source) {
        T target = support.getDefaultTarget(repository, source);
        return target != null ? target : support.getDefaultTarget(repository);
    }

    private @Nullable PushSupport<Repository, PushSource, PushTarget> getPushSupportByRepository(Repository repository) {
        for (PushSupport<Repository, PushSource, PushTarget> support : myPushSupports) {
            if (support.getVcs().equals(repository.getVcs())) {
                return support;
            }
        }
        return null;
    }

    @RequiredUIAccess
    private void createRepoNode(Repository repository,
                                PushSource source,
                                @Nullable PushTarget target,
                                PushSupport<Repository, PushSource, PushTarget> pushSupport) {
        String repoName = getDisplayedRepoName(repository);
        PushTargetPanel<PushTarget> pushTargetPanel = pushSupport.createTargetPanel(repository, source, target);
        Disposer.register(this, pushTargetPanel);

        PushRepoModel<Repository, PushSource, PushTarget> model =
            new PushRepoModel<>(repository, pushSupport, mySingleRepoProject, repoName, source, target, pushTargetPanel);
        if (target == null) {
            model.setError(VcsError.createEmptyTargetError(repoName));
        }

        pushTargetPanel.setFireOnChangeAction(() -> {
            fireOnChange(model);
            myPushLog.refreshRepository(model);
        });
        pushTargetPanel.addTargetEditorListener(new PushTargetEditorListener() {
            @Override
            public void onTargetInEditModeChanged(String value) {
                myPushLog.fireEditorUpdated(value);
            }
        });

        myModels.add(model);
    }

    @RequiredUIAccess
    void fireOnChange(PushRepoModel<Repository, PushSource, PushTarget> model) {
        model.getTargetPanel().fireOnChange();
        PushTarget target = model.getTargetPanel().getValue();
        if (target == null) {
            return;
        }
        onTargetChanged(model, target);
    }

    @RequiredUIAccess
    private void onTargetChanged(PushRepoModel<Repository, PushSource, PushTarget> model, PushTarget newTarget) {
        setChecked(model, true);
        if (!newTarget.equals(model.getTarget()) || model.hasError() || !model.hasCommitInfo()) {
            model.setTarget(newTarget);
            model.clearErrors();
            loadCommits(model, false);
        }
    }

    @RequiredUIAccess
    void onSelectionChanged(PushRepoModel<?, ?, ?> model, boolean isSelected) {
        model.setChecked(isSelected);
        myDialog.updateOkActions();
        if (isSelected && !model.hasCommitInfo() && !model.getSupport().shouldRequestIncomingChangesForNotCheckedRepositories()) {
            loadCommits(model, false);
        }
    }

    @RequiredUIAccess
    private void setChecked(PushRepoModel<?, ?, ?> model, boolean checked) {
        model.setChecked(checked);
        myPushLog.setChecked(model, checked);
    }

    private String getDisplayedRepoName(Repository repository) {
        String name = DvcsUtil.getShortRepositoryName(repository);
        int slash = name.lastIndexOf(File.separatorChar);
        if (slash < 0) {
            return name;
        }
        String candidate = name.substring(slash + 1);
        return !containedInOtherNames(repository, candidate) ? candidate : name;
    }

    private boolean containedInOtherNames(Repository except, String candidate) {
        for (Repository repository : myAllRepos) {
            if (!repository.equals(except) && repository.getRoot().getName().equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    public boolean isPushAllowed() {
        if (myPushLog.isEditing()) {
            return false;
        }

        for (PushRepoModel<Repository, PushSource, PushTarget> model : myModels) {
            if (model.isSelected() && isPushAllowed(model)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasCommitWarnings() {
        return myHasCommitWarning;
    }

    private static boolean isPushAllowed(PushRepoModel<Repository, PushSource, PushTarget> model) {
        PushTarget target = model.getTarget();
        if (target == null) {
            return false;
        }
        return model.getSupport().canBePushed(model.getRepository(), model.getSource(), target);
    }

    @RequiredUIAccess
    void reloadCommits(PushRepoModel<?, ?, ?> model) {
        setChecked(model, true);
        loadCommits(model, false);
    }

    @RequiredUIAccess
    private <R extends Repository, S extends PushSource, T extends PushTarget> void loadCommits(PushRepoModel<R, S, T> model,
                                                                                               boolean initial) {
        if (myDialog.isDisposed()) {
            return;
        }

        model.cancelLoading();
        model.setEnabled(true);

        T target = model.getTarget();
        if (target == null) {
            model.stopLoading();
            myPushLog.refreshRepository(model);
            return;
        }

        PushSupport<R, S, T> support = model.getSupport();
        UIAccess uiAccess = UIAccess.current();
        PushSpec<S, T> pushSpec = new PushSpec<>(model.getSource(), target);
        Future<?> future = myExecutorService.submit(() -> {
            OutgoingResult outgoing;
            try {
                outgoing = support.getOutgoingCommitsProvider().getOutgoingCommits(model.getRepository(), pushSpec, initial);
            }
            catch (ProcessCanceledException ignore) {
                return;
            }
            catch (Exception e) {
                LOG.error(e);
                return;
            }

            uiAccess.give(() -> applyLoaded(model, outgoing, initial));
        });
        model.startLoading(future);
        myPushLog.refreshRepository(model);
    }

    @RequiredUIAccess
    private void applyLoaded(PushRepoModel<?, ?, ?> model, OutgoingResult outgoing, boolean initial) {
        if (myDialog.isDisposed()) {
            return;
        }

        List<VcsError> errors = outgoing.getErrors();
        boolean shouldBeSelected;
        if (!errors.isEmpty()) {
            shouldBeSelected = false;
            model.setLoadedCommits(List.of());
            model.setShownErrors(errors);
            if (model.isSelected()) {
                setChecked(model, false);
            }
        }
        else {
            List<? extends VcsFullCommitDetails> commits = outgoing.getCommits();
            model.setLoadedCommits(commits);
            model.setShownErrors(List.of());
            shouldBeSelected = shouldSelectNodeAfterLoad(model);
        }

        model.stopLoading();
        myPushLog.setChildren(model);
        if (errors.isEmpty() && !model.getLoadedCommits().isEmpty() && shouldBeSelected) {
            myPushLog.selectIfNothingSelected(model);
        }
        if (shouldBeSelected) {
            setChecked(model, true);
        }
        else if (initial) {
            setChecked(model, false);
        }
        myDialog.updateOkActions();
    }

    private boolean shouldSelectNodeAfterLoad(PushRepoModel<?, ?, ?> model) {
        if (mySingleRepoProject) {
            return true;
        }
        return model.isSelected() && (hasCommitsToPush(model) || !model.getSupport().getRepositoryManager().isSyncEnabled());
    }

    private boolean preselectByUser(Repository repository) {
        return mySingleRepoProject || myPreselectedRepositories.contains(repository);
    }

    private static boolean hasCommitsToPush(PushRepoModel<?, ?, ?> model) {
        PushTarget target = Objects.requireNonNull(model.getTarget());
        return !model.getLoadedCommits().isEmpty() || target.hasSomethingToPush();
    }

    public PushLog getPushPanelLog() {
        return myPushLog;
    }

    public UnifiedPushDialog getDialog() {
        return myDialog;
    }

    public PrePushHandler.Result executeHandlers(ProgressIndicator indicator) {
        List<PrePushHandler> handlers = myProject.getApplication().getExtensionList(PrePushHandler.class);
        if (handlers.isEmpty()) {
            return PrePushHandler.Result.OK;
        }

        List<PushInfo> pushDetails = preparePushDetails();
        indicator.setIndeterminate(false);
        indicator.setFraction(0);
        for (int index = 0; index < handlers.size(); index++) {
            PrePushHandler handler = handlers.get(index);
            indicator.checkCanceled();
            indicator.setText(handler.getPresentableName());
            PrePushHandler.Result prePushHandlerResult;
            try {
                prePushHandlerResult = handler.handle(myProject, pushDetails, indicator);
            }
            catch (Throwable e) {
                List<LocalizeValue> skippedHandlers = new ArrayList<>();
                for (PrePushHandler skipped : handlers.subList(index + 1, handlers.size())) {
                    skippedHandlers.add(skipped.getPresentableName());
                }
                throw new PrePushHandlerException(handler.getPresentableName(), skippedHandlers, e);
            }

            if (prePushHandlerResult != PrePushHandler.Result.OK) {
                return prePushHandlerResult;
            }
            indicator.setIndeterminate(false);
            indicator.setFraction((double) (index + 1) / handlers.size());
        }
        return PrePushHandler.Result.OK;
    }

    public void push(boolean force, Map<PushSupport<?, ?, ?>, @Nullable VcsPushOptionValue> options, Map<String, VcsPushOptionValue> customParams) {
        for (PushSupport<Repository, PushSource, PushTarget> support : myPushSupports) {
            doPushSynchronously(support, force, options.get(support), customParams);
        }
    }

    private <R extends Repository, S extends PushSource, T extends PushTarget> void doPushSynchronously(PushSupport<R, S, T> support,
                                                                                                        boolean force,
                                                                                                        @Nullable VcsPushOptionValue options,
                                                                                                        Map<String, VcsPushOptionValue> customParams) {
        Pusher<R, S, T> pusher = support.getPusher();
        Map<R, PushSpec<S, T>> specs = collectPushSpecsForVcs(support);
        if (!specs.isEmpty()) {
            pusher.push(specs, options, force, customParams);
        }
    }

    private static <R extends Repository, S extends PushSource, T extends PushTarget> List<? extends VcsFullCommitDetails> loadCommits(PushRepoModel<R, S, T> model) {
        PushSupport<R, S, T> support = model.getSupport();
        T target = model.getTarget();
        if (target == null) {
            return List.of();
        }
        OutgoingCommitsProvider<R, S, T> outgoingCommitsProvider = support.getOutgoingCommitsProvider();
        return outgoingCommitsProvider.getOutgoingCommits(model.getRepository(), new PushSpec<>(model.getSource(), target), true).getCommits();
    }

    private List<PushInfo> preparePushDetails() {
        List<PushInfo> allDetails = new ArrayList<>();

        for (PushRepoModel<Repository, PushSource, PushTarget> model : getSelectedRepoNode()) {
            PushTarget target = Objects.requireNonNull(model.getTarget());
            PushSpec<PushSource, PushTarget> pushSpec = new PushSpec<>(model.getSource(), target);

            List<VcsFullCommitDetails> loadedCommits = new ArrayList<>(model.getLoadedCommits());
            if (loadedCommits.isEmpty()) {
                loadedCommits.addAll(loadCommits(model));
            }
            Collections.reverse(loadedCommits);

            allDetails.add(new PushInfoImpl(model.getRepository(), pushSpec, loadedCommits));
        }
        return Collections.unmodifiableList(allDetails);
    }

    public Map<PushSupport<Repository, PushSource, PushTarget>, Collection<PushInfo>> getSelectedPushSpecs() {
        Map<PushSupport<Repository, PushSource, PushTarget>, Collection<PushInfo>> result = new HashMap<>();

        for (PushRepoModel<Repository, PushSource, PushTarget> model : getSelectedRepoNode()) {
            PushTarget target = Objects.requireNonNull(model.getTarget());
            PushSpec<PushSource, PushTarget> pushSpec = new PushSpec<>(model.getSource(), target);

            PushInfoImpl pushInfo = new PushInfoImpl(model.getRepository(), pushSpec, List.of());
            result.computeIfAbsent(model.getSupport(), key -> new ArrayList<>()).add(pushInfo);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private <R extends Repository, S extends PushSource, T extends PushTarget> Map<R, PushSpec<S, T>> collectPushSpecsForVcs(PushSupport<R, S, T> pushSupport) {
        Collection<PushInfo> pushInfos = getSelectedPushSpecs().get(pushSupport);
        if (pushInfos == null) {
            return Map.of();
        }
        Map<R, PushSpec<S, T>> result = new LinkedHashMap<>();
        for (PushInfo pushInfo : pushInfos) {
            result.put((R) pushInfo.getRepository(), (PushSpec<S, T>) (PushSpec<?, ?>) pushInfo.getPushSpec());
        }
        return result;
    }

    private List<PushRepoModel<Repository, PushSource, PushTarget>> getSelectedRepoNode() {
        List<PushRepoModel<Repository, PushSource, PushTarget>> result = new ArrayList<>();
        for (PushRepoModel<Repository, PushSource, PushTarget> model : myModels) {
            if ((mySingleRepoProject || model.isSelected()) && isPushAllowed(model)) {
                result.add(model);
            }
        }
        return result;
    }

    @Override
    public void dispose() {
        myExecutorService.shutdownNow();
    }

    public Project getProject() {
        return myProject;
    }

    @RequiredUIAccess
    void addMoreCommits(PushRepoModel<?, ?, ?> model) {
        model.increaseShownCommits();
        myPushLog.setChildren(model);
    }

    @RequiredUIAccess
    void handleError(PushErrorRow row) {
        PushRepoModel<?, ?, ?> model = row.repository();
        row.error().handleError(new CommitLoader() {
            @Override
            public void reloadCommits() {
                UIAccess.current().give(() -> PushController.this.reloadCommits(model));
            }
        });
    }

    @RequiredUIAccess
    public Map<PushSupport<?, ?, ?>, VcsPushOptionsPanel> createAdditionalPanels() {
        Map<PushSupport<?, ?, ?>, VcsPushOptionsPanel> result = new LinkedHashMap<>();
        for (PushSupport<?, ?, ?> support : myPushSupports) {
            VcsPushOptionsPanel panel = support.createOptionsPanel();
            if (panel != null) {
                result.put(support, panel);
            }
        }
        return result;
    }

    @RequiredUIAccess
    public Map<String, VcsPushOptionsPanel> createCustomPanels(Collection<? extends Repository> repos) {
        Map<String, VcsPushOptionsPanel> result = new LinkedHashMap<>();
        List<Repository> repositories = List.copyOf(repos);
        myProject.getApplication().getExtensionPoint(CustomPushOptionsPanelFactory.class).forEach(factory -> {
            try {
                VcsPushOptionsPanel panel = factory.createOptionsPanel(this, repositories);
                if (panel != null) {
                    result.put(factory.getId(), panel);
                }
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        });
        return result;
    }
}
