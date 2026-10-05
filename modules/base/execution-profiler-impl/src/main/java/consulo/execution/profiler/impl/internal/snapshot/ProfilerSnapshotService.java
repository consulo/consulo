/*
 * Copyright 2013-2026 consulo.io
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
package consulo.execution.profiler.impl.internal.snapshot;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.disposer.Disposable;
import consulo.execution.profiler.Failure;
import consulo.execution.profiler.ProfilerDumpFileParsingResult;
import consulo.execution.profiler.ProfilerDumpParserProvider;
import consulo.execution.profiler.Success;
import consulo.execution.profiler.icon.ExecutionProfilerIconGroup;
import consulo.execution.profiler.impl.internal.session.ProfilerCapture;
import consulo.execution.profiler.impl.internal.session.ProfilerSessionManager;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileEditor.FileEditorManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RecentsManager;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class ProfilerSnapshotService {
    public static final String RECENT_SNAPSHOTS_KEY = "profiler.recent.snapshots";

    private static final Logger LOG = Logger.getInstance(ProfilerSnapshotService.class);

    private final Project myProject;
    private final Application myApplication;
    private final Provider<FileEditorManager> myFileEditorManager;
    private final VirtualFileManager myVirtualFileManager;
    private final RecentsManager myRecentsManager;
    private final JBPopupFactory myPopupFactory;
    private final ProfilerSessionManager mySessionManager;
    private final List<Runnable> myRecentSnapshotsListeners = new CopyOnWriteArrayList<>();

    @Inject
    public ProfilerSnapshotService(
        Project project,
        Application application,
        Provider<FileEditorManager> fileEditorManager,
        VirtualFileManager virtualFileManager,
        RecentsManager recentsManager,
        JBPopupFactory popupFactory,
        ProfilerSessionManager sessionManager
    ) {
        myProject = project;
        myApplication = application;
        myFileEditorManager = fileEditorManager;
        myVirtualFileManager = virtualFileManager;
        myRecentsManager = recentsManager;
        myPopupFactory = popupFactory;
        mySessionManager = sessionManager;
    }

    @RequiredUIAccess
    public void chooseAndOpen() {
        boolean acceptsAnyFile = !ProfilerDumpParserLookup.findAnyFileProviders(myApplication).isEmpty();

        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, acceptsAnyFile, false, false, false, false)
            .withTitle(LocalizeValue.localizeTODO("Open Profiler Snapshot"))
            .withDescription(LocalizeValue.localizeTODO("Choose a profiler snapshot to open in an editor tab"));
        if (!acceptsAnyFile) {
            descriptor.withFileFilter(file -> ProfilerDumpParserLookup.hasExtensionProvider(myApplication, file.getName()));
        }

        FileChooser.chooseFile(descriptor, myProject, null).whenComplete((file, error) -> {
            if (error == null && file != null) {
                open(file);
            }
        });
    }

    public void open(VirtualFile file) {
        Path path;
        try {
            path = file.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            LocalizeValue message = LocalizeValue.localizeTODO("Only local files can be opened: " + file.getPresentableUrl());
            myProject.getUIAccess().give(() -> showError(message));
            return;
        }
        open(path);
    }

    public void open(Path path) {
        CoroutineScope.launchAsync(
            myProject.coroutineContext(),
            () -> Coroutine
                .first(CodeExecution.<Void, ProfilerSnapshotRoute>apply(ignored -> route(path)))
                .then(UIAction.<ProfilerSnapshotRoute, ProfilerSnapshotRoute>apply(route -> {
                    openRoute(path, route);
                    return route;
                }))
        );
    }

    public void parse(Path path, ProfilerDumpParserProvider provider) {
        Project project = myProject;
        ProfilerSessionManager sessionManager = mySessionManager;
        new Task.Backgroundable(project, LocalizeValue.localizeTODO("Opening profiler snapshot"), true) {
            private @Nullable ProfilerDumpFileParsingResult myResult;

            @Override
            public void run(ProgressIndicator indicator) {
                indicator.setText(LocalizeValue.localizeTODO("Reading " + path.getFileName()));
                myResult = ProfilerDumpParserLookup.parse(project, path.toFile(), List.of(provider), indicator);
            }

            @Override
            @RequiredUIAccess
            public void onSuccess() {
                ProfilerDumpFileParsingResult result = myResult;
                if (project.isDisposed() || result == null) {
                    return;
                }

                String name = getFileName(path);
                switch (result) {
                    case Success success -> {
                        registerRecentSnapshot(path);
                        ProfilerCapture capture = new ProfilerCapture(
                            null,
                            LocalizeValue.of(name),
                            ExecutionProfilerIconGroup.profilecpu(),
                            success.getData(),
                            true
                        );
                        sessionManager.openCaptureEditor(capture, true);
                    }
                    case Failure failure -> showError(ProfilerDumpParserLookup.getMessage(failure, name));
                }
            }

            @Override
            public void onThrowable(Throwable throwable) {
                LOG.error("Failed to open profiler snapshot " + path + " with " + provider.getId(), throwable);
                if (!project.isDisposed()) {
                    showError(LocalizeValue.of(throwable));
                }
            }
        }.queue();
    }

    @RequiredUIAccess
    public List<String> getRecentSnapshots() {
        List<String> entries = myRecentsManager.getRecentEntries(RECENT_SNAPSHOTS_KEY);
        return entries == null ? List.of() : List.copyOf(entries);
    }

    @RequiredUIAccess
    public void registerRecentSnapshot(Path path) {
        myRecentsManager.registerRecentEntry(RECENT_SNAPSHOTS_KEY, path.toString());
        for (Runnable listener : myRecentSnapshotsListeners) {
            try {
                listener.run();
            }
            catch (Throwable e) {
                LOG.error("Recent profiler snapshots listener failed", e);
            }
        }
    }

    public Disposable addRecentSnapshotsListener(Runnable listener) {
        myRecentSnapshotsListeners.add(listener);
        return () -> myRecentSnapshotsListeners.remove(listener);
    }

    private ProfilerSnapshotRoute route(Path path) {
        if (!Files.exists(path)) {
            return ProfilerSnapshotRoute.MISSING;
        }

        if (!Files.isDirectory(path)) {
            List<ProfilerDumpParserProvider> byExtension = ProfilerDumpParserLookup.findByExtension(myApplication, getFileName(path));
            if (!byExtension.isEmpty()) {
                VirtualFile file = ProfilerDumpParserLookup.hasExclusiveExtensionProvider(byExtension)
                    ? myVirtualFileManager.refreshAndFindFileByNioPath(path)
                    : null;
                return new ProfilerSnapshotRoute(true, file, byExtension);
            }
        }

        return new ProfilerSnapshotRoute(true, null, ProfilerDumpParserLookup.findAnyFileProviders(myApplication));
    }

    @RequiredUIAccess
    private void openRoute(Path path, ProfilerSnapshotRoute route) {
        if (myProject.isDisposed()) {
            return;
        }

        if (!route.exists()) {
            showError(LocalizeValue.localizeTODO("'" + path + "' does not exist"));
            return;
        }

        VirtualFile editorFile = route.editorFile();
        if (editorFile != null) {
            myFileEditorManager.get().openFile(editorFile, true, true);
            return;
        }

        chooseProviderAndParse(path, route.providers());
    }

    @RequiredUIAccess
    private void chooseProviderAndParse(Path path, List<ProfilerDumpParserProvider> providers) {
        if (providers.isEmpty()) {
            showError(LocalizeValue.localizeTODO("No installed profiler can open '" + path + "'"));
            return;
        }

        if (providers.size() == 1) {
            parse(path, providers.get(0));
            return;
        }

        String title = LocalizeValue.localizeTODO("Open Snapshot As").get();
        BaseListPopupStep<ProfilerDumpParserProvider> step = new BaseListPopupStep<ProfilerDumpParserProvider>(title, providers) {
            @Override
            public String getTextFor(ProfilerDumpParserProvider value) {
                return value.getName().get();
            }

            @Override
            public PopupStep<?> onChosen(ProfilerDumpParserProvider selectedValue, boolean finalChoice) {
                return doFinalStep(() -> parse(path, selectedValue));
            }
        };
        myPopupFactory.createListPopup(step).showCenteredInCurrentWindow(myProject);
    }

    private static String getFileName(Path path) {
        Path fileName = path.getFileName();
        return fileName == null ? path.toString() : fileName.toString();
    }

    @RequiredUIAccess
    private static void showError(LocalizeValue message) {
        MessageBoxes.okError(message).title(LocalizeValue.localizeTODO("Open Profiler Snapshot")).showAsync();
    }
}
