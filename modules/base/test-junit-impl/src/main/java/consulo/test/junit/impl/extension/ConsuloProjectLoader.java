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

package consulo.test.junit.impl.extension;

import consulo.annotation.component.ComponentScope;
import consulo.application.Application;
import consulo.project.Project;
import consulo.test.light.LightProjectBuilder;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * @author VISTALL
 * @since 2025-01-14
 */
public class ConsuloProjectLoader extends ConsuloApplicationLoader {
    @Override
    protected void subInit(@Nonnull ExtensionContext.Store store) {
        Application application = store.get(Application.class, Application.class);

        LightProjectBuilder projectBuilder = LightProjectBuilder.create(application);

        Project project = projectBuilder.build();

        store.getOrComputeIfAbsent(Project.class, it -> project);
    }

    @Override
    protected boolean isImpicitInject(@Nonnull Class<?> type) {
        return super.isImpicitInject(type) || type == Project.class;
    }

    @Override
    protected boolean isExplicitInject(@Nonnull ComponentScope scope) {
        return super.isExplicitInject(scope) || scope == ComponentScope.PROJECT;
    }

    @Override
    protected <T> Object getExplicitInject(@Nonnull ExtensionContext.Store store, @Nonnull ComponentScope scope, @Nonnull Class<?> type) {
        if (scope == ComponentScope.PROJECT) {
            Project project = store.get(Project.class, Project.class);
            return project.getInstance(type);
        }

        return super.getExplicitInject(store, scope, type);
    }
}
