// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.project.impl.internal;

import consulo.application.AccessToken;
import consulo.project.Project;
import consulo.project.internal.UnindexedFilesScannerExecutor;
import consulo.project.localize.ProjectLocalize;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.concurrent.coroutine.step.AwaitValue;
import consulo.util.concurrent.coroutine.step.CallSubroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.step.Loop;

public final class DumbServiceScanningListener {
    private final Project myProject;
    private final MergingQueueGuiSuspender myGuiSuspender;

    public DumbServiceScanningListener(Project project, MergingQueueGuiSuspender guiSuspender) {
        myProject = project;
        myGuiSuspender = guiSuspender;
    }

    public void subscribe() {
        subscribe(UnindexedFilesScannerExecutor.getInstance(myProject).isRunning());
    }

    void subscribe(ObservableValue<Boolean> scanningState) {
        CoroutineScope.launchAsync(myProject.coroutineContext(), () -> Coroutine.first(Loop.<Void>loopWhile(
            (input, continuation) -> !myProject.isDisposed(),
            CallSubroutine.call(Coroutine.<Void, Boolean>first(AwaitValue.until(scanningState, running -> running))
                .then(CodeExecution.<Boolean, AccessToken>apply(
                    running -> myGuiSuspender.heavyActivityStarted(ProjectLocalize.progressIndexingScanning())
                ))
                .then(AwaitValue.<AccessToken>until(() -> !scanningState.get(), scanningState))
                .then(CodeExecution.<AccessToken, Void>apply(token -> {
                    token.finish();
                    return null;
                })))
        )));
    }
}
