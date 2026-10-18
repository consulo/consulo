/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.versionControlSystem.change;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * @author max
 */
public interface CommitSession {
    CommitSession VCS_COMMIT = new CommitSession() {
        @Override
        public boolean canExecute(Collection<Change> changes, String commitMessage) {
            return true;
        }

        @Override
        public void execute(Collection<Change> changes, String commitMessage) {
        }

        @Override
        public void executionCanceled() {
        }

        @Override
        public @Nullable String getHelpId() {
            return null;
        }
    };

    /**
     * Options asked for before {@link #execute}. The component calls {@code stateChanged} after each edit, so the
     * dialog showing it re-reads {@link #canExecute} and {@link #validateFields()}.
     */
    @RequiredUIAccess
    default @Nullable Component getAdditionalConfigurationUI(Collection<Change> changes, String commitMessage, Runnable stateChanged) {
        return null;
    }

    default LocalizeValue getAdditionalConfigurationTitle() {
        return LocalizeValue.empty();
    }

    @RequiredUIAccess
    boolean canExecute(Collection<Change> changes, String commitMessage);

    void execute(Collection<Change> changes, String commitMessage);

    void executionCanceled();

    /**
     * @return the ID of the help topic to show for the dialog
     * @since 10.5
     */
    @Nullable String getHelpId();

    /**
     * @return the problem to show under the options, empty when they are valid
     */
    @RequiredUIAccess
    default LocalizeValue validateFields() {
        return LocalizeValue.empty();
    }
}
