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
package consulo.ide.impl.idea.openapi.application.impl;

import consulo.application.AppUIExecutor;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.constraint.Expiration;
import consulo.component.ComponentManager;
import consulo.project.Project;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

public class AppUIExecutorImpl extends BaseExpirableExecutorMixinImpl<AppUIExecutorImpl> implements AppUIExecutor {
    private static final Executor EDT_EXECUTOR = command -> {
        Application application = Application.get();
        if (application.isDispatchThread()) {
            command.run();
        }
        else {
            application.invokeLater(command);
        }
    };

    public AppUIExecutorImpl() {
        super(new ContextConstraint[0], new BooleanSupplier[0], Collections.emptySet(), EDT_EXECUTOR);
    }

    private AppUIExecutorImpl(ContextConstraint[] constraints, BooleanSupplier[] cancellationConditions, Set<? extends Expiration> expirableHandles) {
        super(constraints, cancellationConditions, expirableHandles, EDT_EXECUTOR);
    }

    @Nonnull
    @Override
    protected AppUIExecutorImpl cloneWith(ContextConstraint[] constraints, BooleanSupplier[] cancellationConditions, Set<? extends Expiration> expirationSet) {
        return new AppUIExecutorImpl(constraints, cancellationConditions, expirationSet);
    }

    @Nonnull
    @Override
    public AppUIExecutor later() {
        return withConstraint(new ContextConstraint() {
            private volatile boolean scheduled;

            @Override
            public boolean isCorrectContext() {
                return scheduled && UIAccess.isUIThread();
            }

            @Override
            public void schedule(Runnable runnable) {
                dispatchLaterUnconstrained(() -> {
                    scheduled = true;
                    runnable.run();
                });
            }

            @Override
            public String toString() {
                return "later";
            }
        });
    }

    @Nonnull
    @Override
    public AppUIExecutor withDocumentsCommitted(@Nonnull ComponentManager project) {
        return withConstraint(new WithDocumentsCommitted((Project) project), project);
    }

    @Nonnull
    @Override
    public AppUIExecutor inSmartMode(@Nonnull ComponentManager project) {
        return withConstraint(new InSmartMode((Project) project), project);
    }

    @Override
    public void dispatchLaterUnconstrained(Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable);
    }
}
