/*
 * Copyright 2000-2023 JetBrains s.r.o. and contributors.
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
package consulo.versionControlSystem.checkin;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * Allows to show a link near the error, that can be used to show more detailed explanation or propose a quick fix for the problem.
 */
public interface CommitProblemWithDetails extends CommitProblem {
    /**
     * If empty, the whole {@link CommitProblem#getText()} will become a link.
     */
    default LocalizeValue getShowDetailsLink() {
        return LocalizeValue.empty();
    }

    LocalizeValue getShowDetailsAction();

    /**
     * Allows showing details for the problem (ex: by opening a toolwindow tab with a list of failed inspections).
     * Modal dialog will be closed after this call if it is shown.
     */
    @RequiredUIAccess
    void showDetails(Project project);
}
