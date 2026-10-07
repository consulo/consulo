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
package consulo.execution.profiler.impl.internal.editor;

import consulo.application.Application;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.disposer.Disposer;
import consulo.execution.profiler.Failure;
import consulo.execution.profiler.ProfilerDumpFileParsingResult;
import consulo.execution.profiler.Success;
import consulo.execution.profiler.impl.internal.snapshot.ProfilerDumpParserLookup;
import consulo.execution.profiler.impl.internal.snapshot.ProfilerSnapshotService;
import consulo.execution.profiler.impl.internal.view.ProfilerCaptureViewFactory;
import consulo.execution.profiler.impl.internal.view.ProfilerUIUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LoadingLayout;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerSnapshotFileEditor extends ProfilerFileEditorBase {
    private final Project myProject;
    private final Application myApplication;
    private final VirtualFile myFile;
    private final ProfilerSnapshotService mySnapshotService;
    private final ProfilerCaptureViewFactory myCaptureViewFactory;

    public ProfilerSnapshotFileEditor(
        Project project,
        Application application,
        VirtualFile file,
        ProfilerSnapshotService snapshotService,
        ProfilerCaptureViewFactory captureViewFactory
    ) {
        super(file, LocalizeValue.localizeTODO("Profiler Snapshot"));
        myProject = project;
        myApplication = application;
        myFile = file;
        mySnapshotService = snapshotService;
        myCaptureViewFactory = captureViewFactory;
    }

    @RequiredUIAccess
    @Override
    protected Component createComponent() {
        DockLayout root = DockLayout.create(Space.NONE);

        Path path = toPath(myFile);
        if (path == null) {
            root.center(ProfilerUIUtil.error(LocalizeValue.localizeTODO("Only local files can be opened: " + myFile.getPresentableUrl())));
            return root;
        }

        ProgressIndicator indicator = new EmptyProgressIndicator();
        Disposer.register(this, indicator::cancel);

        LoadingLayout<DockLayout> loading = LoadingLayout.create(DockLayout.create(Space.NONE), this);
        loading.setLoadingText(LocalizeValue.localizeTODO("Reading the profiler snapshot…"));
        root.center(loading);

        String fileName = myFile.getName();
        loading.startLoading(
            () -> ProfilerDumpParserLookup.parse(
                myProject,
                path.toFile(),
                ProfilerDumpParserLookup.findByExtension(myApplication, fileName),
                indicator
            ),
            (content, result) -> {
                if (!isDisposed()) {
                    showResult(content, path, result);
                }
            }
        );
        return root;
    }

    @RequiredUIAccess
    private void showResult(DockLayout content, Path path, ProfilerDumpFileParsingResult result) {
        switch (result) {
            case Success success -> {
                mySnapshotService.registerRecentSnapshot(path);
                content.center(myCaptureViewFactory.createView(success.getData(), this));
            }
            case Failure failure -> content.center(ProfilerUIUtil.error(ProfilerDumpParserLookup.getMessage(failure, myFile.getName())));
        }
    }

    private static @Nullable Path toPath(VirtualFile file) {
        try {
            return file.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
