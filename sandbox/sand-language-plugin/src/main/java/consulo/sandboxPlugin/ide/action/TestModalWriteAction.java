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
package consulo.sandboxPlugin.ide.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.application.progress.ProgressBuilderFactory;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.application.progress.ProgressIndicator;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.util.lang.TimeoutUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2025-07-16
 */
@ActionImpl(id = "ShowUITesterAction", parents = @ActionParentRef(@ActionRef(id = IdeActions.TOOLS_MENU)))
public class TestModalWriteAction extends DumbAwareAction {
    private final ProgressBuilderFactory myProgressBuilderFactory;

    @Inject
    public TestModalWriteAction(ProgressBuilderFactory progressBuilderFactory) {
        super(LocalizeValue.localizeTODO("Test Modal Write"));
        myProgressBuilderFactory = progressBuilderFactory;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        myProgressBuilderFactory.newProgressBuilder(e.getData(Project.KEY), LocalizeValue.localizeTODO("Test Write"))
            .cancelable()
            .execute(UIAccess.current(), () -> Coroutine.first(WriteLock.apply((o, c) -> {
                    ProgressIndicator indicator = ProgressIndicator.from(c);
                    indicator.setIndeterminate(false);
                    
                    for (int i = 0; i < 100; i++) {
                        indicator.setFraction(i / 100f);

                        indicator.checkCanceled();

                        TimeoutUtil.sleep(1000L);
                    }
                    return null;
                })));
    }
}
