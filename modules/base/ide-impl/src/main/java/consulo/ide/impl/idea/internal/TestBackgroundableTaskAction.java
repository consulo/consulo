/*
 * Copyright 2013-2025 consulo.io
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
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Alerts;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.lang.TimeoutUtil;
import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2025-05-06
 */
public class TestBackgroundableTaskAction extends DumbAwareAction {
    @Nonnull
    private final ProgressBuilderFactory myProgressBuilderFactory;

    public TestBackgroundableTaskAction(@Nonnull ProgressBuilderFactory progressBuilderFactory) {
        super(LocalizeValue.of("Test Backgroundable Task"));

        myProgressBuilderFactory = progressBuilderFactory;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);

        UIAccess uiAccess = UIAccess.current();
        
        CompletableFuture<String> future = myProgressBuilderFactory.newProgressBuilder(project, LocalizeValue.of("Background Action..."))
            .cancelable()
            .execute(progressIndicator -> {
                progressIndicator.checkCanceled();
                TimeoutUtil.sleep(5000L);
                progressIndicator.checkCanceled();
                progressIndicator.setTextValue(LocalizeValue.of("After 5 seconds"));
                progressIndicator.checkCanceled();
                TimeoutUtil.sleep(5000L);
                progressIndicator.checkCanceled();
                progressIndicator.setTextValue(LocalizeValue.of("After 10 seconds"));
                progressIndicator.checkCanceled();
                TimeoutUtil.sleep(5000L);
                progressIndicator.checkCanceled();
                return "Success Result";
            });

        future.whenCompleteAsync((s, throwable) -> {
            if (throwable != null) {
                Alerts.okError(LocalizeValue.ofNullable(throwable.getLocalizedMessage())).showAsync(project);
            } else {
                Alerts.okInfo(LocalizeValue.ofNullable(s)).showAsync(project);
            }
        }, uiAccess);
    }
}
