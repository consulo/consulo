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
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.test.light.LightApplicationBuilder;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.*;

/**
 * @author VISTALL
 * @since 2025-01-14
 */
public class ConsuloApplicationLoader implements BeforeAllCallback, AfterAllCallback, ParameterResolver {
    /**
     * @see org.junit.jupiter.engine.extension.TestInfoParameterResolver
     */
    private static class DefaultTestInfo implements TestInfo {
        private final String displayName;
        private final Set<String> tags;
        private final Optional<Class<?>> testClass;
        private final Optional<Method> testMethod;

        DefaultTestInfo(ExtensionContext extensionContext) {
            this.displayName = extensionContext.getDisplayName();
            this.tags = extensionContext.getTags();
            this.testClass = extensionContext.getTestClass();
            this.testMethod = extensionContext.getTestMethod();
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        @Override
        public Set<String> getTags() {
            return this.tags;
        }

        @Override
        public Optional<Class<?>> getTestClass() {
            return this.testClass;
        }

        @Override
        public Optional<Method> getTestMethod() {
            return this.testMethod;
        }

        @Override
        public String toString() {
            return "DefaultTestInfo{" +
                "displayName='" + displayName + '\'' +
                ", tags=" + tags +
                ", testClass=" + testClass +
                ", testMethod=" + testMethod +
                '}';
        }

        private static Object nullSafeGet(Optional<?> optional) {
            return optional != null ? optional.orElse(null) : null;
        }

    }

    private static final ExtensionContext.Namespace CONSULO = ExtensionContext.Namespace.create("consulo");

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Disposable disposable = Disposable.newDisposable("App Disposable");

        LightApplicationBuilder builder = LightApplicationBuilder.create(disposable);

        Application application = builder.build();

        ExtensionContext.Store store = context.getStore(CONSULO);

        store.getOrComputeIfAbsent(Application.class, it -> application);

        store.getOrComputeIfAbsent(Disposable.class, it -> disposable);

        subInit(store);
    }

    protected void subInit(@Nonnull ExtensionContext.Store store) {
    }

    protected boolean isImpicitInject(@Nonnull Class<?> type) {
        return type == Application.class;
    }

    protected boolean isExplicitInject(@Nonnull ComponentScope scope) {
        return scope == ComponentScope.APPLICATION;
    }

    protected <T> Object getExplicitInject(@Nonnull ExtensionContext.Store store, @Nonnull ComponentScope scope, @Nonnull Class<?> type) {
        switch (scope) {
            case APPLICATION:
                Application application = store.get(Application.class, Application.class);
                return application.getInstance(type);
        }

        throw new RuntimeException("Not supported " + scope + " " + type);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        ExtensionContext.Store store = context.getStore(CONSULO);

        Disposable disposable = store.get(Disposable.class, Disposable.class);

        disposable.disposeWithTree();

        Disposer.dispose(disposable);

        Disposer.assertIsEmpty();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();

        if (InjectingRecord.class.isAssignableFrom(type)) {
            if (!type.isRecord()) {
                throw new ParameterResolutionException(type.getName() + " must be record");
            }

            for (RecordComponent component : type.getRecordComponents()) {
                Class<?> componentType = component.getType();
                if (componentType == TestInfo.class) {
                    // custom support for TestInfo
                    continue;
                }

                if (!isInjectable(componentType)) {
                    throw new ParameterResolutionException(component.getName() + " is not injectable");
                }
            }

            return true;
        }
        else {
            return isInjectable(type);
        }
    }

    private boolean isInjectable(Class<?> type) {
        if (isImpicitInject(type)) {
            return true;
        }

        ServiceAPI serviceAPI = type.getAnnotation(ServiceAPI.class);
        return serviceAPI != null && isExplicitInject(serviceAPI.value());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        ExtensionContext.Store store = extensionContext.getStore(CONSULO);

        Class<?> type = parameterContext.getParameter().getType();

        if (InjectingRecord.class.isAssignableFrom(type)) {
            List<Class> types = new ArrayList<>();
            List<Object> values = new ArrayList<>();

            for (RecordComponent component : type.getRecordComponents()) {
                Class<?> componentType = component.getType();

                types.add(componentType);

                if (componentType == TestInfo.class) {
                    values.add(new DefaultTestInfo(extensionContext));
                }
                else {
                    values.add(getInjectValue(store, componentType));
                }
            }

            try {
                return type.getConstructor(types.toArray(Class[]::new)).newInstance(values.toArray());
            }
            catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                throw new ParameterResolutionException(type.getName() + " failed to inject", e);
            }
        }
        else {
            return getInjectValue(store, type);
        }
    }

    private Object getInjectValue(ExtensionContext.Store store, Class<?> type) {
        if (isImpicitInject(type)) {
            return store.get(type, type);
        }

        ServiceAPI serviceAPI = Objects.requireNonNull(type.getAnnotation(ServiceAPI.class));

        return getExplicitInject(store, serviceAPI.value(), type);
    }
}
