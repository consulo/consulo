/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.internal;

import consulo.application.progress.ProgressBuilderFactory;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Alerts;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.lang.TimeoutUtil;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2024-11-23
 */
public class TestModalTaskAction extends DumbAwareAction {
    @Nonnull
    private final ProgressBuilderFactory myProgressBuilderFactory;

    public TestModalTaskAction(@Nonnull ProgressBuilderFactory progressBuilderFactory) {
        super(LocalizeValue.of("Test Modal Task"));

        myProgressBuilderFactory = progressBuilderFactory;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);

        UIAccess uiAccess = UIAccess.current();

        SimpleReference<Boolean> started = SimpleReference.create(Boolean.FALSE);

        CompletableFuture<String> future = myProgressBuilderFactory.newProgressBuilder(project, LocalizeValue.of("Modal Action..."))
            .cancelable()
            .modal()
            .execute(progressIndicator -> {
                started.set(Boolean.TRUE);

                progressIndicator.setIndeterminate(true);
                ProgressIndicatorProvider.checkCanceled();
                TimeoutUtil.sleep(5000L);
                ProgressIndicatorProvider.checkCanceled();
                progressIndicator.setTextValue(LocalizeValue.of("After 5 seconds"));              
                ProgressIndicatorProvider.checkCanceled();
                TimeoutUtil.sleep(5000L);
                ProgressIndicatorProvider.checkCanceled();
                progressIndicator.setTextValue(LocalizeValue.of("After 10 seconds"));
                ProgressIndicatorProvider.checkCanceled();
                TimeoutUtil.sleep(5000L);
                ProgressIndicatorProvider.checkCanceled();
                return "Success Result";
            });

        future.whenCompleteAsync((s, throwable) -> {
            if (throwable != null) {
                Alerts.okError(LocalizeValue.ofNullable(throwable.getLocalizedMessage())).showAsync(project);
            }
            else {
                Alerts.okInfo(LocalizeValue.ofNullable(s)).showAsync(project);
            }
        }, uiAccess);

        System.out.println("started: " + started.get());
    }
}
