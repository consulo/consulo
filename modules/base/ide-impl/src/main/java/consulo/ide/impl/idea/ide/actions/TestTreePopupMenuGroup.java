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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.execution.test.action.ViewAssertEqualsDiffAction;
import consulo.ide.impl.idea.execution.testframework.sm.runner.ui.statistics.SMTestRunnerStatisticsGroup;
import consulo.ide.impl.idea.execution.testframework.sm.runner.ui.statistics.SMTestRunnerTestsTreeGroup;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;

/**
 * @author UNV
 * @since 2025-09-15
 */
@ActionImpl(
    id = IdeActions.GROUP_TESTTREE_POPUP,
    children = {
        @ActionRef(type = ViewAssertEqualsDiffAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = RunContextGroup.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = EditSourceAction.class),
        @ActionRef(type = ViewSourceAction.class),
        @ActionRef(type = SMTestRunnerTestsTreeGroup.class),
        @ActionRef(type = SMTestRunnerStatisticsGroup.class)
    }
)
public class TestTreePopupMenuGroup extends DefaultActionGroup implements DumbAware {
    public TestTreePopupMenuGroup() {
        super(LocalizeValue.empty(), false);
    }
}
