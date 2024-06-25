package consulo.versionControlSystem.internal;

import consulo.application.ReadAction;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsIgnoreManager;
import consulo.versionControlSystem.change.ChangeListListener;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public final class ProjectConfigurationFilesProcessorImpl extends FilesProcessorWithNotificationImpl implements ChangeListListener {
  private Disposable parentDisposable;

  private AbstractVcs vcs;

  private Function<Collection<? extends VirtualFile>, Void> addChosenFiles;

  private AtomicBoolean foundProjectConfigurationFiles = new AtomicBoolean();

  private LocalFileSystem fileSystem = LocalFileSystem.getInstance();

  private VcsIgnoreManager vcsIgnoreManager;

  private String notificationDisplayId = VcsNotificationIdsHolder.INSTANCE.INSTANCE.PROJECT_CONFIGURATION_FILES_ADDED;

  private String askedBeforeProperty = ProjectConfigurationFilesProcessorImplKt.ASKED_SHARE_PROJECT_CONFIGURATION_FILES_PROPERTY;

  private String doForCurrentProjectProperty = ProjectConfigurationFilesProcessorImplKt.SHARE_PROJECT_CONFIGURATION_FILES_PROPERTY;

  private String showActionText = VcsBundle.message("project.configuration.files.add.notification.action.view");

  private String forCurrentProjectActionText = VcsBundle.message("project.configuration.files.add.notification.action.add");

  private String muteActionText = VcsBundle.message("project.configuration.files.add.notification.action.mute");

  private String viewFilesDialogTitle = VcsBundle.message("project.configuration.files.view.dialog.title", vcs.getDisplayName());

  public ProjectConfigurationFilesProcessorImpl(@Nonnull Project project,
                                                @Nonnull Disposable parentDisposable, @Nonnull AbstractVcs vcs,
                                                @Nonnull Function<Collection<? extends VirtualFile>, Void> addChosenFiles) {
    super(project, parentDisposable);
    vcsIgnoreManager = VcsIgnoreManager.getInstance(project);
    this.parentDisposable = parentDisposable;
    this.vcs = vcs;
    this.addChosenFiles = addChosenFiles;
  }

  public final void install() {
    ReadAction.run(() -> {
      if (!getProject().isDisposed()) {
        getProject().getMessageBus().connect(parentDisposable).subscribe(ChangeListListener.TOPIC, this);
      }
    });
  }

  @Nonnull
  public final List<VirtualFile> filterNotProjectConfigurationFiles(
    @Nonnull List<? extends VirtualFile> files) {
    Collection<VirtualFile> projectConfigurationFiles = doFilterFiles(files);
    if (isNotEmpty(projectConfigurationFiles)) {
      if (foundProjectConfigurationFiles.compareAndSet(false, true)) {
        ProjectConfigurationFilesProcessorImplKt.LOG.debug("Found new project configuration files ", projectConfigurationFiles);
      }
    }
    return files.minus(projectConfigurationFiles);
  }

  @Override
  public void unchangedFileStatusChanged(boolean upToDate) {
    if (!upToDate) {
      return;
    }
    if (foundProjectConfigurationFiles.compareAndSet(true, false)) {
      Collection<VirtualFile> unversionedProjectConfigurationFiles =
        doFilterFiles(ChangeListManagerImpl.getInstanceImpl(getProject()).getUnversionedFiles());
      if (isNotEmpty(unversionedProjectConfigurationFiles)) {
        setForCurrentProject(VcsImplUtil.isProjectSharedInVcs(getProject()));
        processFiles(new ArrayList(unversionedProjectConfigurationFiles));
      }
    }
  }

  @Override
  @Nonnull
  public Collection<VirtualFile> doFilterFiles(@Nonnull Collection<? extends VirtualFile> files) {
    VirtualFile projectConfigDir = getProjectConfigDir(getProject());
    return toSet(filterNot(filter(asSequence(files),
                                  it -> ProjectConfigurationFilesProcessorImplKt.configurationFilesExtensionsOutsideStoreDirectory.contains(
                                    it.getExtension()) || isProjectConfigurationFile(projectConfigDir, it)),
                           "unsupported 'vcsIgnoreManager::isPotentiallyIgnoredFile' expression"));
  }

  @Override
  public void doActionOnChosenFiles(@Nonnull Collection<? extends VirtualFile> files) {
    addChosenFiles.apply(files);
  }

  @Override
  @Nonnull
  public String getNotificationDisplayId() {
    return notificationDisplayId;
  }

  @Override
  @Nonnull
  public String getAskedBeforeProperty() {
    return askedBeforeProperty;
  }

  @Override
  @Nonnull
  public String getDoForCurrentProjectProperty() {
    return doForCurrentProjectProperty;
  }

  @Override
  @Nonnull
  public String notificationTitle() {
    return "";
  }

  @Override
  @Nonnull
  public String notificationMessage() {
    return VcsBundle.message("project.configuration.files.add.notification.message", vcs.getDisplayName());
  }

  @Override
  @Nonnull
  public String getShowActionText() {
    return showActionText;
  }

  @Override
  @Nonnull
  public String getForCurrentProjectActionText() {
    return forCurrentProjectActionText;
  }

  @Override
  @Nonnull
  public String getMuteActionText() {
    return muteActionText;
  }

  @Override
  @Nonnull
  protected String getViewFilesDialogTitle() {
    return viewFilesDialogTitle;
  }

  private boolean isProjectConfigurationFile(VirtualFile configDir, VirtualFile file) {
    return configDir != null && VirtualFileUtil.isAncestor((VirtualFile)configDir, file, true);
  }

  private VirtualFile getProjectConfigDir(@Nonnull Project $this$getProjectConfigDir) {
    if (!ProjectKt.getIsDirectoryBased() || isDefault()) {
      return null;
    }
    VirtualFile projectConfigDir =
      ProjectKt.getStateStore().getDirectoryStorePath().let("unsupported 'fileSystem::findFileByNioFile' expression");
    if (projectConfigDir == null) {
      ProjectConfigurationFilesProcessorImplKt.LOG.warn(
        "Cannot find project config directory for non-default and non-directory based project " + getName());
    }
    return projectConfigDir;
  }
}
