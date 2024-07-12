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
package consulo.ide.impl.idea.dvcs.push;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.dvcs.push.ui.*;
import consulo.ide.impl.idea.util.ConcurrencyUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.CheckedTreeNode;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.function.Condition;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.distributed.DvcsBundle;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.distributed.push.*;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.repository.VcsRepositoryManager;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static consulo.ui.ex.awt.Messages.OK;

public class PushController implements Disposable {

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final List<? extends Repository> myPreselectedRepositories;
  @Nonnull
  private final VcsRepositoryManager myGlobalRepositoryManager;
  @Nonnull
  private final List<PushSupport<Repository, PushSource, PushTarget>> myPushSupports;
  @Nonnull
  private final PushLog myPushLog;
  @Nonnull
  private final VcsPushDialog myDialog;
  @Nonnull
  private final PushSettings myPushSettings;
  @Nonnull
  private final Set<String> myExcludedRepositoryRoots;
  @Nullable
  private final Repository myCurrentlyOpenedRepository;
  private final boolean mySingleRepoProject;
  private static final int DEFAULT_CHILDREN_PRESENTATION_NUMBER = 20;
  private final ExecutorService myExecutorService = Executors.newSingleThreadExecutor(ConcurrencyUtil.newNamedThreadFactory("DVCS Push"));

  private final Map<RepositoryNode, MyRepoModel<?, ?, ?>> myView2Model = new TreeMap<RepositoryNode, MyRepoModel<?, ?, ?>>();

  public PushController(@Nonnull Project project, @Nonnull VcsPushDialog dialog, @Nonnull List<? extends Repository> preselectedRepositories, @Nullable Repository currentRepo) {
    myProject = project;
    myPushSettings = ServiceManager.getService(project, PushSettings.class);
    myGlobalRepositoryManager = VcsRepositoryManager.getInstance(project);
    myExcludedRepositoryRoots = ContainerUtil.newHashSet(myPushSettings.getExcludedRepoRoots());
    myPreselectedRepositories = preselectedRepositories;
    myCurrentlyOpenedRepository = currentRepo;
    myPushSupports = getAffectedSupports();
    mySingleRepoProject = isSingleRepoProject();
    myDialog = dialog;
    CheckedTreeNode rootNode = new CheckedTreeNode(null);
    createTreeModel(rootNode);
    myPushLog = new PushLog(myProject, rootNode, isSyncStrategiesAllowed());
    myPushLog.getTree().addPropertyChangeListener(PushLogTreeUtil.EDIT_MODE_PROP, new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        // when user starts edit we need to force disable ok actions, because tree.isEditing() still false;
        // after editing completed okActions will be enabled automatically by dialog validation
        Boolean isEditMode = (Boolean)evt.getNewValue();
        if (isEditMode) {
          myDialog.disableOkActions();
        }
      }
    });
    startLoadingCommits();
    Disposer.register(dialog.getDisposable(), this);
  }

  private boolean isSyncStrategiesAllowed() {
    return !mySingleRepoProject && ContainerUtil.and(getAffectedSupports(), new Condition<PushSupport<Repository, PushSource, PushTarget>>() {
      @Override
      public boolean value(PushSupport<Repository, PushSource, PushTarget> support) {
        return support.mayChangeTargetsSync();
      }
    });
  }

  private boolean isSingleRepoProject() {
    return myGlobalRepositoryManager.getRepositories().size() == 1;
  }

  @Nonnull
  private <R extends Repository, S extends PushSource, T extends PushTarget> List<PushSupport<R, S, T>> getAffectedSupports() {
    Collection<Repository> repositories = myGlobalRepositoryManager.getRepositories();
    Collection<AbstractVcs> vcss = ContainerUtil.map2Set(repositories, repository -> repository.getVcs());
    return ContainerUtil.map(vcss, (Function<AbstractVcs, PushSupport<R, S, T>>)vcs -> {
      //noinspection unchecked
      return DvcsUtil.getPushSupport(vcs);
    });
  }

  public boolean isForcePushEnabled() {
    return ContainerUtil.exists(myView2Model.values(), model -> model.getSupport().isForcePushEnabled());
  }

  @Nullable
  public PushTarget getProhibitedTarget() {
    MyRepoModel model = ContainerUtil.find(myView2Model.values(), new Condition<MyRepoModel>() {
      @Override
      public boolean value(MyRepoModel model) {
        PushTarget target = model.getTarget();
        return model.isSelected() && target != null && !model.getSupport().isForcePushAllowed(model.getRepository(), target);
      }
    });
    return model != null ? model.getTarget() : null;
  }

  private void startLoadingCommits() {
    Map<RepositoryNode, MyRepoModel> priorityLoading = new LinkedHashMap<>();
    Map<RepositoryNode, MyRepoModel> others = new LinkedHashMap<>();
    RepositoryNode nodeForCurrentEditor = findNodeByRepo(myCurrentlyOpenedRepository);
    for (Map.Entry<RepositoryNode, MyRepoModel<?, ?, ?>> entry : myView2Model.entrySet()) {
      MyRepoModel model = entry.getValue();
      Repository repository = model.getRepository();
      RepositoryNode repoNode = entry.getKey();
      if (preselectByUser(repository)) {
        priorityLoading.put(repoNode, model);
      }
      else if (model.getSupport().shouldRequestIncomingChangesForNotCheckedRepositories() && !repoNode.equals(nodeForCurrentEditor)) {
        others.put(repoNode, model);
      }
      if (shouldPreSelect(model)) {
        model.setChecked(true);
      }
    }
    if (nodeForCurrentEditor != null) {
      //add repo for currently opened editor to the end of priority queue
      priorityLoading.put(nodeForCurrentEditor, myView2Model.get(nodeForCurrentEditor));
    }
    loadCommitsFromMap(priorityLoading);
    loadCommitsFromMap(others);
  }

  private boolean shouldPreSelect(@Nonnull MyRepoModel model) {
    Repository repository = model.getRepository();
    return mySingleRepoProject || preselectByUser(repository) || (notExcludedByUser(repository) && model.getSupport().shouldRequestIncomingChangesForNotCheckedRepositories());
  }

  private RepositoryNode findNodeByRepo(@Nullable final Repository repository) {
    if (repository == null) return null;
    Map.Entry<RepositoryNode, MyRepoModel<?, ?, ?>> entry = ContainerUtil.find(myView2Model.entrySet(), entry1 -> {
      MyRepoModel model = entry1.getValue();
      return model.getRepository().getRoot().equals(repository.getRoot());
    });
    return entry != null ? entry.getKey() : null;
  }

  private void loadCommitsFromMap(@Nonnull Map<RepositoryNode, MyRepoModel> items) {
    for (Map.Entry<RepositoryNode, MyRepoModel> entry : items.entrySet()) {
      RepositoryNode node = entry.getKey();
      loadCommits(entry.getValue(), node, true);
    }
  }

  private void createTreeModel(@Nonnull CheckedTreeNode rootNode) {
    for (Repository repository : DvcsUtil.sortRepositories(myGlobalRepositoryManager.getRepositories())) {
      createRepoNode(repository, rootNode);
    }
  }

  @Nullable
  private <R extends Repository, S extends PushSource, T extends PushTarget> PushSupport<R, S, T>
  getPushSupportByRepository(@Nonnull final R repository) {
    //noinspection unchecked
    return (PushSupport<R, S, T>)ContainerUtil.find(myPushSupports, support -> support.getVcs().equals(repository.getVcs()));
  }

  private <R extends Repository, S extends PushSource, T extends PushTarget> void createRepoNode(@Nonnull final R repository, @Nonnull final CheckedTreeNode rootNode) {

    PushSupport<R, S, T> support = getPushSupportByRepository(repository);
    if (support == null) return;

    T target = support.getDefaultTarget(repository);
    String repoName = getDisplayedRepoName(repository);
    S source = support.getSource(repository);
    final MyRepoModel<R, S, T> model = new MyRepoModel<>(repository, support, mySingleRepoProject, source, target);
    if (target == null) {
      model.setError(VcsError.createEmptyTargetError(repoName));
    }

    final PushTargetPanel<T> pushTargetPanel = support.createTargetPanel(repository, target);
    final RepositoryWithBranchPanel<T> repoPanel =
      new RepositoryWithBranchPanel<>(myProject, repoName, source.getPresentation(), pushTargetPanel);
    CheckBoxModel checkBoxModel = model.getCheckBoxModel();
    final RepositoryNode repoNode = mySingleRepoProject
      ? new SingleRepositoryNode(repoPanel, checkBoxModel)
      : new RepositoryNode(repoPanel, checkBoxModel, target != null);
    pushTargetPanel.setFireOnChangeAction(() -> {
      repoPanel.fireOnChange();
      ((DefaultTreeModel)myPushLog.getTree().getModel()).nodeChanged(repoNode); // tell the tree to repaint the changed node
    });
    myView2Model.put(repoNode, model);
    repoPanel.addRepoNodeListener(new RepositoryNodeListener<>() {
      @Override
      public void onTargetChanged(T newTarget) {
        repoNode.setChecked(true);
        myExcludedRepositoryRoots.remove(model.getRepository().getRoot().getPath());
        if (!newTarget.equals(model.getTarget()) || model.hasError() || !model.hasCommitInfo()) {
          model.setTarget(newTarget);
          model.clearErrors();
          loadCommits(model, repoNode, false);
        }
      }

      @Override
      public void onSelectionChanged(boolean isSelected) {
        myDialog.updateOkActions();
        if (isSelected) {
          boolean forceLoad = myExcludedRepositoryRoots.remove(model.getRepository().getRoot().getPath());
          if (!model.hasCommitInfo() && (forceLoad || !model.getSupport().shouldRequestIncomingChangesForNotCheckedRepositories())) {
            loadCommits(model, repoNode, false);
          }
        }
        else {
          myExcludedRepositoryRoots.add(model.getRepository().getRoot().getPath());
        }
      }

      @Override
      public void onTargetInEditMode(@Nonnull String currentValue) {
        myPushLog.fireEditorUpdated(currentValue);
      }
    });
    rootNode.add(repoNode);
  }

  // TODO This logic shall be moved to some common place and used instead of DvcsUtil.getShortRepositoryName
  @Nonnull
  private String getDisplayedRepoName(@Nonnull Repository repository) {
    String name = DvcsUtil.getShortRepositoryName(repository);
    int slash = name.lastIndexOf(File.separatorChar);
    if (slash < 0) {
      return name;
    }
    String candidate = name.substring(slash + 1);
    return !containedInOtherNames(repository, candidate) ? candidate : name;
  }

  private boolean containedInOtherNames(@Nonnull final Repository except, final String candidate) {
    return ContainerUtil.exists(
      myGlobalRepositoryManager.getRepositories(),
      repository -> !repository.equals(except) && repository.getRoot().getName().equals(candidate)
    );
  }

  public boolean isPushAllowed(final boolean force) {
    JTree tree = myPushLog.getTree();
    return !tree.isEditing() && ContainerUtil.exists(myPushSupports, support -> isPushAllowed(support, force));
  }

  private boolean isPushAllowed(@Nonnull PushSupport<?, ?, ?> pushSupport, boolean force) {
    Collection<RepositoryNode> nodes = getNodesForSupport(pushSupport);
    if (hasSomethingToPush(nodes)) return true;
    if (hasCheckedNodesWithContent(nodes, force || myDialog.getAdditionalOptionValue(pushSupport) != null)) {
      return !pushSupport.getRepositoryManager().isSyncEnabled() || !hasLoadingNodes(nodes);
    }
    return false;
  }

  private boolean hasSomethingToPush(Collection<RepositoryNode> nodes) {
    return ContainerUtil.exists(nodes, node -> {
      PushTarget target = myView2Model.get(node).getTarget();
      //if node is selected target should not be null
      return node.isChecked() && target != null && target.hasSomethingToPush();
    });
  }

  private boolean hasCheckedNodesWithContent(@Nonnull Collection<RepositoryNode> nodes, final boolean withRefs) {
    return ContainerUtil.exists(nodes, node -> node.isChecked() && (withRefs || !myView2Model.get(node).getLoadedCommits().isEmpty()));
  }

  @Nonnull
  private Collection<RepositoryNode> getNodesForSupport(final PushSupport<?, ?, ?> support) {
    return ContainerUtil.mapNotNull(myView2Model.entrySet(), entry -> support.equals(entry.getValue().getSupport()) ? entry.getKey() : null);
  }

  private static boolean hasLoadingNodes(@Nonnull Collection<RepositoryNode> nodes) {
    return ContainerUtil.exists(nodes, RepositoryNode::isLoading);
  }

  private <R extends Repository, S extends PushSource, T extends PushTarget> void loadCommits(
    @Nonnull final MyRepoModel<R, S, T> model,
    @Nonnull final RepositoryNode node,
    final boolean initial
  ) {
    node.cancelLoading();
    final T target = model.getTarget();
    if (target == null) {
      node.stopLoading();
      return;
    }
    node.setEnabled(true);
    final PushSupport<R, S, T> support = model.getSupport();
    final AtomicReference<OutgoingResult> result = new AtomicReference<>();
    Runnable task = () -> {
      final R repository = model.getRepository();
      OutgoingResult outgoing = support.getOutgoingCommitsProvider()
        .getOutgoingCommits(repository, new PushSpec<>(model.getSource(), model.getTarget()), initial);
      result.compareAndSet(null, outgoing);
      UIUtil.invokeAndWaitIfNeeded((Runnable)() -> {
        OutgoingResult outgoing1 = result.get();
        List<VcsError> errors = outgoing1.getErrors();
        boolean shouldBeSelected;
        if (!errors.isEmpty()) {
          shouldBeSelected = false;
          model.setLoadedCommits(ContainerUtil.<VcsFullCommitDetails>emptyList());
          myPushLog.setChildren(node, ContainerUtil.map(errors, (Function<VcsError, DefaultMutableTreeNode>)error -> {
            VcsLinkedTextComponent errorLinkText = new VcsLinkedTextComponent(
              error.getText(),
              (sourceNode, event) -> error.handleError(
                () -> {
                  node.setChecked(true);
                  loadCommits(model, node, false);
                }
              )
            );
            return new TextWithLinkNode(errorLinkText);
          }));
          if (node.isChecked()) {
            node.setChecked(false);
          }
        }
        else {
          List<? extends VcsFullCommitDetails> commits = outgoing1.getCommits();
          model.setLoadedCommits(commits);
          shouldBeSelected = shouldSelectNodeAfterLoad(model);
          myPushLog.setChildren(node, getPresentationForCommits(PushController.this.myProject, model.getLoadedCommits(), model.getNumberOfShownCommits()));
          if (!commits.isEmpty()) {
            myPushLog.selectIfNothingSelected(node);
          }
        }
        node.stopLoading();
        updateLoadingPanel();
        if (shouldBeSelected) {
          node.setChecked(true);
        }
        else if (initial) {
          //do not un-check if user checked manually and no errors occurred, only initial check may be changed
          node.setChecked(false);
        }
        myDialog.updateOkActions();
      });
    };
    node.startLoading(myPushLog.getTree(), myExecutorService.submit(task, result), initial);
    updateLoadingPanel();
  }

  private void updateLoadingPanel() {
    myPushLog.getTree().setPaintBusy(hasLoadingNodes(myView2Model.keySet()));
  }

  private boolean shouldSelectNodeAfterLoad(@Nonnull MyRepoModel model) {
    return mySingleRepoProject || hasCommitsToPush(model) && model.isSelected();
  }

  private boolean notExcludedByUser(@Nonnull Repository repository) {
    return !myExcludedRepositoryRoots.contains(repository.getRoot().getPath());
  }

  private boolean preselectByUser(@Nonnull Repository repository) {
    return myPreselectedRepositories.contains(repository);
  }

  private static boolean hasCommitsToPush(@Nonnull MyRepoModel model) {
    PushTarget target = model.getTarget();
    assert target != null;
    return (!model.getLoadedCommits().isEmpty() || target.hasSomethingToPush());
  }

  public PushLog getPushPanelLog() {
    return myPushLog;
  }

  public void push(final boolean force) {
    Task.Backgroundable task = new Task.Backgroundable(myProject, "Pushing...", true) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        myPushSettings.saveExcludedRepoRoots(myExcludedRepositoryRoots);
        for (PushSupport support : myPushSupports) {
          doPush(support, force);
        }
      }
    };
    task.queue();
  }

  private <R extends Repository, S extends PushSource, T extends PushTarget> void doPush(@Nonnull PushSupport<R, S, T> support, boolean force) {
    VcsPushOptionValue options = myDialog.getAdditionalOptionValue(support);
    Pusher<R, S, T> pusher = support.getPusher();
    Map<R, PushSpec<S, T>> specs = collectPushSpecsForVcs(support);
    if (!specs.isEmpty()) {
      pusher.push(specs, options, force);
    }
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  private <R extends Repository, S extends PushSource, T extends PushTarget> Map<R, PushSpec<S, T>>
  collectPushSpecsForVcs(@Nonnull PushSupport<R, S, T> pushSupport) {
    Map<R, PushSpec<S, T>> pushSpecs = new HashMap<>();
    Collection<MyRepoModel<?, ?, ?>> repositoriesInformation = getSelectedRepoNode();
    for (MyRepoModel<?, ?, ?> repoModel : repositoriesInformation) {
      if (pushSupport.equals(repoModel.getSupport())) {
        //todo improve generics: unchecked casts
        T target = (T)repoModel.getTarget();
        if (target != null) {
          pushSpecs.put((R)repoModel.getRepository(), new PushSpec<>((S)repoModel.getSource(), target));
        }
      }
    }
    return pushSpecs;
  }

  private Collection<MyRepoModel<?, ?, ?>> getSelectedRepoNode() {
    if (mySingleRepoProject) {
      return myView2Model.values();
    }
    //return all selected despite a loading state;
    return ContainerUtil.mapNotNull(
      myView2Model.entrySet(),
      (Function<Map.Entry<RepositoryNode, MyRepoModel<?, ?, ?>>, MyRepoModel<?, ?, ?>>)entry -> {
        MyRepoModel<?, ?, ?> model = entry.getValue();
        return model.isSelected() && model.getTarget() != null ? model : null;
      }
    );
  }

  @Override
  public void dispose() {
    myExecutorService.shutdownNow();
  }

  @Nonnull
  public Project getProject() {
    return myProject;
  }

  private void addMoreCommits(RepositoryNode repositoryNode) {
    MyRepoModel<?, ?, ?> repoModel = myView2Model.get(repositoryNode);
    repoModel.increaseShownCommits();
    myPushLog.setChildren(repositoryNode, getPresentationForCommits(myProject, repoModel.getLoadedCommits(), repoModel.getNumberOfShownCommits()));
  }


  @Nonnull
  private List<DefaultMutableTreeNode> getPresentationForCommits(
    @Nonnull final Project project,
    @Nonnull List<? extends VcsFullCommitDetails> commits,
    int commitsNum
  ) {
    Function<VcsFullCommitDetails, DefaultMutableTreeNode> commitToNode = commit -> new CommitNode(project, commit);
    List<DefaultMutableTreeNode> childrenToShown = new ArrayList<>();
    for (int i = 0; i < commits.size(); ++i) {
      if (i >= commitsNum) {
        final VcsLinkedTextComponent moreCommitsLink = new VcsLinkedTextComponent(
          "<a href='loadMore'>...</a>",
          (sourceNode, event) -> {
            TreeNode parent = sourceNode.getParent();
            if (parent instanceof RepositoryNode repositoryNode) {
              addMoreCommits(repositoryNode);
            }
          }
        );
        childrenToShown.add(new TextWithLinkNode(moreCommitsLink));
        break;
      }
      childrenToShown.add(commitToNode.apply(commits.get(i)));
    }
    return childrenToShown;
  }

  @Nonnull
  public Map<PushSupport, VcsPushOptionsPanel> createAdditionalPanels() {
    Map<PushSupport, VcsPushOptionsPanel> result = new LinkedHashMap<>();
    for (PushSupport support : myPushSupports) {
      ContainerUtil.putIfNotNull(support, support.createOptionsPanel(), result);
    }
    return result;
  }

  @RequiredUIAccess
  public boolean ensureForcePushIsNeeded() {
    Collection<MyRepoModel<?, ?, ?>> selectedNodes = getSelectedRepoNode();
    MyRepoModel<?, ?, ?> selectedModel = ContainerUtil.getFirstItem(selectedNodes);
    if (selectedModel == null) return false;
    final PushSupport activePushSupport = selectedModel.getSupport();
    final PushTarget commonTarget = getCommonTarget(selectedNodes);
    if (commonTarget != null && activePushSupport.isSilentForcePushAllowed(commonTarget)) {
      return true;
    }

    return Messages.showOkCancelDialog(
      myProject,
      XmlStringUtil.wrapInHtml(
        DvcsBundle.message(
          "push.force.confirmation.text",
          commonTarget != null ? " to <b>" + commonTarget.getPresentation() + "</b>" : ""
        )
      ),
      "Force Push",
      "&Force Push",
      CommonLocalize.buttonCancel().get(),
      UIUtil.getWarningIcon(),
      commonTarget != null ? new MyDoNotAskOptionForPush(activePushSupport, commonTarget) : null
    ) == OK;
  }

  @Nullable
  private static PushTarget getCommonTarget(@Nonnull Collection<MyRepoModel<?, ?, ?>> selectedNodes) {
    final PushTarget commonTarget = ObjectUtil.assertNotNull(ContainerUtil.getFirstItem(selectedNodes)).getTarget();
    return commonTarget != null
      && !ContainerUtil.exists(selectedNodes, model -> !commonTarget.equals(model.getTarget())) ? commonTarget : null;
  }

  private static class MyRepoModel<Repo extends Repository, S extends PushSource, T extends PushTarget> {
    @Nonnull
    private final Repo myRepository;
    @Nonnull
    private final PushSupport<Repo, S, T> mySupport;
    @Nonnull
    private final S mySource;
    @Nullable
    private T myTarget;
    @Nullable
    VcsError myTargetError;

    int myNumberOfShownCommits;
    @Nonnull
    List<? extends VcsFullCommitDetails> myLoadedCommits = Collections.emptyList();
    @Nonnull
    private final CheckBoxModel myCheckBoxModel;

    public MyRepoModel(@Nonnull Repo repository, @Nonnull PushSupport<Repo, S, T> supportForRepo, boolean isSelected, @Nonnull S source, @Nullable T target) {
      myRepository = repository;
      mySupport = supportForRepo;
      myCheckBoxModel = new CheckBoxModel(isSelected);
      mySource = source;
      myTarget = target;
      myNumberOfShownCommits = DEFAULT_CHILDREN_PRESENTATION_NUMBER;
    }

    @Nonnull
    public Repo getRepository() {
      return myRepository;
    }

    @Nonnull
    public PushSupport<Repo, S, T> getSupport() {
      return mySupport;
    }

    @Nonnull
    public S getSource() {
      return mySource;
    }

    @Nullable
    public T getTarget() {
      return myTarget;
    }

    public void setTarget(@Nullable T target) {
      myTarget = target;
    }

    public boolean isSelected() {
      return myCheckBoxModel.isChecked();
    }

    public void setError(@Nullable VcsError error) {
      myTargetError = error;
    }

    public void clearErrors() {
      myTargetError = null;
    }

    public boolean hasError() {
      return myTargetError != null;
    }

    public int getNumberOfShownCommits() {
      return myNumberOfShownCommits;
    }

    public void increaseShownCommits() {
      myNumberOfShownCommits *= 2;
    }

    @Nonnull
    public List<? extends VcsFullCommitDetails> getLoadedCommits() {
      return myLoadedCommits;
    }

    public void setLoadedCommits(@Nonnull List<? extends VcsFullCommitDetails> loadedCommits) {
      myLoadedCommits = loadedCommits;
    }

    public boolean hasCommitInfo() {
      return myTargetError != null || !myLoadedCommits.isEmpty();
    }

    @Nonnull
    public CheckBoxModel getCheckBoxModel() {
      return myCheckBoxModel;
    }

    public void setChecked(boolean checked) {
      myCheckBoxModel.setChecked(checked);
    }
  }

  private static class MyDoNotAskOptionForPush implements DialogWrapper.DoNotAskOption {

    @Nonnull
    private final PushSupport myActivePushSupport;
    @Nonnull
    private final PushTarget myCommonTarget;

    public MyDoNotAskOptionForPush(@Nonnull PushSupport support, @Nonnull PushTarget target) {
      myActivePushSupport = support;
      myCommonTarget = target;
    }

    @Override
    public boolean isToBeShown() {
      return true;
    }

    @Override
    public void setToBeShown(boolean toBeShown, int exitCode) {
      if (!toBeShown && exitCode == OK) {
        myActivePushSupport.saveSilentForcePushTarget(myCommonTarget);
      }
    }

    @Override
    public boolean canBeHidden() {
      return true;
    }

    @Override
    public boolean shouldSaveOptionsOnCancel() {
      return false;
    }

    @Nonnull
    @Override
    public String getDoNotShowMessage() {
      return "Don't warn about this target";
    }
  }
}
