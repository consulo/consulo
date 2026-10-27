// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change.ignore;

import consulo.annotation.component.ServiceImpl;
import consulo.application.WriteAction;
import consulo.logging.Logger;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsApplicationSettings;
import consulo.versionControlSystem.VcsNotifier;
import consulo.versionControlSystem.change.IgnoredFileContentProvider;
import consulo.versionControlSystem.change.IgnoredFileGenerator;
import consulo.versionControlSystem.change.IgnoredFileProvider;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.util.VcsImplUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static consulo.versionControlSystem.impl.internal.change.ignore.IgnoreConfigurationProperty.ASKED_MANAGE_IGNORE_FILES_PROPERTY;
import static consulo.versionControlSystem.impl.internal.change.ignore.IgnoreConfigurationProperty.MANAGE_IGNORE_FILES_PROPERTY;
import static java.lang.System.lineSeparator;

@Singleton
@ServiceImpl
public class IgnoredFileGeneratorImpl implements IgnoredFileGenerator {
    private static final Logger LOG = Logger.getInstance(IgnoredFileGeneratorImpl.class);

    private final Project myProject;

    private final Object myWriteLock = new Object();

    private static final Object myNotificationLock = new Object();

    private static @Nullable Notification myNotification;

    private static @Nullable VirtualFile myIgnoreFileRootNotificationShowFor;

    @Inject
    public IgnoredFileGeneratorImpl(Project project) {
        myProject = project;
    }

    @Override
    public void generateFile(VirtualFile ignoreFileRoot, AbstractVcs vcs, boolean notify) {
        doGenerate(ignoreFileRoot, vcs, notify);
    }

    @Override
    public boolean needGenerateInternalIgnoreFile(VirtualFile ignoreFileRoot) {
        return needGenerateInternalIgnoreFile(myProject, ignoreFileRoot);
    }

    @Override
    public void markIgnoreFileRootAsGenerated(String ignoreFileRoot) {
        markIgnoreFileRootAsGenerated(myProject, ignoreFileRoot);
    }

    private void doGenerate(VirtualFile ignoreFileRoot, AbstractVcs vcs, boolean notify) {
        if (skipGeneration(ignoreFileRoot, notify)) {
            return;
        }

        IgnoredFileContentProvider ignoredFileContentProvider = VcsImplUtil.findIgnoredFileContentProvider(vcs);
        if (ignoredFileContentProvider == null) {
            LOG.debug("Cannot find content provider for vcs " + vcs.getName());
            return;
        }

        String ignoreFileName = ignoredFileContentProvider.getFileName();

        synchronized (myWriteLock) {
            List<IgnoredFileProvider> providers = myProject.getExtensionPoint(IgnoredFileProvider.class).getExtensionList();
            String ignoreFileContent =
                ignoredFileContentProvider.buildIgnoreFileContent(ignoreFileRoot, providers.toArray(new IgnoredFileProvider[0]));
            if (StringUtil.isEmptyOrSpaces(ignoreFileContent)) {
                return;
            }

            File ignoreFile = getIgnoreFile(ignoreFileRoot, ignoreFileName);
            if (notify && needAskToManageIgnoreFiles(myProject)) {
                notifyVcsIgnoreFileManage(
                    myProject,
                    ignoreFileRoot,
                    ignoredFileContentProvider,
                    () -> writeToFile(ignoreFileRoot, ignoreFile, ignoreFileContent, true)
                );
            }
            else {
                writeToFile(ignoreFileRoot, ignoreFile, ignoreFileContent, false);
            }
        }
    }

    private boolean skipGeneration(VirtualFile ignoreFileRoot, boolean notify) {
        if (notify && !needGenerateIgnoreFile(myProject, ignoreFileRoot)) { // notify == true for non-internal ignore file generation
            LOG.debug("Skip VCS ignore file generation");
            return true;
        }
        else if (!needGenerateInternalIgnoreFile(myProject, ignoreFileRoot)) {
            LOG.debug("Skip VCS internal ignore file generation");
            return true;
        }
        return false;
    }

    private void writeToFile(VirtualFile ignoreFileRoot, File ignoreFile, String ignoreFileContent, boolean openFile) {
        boolean append = ignoreFile.exists();
        String projectCharsetName = EncodingProjectManager.getInstance(myProject).getDefaultCharsetName();
        try {
            if (append) {
                FileUtil.writeToFile(ignoreFile, (lineSeparator() + ignoreFileContent).getBytes(projectCharsetName), true);
            }
            else {
                //create ignore file with VFS to prevent externally added files detection
                IOException[] error = new IOException[1];
                WriteAction.runAndWait(() -> {
                    try {
                        VirtualFile newIgnoreFile = ignoreFileRoot.createChildData(ignoreFileRoot, ignoreFile.getName());
                        VirtualFileUtil.saveText(newIgnoreFile, ignoreFileContent);
                    }
                    catch (IOException e) {
                        error[0] = e;
                    }
                });
                if (error[0] != null) {
                    throw error[0];
                }
            }
        }
        catch (IOException e) {
            LOG.warn("Cannot write to file " + ignoreFile.getPath());
        }

        markIgnoreFileRootAsGenerated(myProject, ignoreFile.getParent());
        LocalFileSystem.getInstance().refreshIoFiles(List.of(ignoreFile), true, false, null);

        if (openFile) {
            openFile(ignoreFile);
        }
    }

    private void openFile(File file) {
        myProject.getApplication().invokeLater(() -> {
            VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
            if (vFile == null) {
                return;
            }
            OpenFileDescriptorFactory.getInstance(myProject).newBuilder(vFile).build().navigate(true);
        });
    }

    private static void notifyVcsIgnoreFileManage(
        Project project,
        VirtualFile ignoreFileRoot,
        IgnoredFileContentProvider ignoredFileContentProvider,
        Runnable writeToIgnoreFile
    ) {
        ProjectPropertiesComponent propertiesComponent = ProjectPropertiesComponent.getInstance(project);
        VcsApplicationSettings applicationSettings = VcsApplicationSettings.getInstance();

        synchronized (myNotificationLock) {
            if (myNotification != null
                && myIgnoreFileRootNotificationShowFor != null
                && !myNotification.isExpired()
                && myIgnoreFileRootNotificationShowFor.equals(ignoreFileRoot)) {
                return;
            }

            myIgnoreFileRootNotificationShowFor = ignoreFileRoot;
            myNotification = NotificationService.getInstance()
                .newInfo(VcsNotifier.STANDARD_NOTIFICATION)
                .content(VcsLocalize.ignoredFileManageMessage(project.getApplication().getName(), ignoredFileContentProvider.getFileName()))
                .addClosingAction(VcsLocalize.ignoredFileManageThisProject(), () -> {
                    writeToIgnoreFile.run();
                    propertiesComponent.setValue(MANAGE_IGNORE_FILES_PROPERTY, true);
                    propertiesComponent.setValue(ASKED_MANAGE_IGNORE_FILES_PROPERTY, true);
                    synchronized (myNotificationLock) {
                        myIgnoreFileRootNotificationShowFor = null;
                    }
                })
                .addClosingAction(VcsLocalize.ignoredFileManageAllProject(), () -> {
                    writeToIgnoreFile.run();
                    applicationSettings.getState().MANAGE_IGNORE_FILES = true;
                    propertiesComponent.setValue(ASKED_MANAGE_IGNORE_FILES_PROPERTY, true);
                    synchronized (myNotificationLock) {
                        myIgnoreFileRootNotificationShowFor = null;
                    }
                })
                .addClosingAction(VcsLocalize.ignoredFileManageNotmanage(), () -> {
                    propertiesComponent.setValue(ASKED_MANAGE_IGNORE_FILES_PROPERTY, true);
                    synchronized (myNotificationLock) {
                        myIgnoreFileRootNotificationShowFor = null;
                    }
                })
                .notifyAndGet(project);
        }
    }

    private static File getIgnoreFile(VirtualFile ignoreFileRoot, String ignoreFileName) {
        File vcsRootFile = VirtualFileUtil.virtualToIoFile(ignoreFileRoot);
        return new File(vcsRootFile.getPath(), ignoreFileName);
    }

    public static boolean needGenerateInternalIgnoreFile(Project project, VirtualFile ignoreFileRoot) {
        boolean wasGeneratedPreviously = IgnoredFileRootStore.getInstance(project).containsRoot(ignoreFileRoot.getPath());
        if (wasGeneratedPreviously) {
            LOG.debug("Ignore file generated previously for root " + ignoreFileRoot.getPath());
            return false;
        }
        return true;
    }

    public static void markIgnoreFileRootAsGenerated(Project project, String ignoreFileRoot) {
        IgnoredFileRootStore.getInstance(project).addRoot(ignoreFileRoot);
    }

    private static boolean needGenerateIgnoreFile(Project project, VirtualFile ignoreFileRoot) {
        VcsApplicationSettings vcsApplicationSettings = VcsApplicationSettings.getInstance();
        if (vcsApplicationSettings.getState().DISABLE_MANAGE_IGNORE_FILES) {
            return false;
        }

        boolean wasGeneratedPreviously = IgnoredFileRootStore.getInstance(project).containsRoot(ignoreFileRoot.getPath());
        if (wasGeneratedPreviously) {
            LOG.debug("Ignore file generated previously for root " + ignoreFileRoot.getPath());
            return false;
        }

        if (!vcsApplicationSettings.getState().GENERATE_IGNORE_FILES_IN_VCS_ROOTS) {
            return false;
        }

        ProjectPropertiesComponent propertiesComponent = ProjectPropertiesComponent.getInstance(project);
        boolean askedToManageIgnores = propertiesComponent.getBoolean(ASKED_MANAGE_IGNORE_FILES_PROPERTY, false);
        return isManageIgnoreTurnOn(project) || !askedToManageIgnores;
    }

    private static boolean isManageIgnoreTurnOn(Project project) {
        boolean globalManageIgnores = VcsApplicationSettings.getInstance().getState().MANAGE_IGNORE_FILES;
        ProjectPropertiesComponent propertiesComponent = ProjectPropertiesComponent.getInstance(project);
        boolean manageIgnoresInProject = propertiesComponent.getBoolean(MANAGE_IGNORE_FILES_PROPERTY, false);
        return globalManageIgnores || manageIgnoresInProject;
    }

    private static boolean needAskToManageIgnoreFiles(Project project) {
        ProjectPropertiesComponent propertiesComponent = ProjectPropertiesComponent.getInstance(project);
        boolean askedToManageIgnores = propertiesComponent.getBoolean(ASKED_MANAGE_IGNORE_FILES_PROPERTY, false);
        return !askedToManageIgnores && !isManageIgnoreTurnOn(project);
    }
}
