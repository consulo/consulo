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
import consulo.it.internal.HeadlessApplicationImpl;
import consulo.util.concurrent.ThreadIssueException;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.List;

/**
 * JUnit 5 extension that boots the real headless {@link Application} once per JVM and injects it
 * (and any {@code @ServiceAPI(APPLICATION)} service) into test constructor/method parameters.
 * <p>
 * Mirrors {@code consulo.test.junit.impl.extension.ConsuloApplicationLoader}, but stands up the
 * real {@link consulo.it.internal.HeadlessApplicationImpl} instead of the light stub, and keeps a
 * single application alive for the whole test run (the application is a JVM-wide singleton).
 * <p>
 * A write action started on the UI thread throws, unless the test opts out with
 * {@link AllowWriteLockUnderUIThread}. It is recorded as well as thrown, so that a violation swallowed by a
 * catch-all still fails the test.
 *
 * @author VISTALL
 */
public class HeadlessApplicationExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private static volatile Application ourApplication;

    @Override
    public void beforeAll(ExtensionContext context) {
        ensureBooted();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        HeadlessApplicationImpl.takeThreadIssues();
        HeadlessApplicationImpl.setAllowWriteLockUnderUIThread(isAllowWriteLockUnderUIThread(context));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        HeadlessApplicationImpl.setAllowWriteLockUnderUIThread(false);

        List<ThreadIssueException> issues = HeadlessApplicationImpl.takeThreadIssues();

        if (!issues.isEmpty()) {
            AssertionError error = new AssertionError(issues.size() + " thread issue(s) reported during the test");
            issues.forEach(error::addSuppressed);
            throw error;
        }
    }

    private static boolean isAllowWriteLockUnderUIThread(ExtensionContext context) {
        return context.getTestMethod().map(method -> method.isAnnotationPresent(AllowWriteLockUnderUIThread.class)).orElse(false)
            || context.getTestClass().map(type -> type.isAnnotationPresent(AllowWriteLockUnderUIThread.class)).orElse(false);
    }

    protected static Application ensureBooted() {
        Application application = ourApplication;
        if (application == null) {
            synchronized (HeadlessApplicationExtension.class) {
                application = ourApplication;
                if (application == null) {
                    application = HeadlessApplicationBuilder.build();
                    ourApplication = application;
                }
            }
        }
        return application;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException {
        return isInjectable(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException {
        return getInjectValue(parameterContext.getParameter().getType());
    }

    protected boolean isInjectable(Class<?> type) {
        if (type == Application.class) {
            return true;
        }
        ServiceAPI serviceAPI = type.getAnnotation(ServiceAPI.class);
        return serviceAPI != null && serviceAPI.value() == ComponentScope.APPLICATION;
    }

    protected Object getInjectValue(Class<?> type) {
        Application application = ensureBooted();
        if (type == Application.class) {
            return application;
        }
        return application.getInstance(type);
    }
}
