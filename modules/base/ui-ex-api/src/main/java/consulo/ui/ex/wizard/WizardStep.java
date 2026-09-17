/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.ex.wizard;

import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2019-08-20
 */
public interface WizardStep<CONTEXT> {
    @RequiredUIAccess
    Component getComponent(CONTEXT context, Disposable uiDisposable);

    default @Nullable Component getPreferredFocusedComponent() {
        return null;
    }

    default void onStepEnter(CONTEXT context) {
    }

    default void onStepLeave(CONTEXT context) {
    }

    /**
     * Asks the step whether the wizard may move on, and lets it do the work that answer depends on. The answer is a future
     * because that work - resolving an external project, for one - has no business blocking the thread the user interface runs
     * on. A failed future keeps the wizard where it is and shows its reason.
     */
    @RequiredUIAccess
    default CompletableFuture<?> validateStep(CONTEXT context) {
        return CompletableFuture.completedFuture(null);
    }

    default boolean isVisible(CONTEXT context) {
        return true;
    }
}
