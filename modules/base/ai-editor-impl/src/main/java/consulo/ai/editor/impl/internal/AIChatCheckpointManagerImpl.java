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
package consulo.ai.editor.impl.internal;

import consulo.ai.AIChatCheckpoint;
import consulo.ai.AIChatCheckpointManager;
import consulo.annotation.component.ServiceImpl;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.localHistory.Label;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Checkpoints as labels of the local history - the user finds them there too, and can go back to one by hand.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ServiceImpl
@Singleton
public class AIChatCheckpointManagerImpl implements AIChatCheckpointManager {
    private final Project myProject;
    private final ApplicationConcurrency myConcurrency;

    @Inject
    public AIChatCheckpointManagerImpl(Project project, ApplicationConcurrency concurrency) {
        myProject = project;
        myConcurrency = concurrency;
    }

    @Override
    public CompletableFuture<AIChatCheckpoint> create(LocalizeValue name, Path directory) {
        return CompletableFuture.supplyAsync(() -> LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory), myConcurrency.executor())
            .thenCompose(root -> {
                CompletableFuture<?> refreshed = new CompletableFuture<>();
                if (root == null) {
                    refreshed.complete(null);
                }
                else {
                    root.refresh(true, true, () -> refreshed.complete(null));
                }
                return refreshed;
            })
            .thenApply(ignored -> checkpoint(LocalHistory.getInstance().putSystemLabel(myProject, name)));
    }

    private AIChatCheckpoint checkpoint(Label label) {
        return new AIChatCheckpoint() {
            @RequiredUIAccess
            @Override
            public CompletableFuture<?> revert(Path directory) {
                VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
                if (root == null) {
                    return CompletableFuture.failedFuture(new FileNotFoundException(directory.toString()));
                }
                try {
                    label.revert(myProject, root);
                }
                catch (LocalHistoryException e) {
                    return CompletableFuture.failedFuture(e);
                }
                root.refresh(true, true);
                return CompletableFuture.completedFuture(null);
            }
        };
    }
}
