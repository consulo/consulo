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
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit 5 extension that boots the real headless {@link Application} once per JVM and injects it
 * (and any {@code @ServiceAPI(APPLICATION)} service) into test constructor/method parameters.
 * <p>
 * Mirrors {@code consulo.test.junit.impl.extension.ConsuloApplicationLoader}, but stands up the
 * real {@link consulo.it.internal.HeadlessApplicationImpl} instead of the light stub, and keeps a
 * single application alive for the whole test run (the application is a JVM-wide singleton).
 *
 * @author VISTALL
 */
public class HeadlessApplicationExtension implements BeforeAllCallback, ParameterResolver {
    private static volatile Application ourApplication;

    @Override
    public void beforeAll(ExtensionContext context) {
        ensureBooted();
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
