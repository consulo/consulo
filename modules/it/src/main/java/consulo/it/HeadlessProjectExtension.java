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
package consulo.it;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.project.Project;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import java.lang.reflect.Method;

/**
 * {@link HeadlessApplicationExtension} plus projects: a test method can take a {@link HeadlessProjects} parameter to open
 * projects over directories it prepared, a bare {@link Project} parameter for a project over a fresh directory, or any
 * {@code @ServiceAPI(PROJECT)} service of that project. Whatever the test leaves open is settled, closed and disposed after
 * the test, before the logged errors of the test are checked, so a scanning or dumb task of one test never runs into the next.
 * <p>
 * Mirrors {@code consulo.test.junit.impl.extension.ConsuloProjectLoader}, which extends the application loader the same way.
 *
 * @author VISTALL
 */
public class HeadlessProjectExtension extends HeadlessApplicationExtension {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(HeadlessProjectExtension.class);

    @Override
    public void afterEach(ExtensionContext context) {
        Throwable closeFailure = null;
        try {
            closeProjects(context);
        }
        catch (Throwable t) {
            closeFailure = t;
        }

        try {
            super.afterEach(context);
        }
        catch (Throwable t) {
            if (closeFailure == null) {
                throw t;
            }
            closeFailure.addSuppressed(t);
        }

        if (closeFailure != null) {
            throw new AssertionError("closing the projects of the test failed", closeFailure);
        }
    }

    private static void closeProjects(ExtensionContext context) throws Exception {
        HeadlessProjects projects = context.getStore(NAMESPACE).remove(HeadlessProjects.class, HeadlessProjects.class);
        if (projects != null) {
            projects.closeAll();
        }
    }

    @Override
    protected boolean isInjectable(Class<?> type) {
        return super.isInjectable(type) || type == HeadlessProjects.class || type == Project.class || isProjectService(type);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        try {
            if (type == HeadlessProjects.class) {
                return projects(extensionContext);
            }
            if (type == Project.class) {
                return projects(extensionContext).defaultProject();
            }
            if (isProjectService(type)) {
                return projects(extensionContext).defaultProject().getInstance(type);
            }
        }
        catch (Exception e) {
            throw new ParameterResolutionException("cannot open the project of the test", e);
        }
        return super.resolveParameter(parameterContext, extensionContext);
    }

    private static HeadlessProjects projects(ExtensionContext context) {
        Application application = ensureBooted();
        return context.getStore(NAMESPACE).getOrComputeIfAbsent(
            HeadlessProjects.class,
            key -> new HeadlessProjects(testName(context), application),
            HeadlessProjects.class
        );
    }

    private static String testName(ExtensionContext context) {
        String name = context.getTestMethod().map(Method::getName).orElse("test");
        StringBuilder safe = new StringBuilder();
        for (int i = 0; i < name.length() && safe.length() < 40; i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                safe.append(c);
            }
        }
        return safe.isEmpty() ? "test" : safe.toString();
    }

    private static boolean isProjectService(Class<?> type) {
        ServiceAPI serviceAPI = type.getAnnotation(ServiceAPI.class);
        return serviceAPI != null && serviceAPI.value() == ComponentScope.PROJECT;
    }
}
