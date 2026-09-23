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
package consulo.execution.impl.internal.console;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.content.scope.SearchScope;
import consulo.execution.ui.console.ConsoleState;
import consulo.execution.ui.console.ConsoleView;
import consulo.project.Project;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-09-23
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedConsoleViewFactory implements ConsoleViewFactory {
    @Override
    public ConsoleView createConsoleView(
        Project project,
        SearchScope searchScope,
        boolean viewer,
        ConsoleState initialState,
        boolean usePredefinedMessageFilter
    ) {
        return new UnifiedConsoleViewImpl(project, viewer);
    }
}
