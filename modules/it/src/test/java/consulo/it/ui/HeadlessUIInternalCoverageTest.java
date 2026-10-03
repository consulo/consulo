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
package consulo.it.ui;

import consulo.it.HeadlessApplicationExtension;
import consulo.it.internal.HeadlessUIInternal;
import consulo.ui.ex.internal.UIInternalEx;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class HeadlessUIInternalCoverageTest {
    private static final Set<String> BASE_TYPES = Set.of(UIInternal.class.getName(), UIInternalEx.class.getName());

    @Test
    public void theIntegrationTestApplicationRunsOnHeadlessUIInternal() {
        assertThat(UIInternal.get()).isInstanceOf(HeadlessUIInternal.class);
    }

    @Test
    public void noFactoryResolvedOnHeadlessUIInternalFallsIntoAThrowingBaseBody() {
        UIInternal uiInternal = UIInternal.get();
        assertThat(uiInternal).isInstanceOf(HeadlessUIInternal.class);

        List<Method> inherited = inheritedFactories();
        assertThat(inherited)
            .as("the scan of HeadlessUIInternal saw no factory inherited from UIInternal/UIInternalEx, so it checks nothing")
            .isNotEmpty();

        List<String> unsupported = new ArrayList<>();
        for (Method method : inherited) {
            StackTraceElement thrower = throwingBaseFrame(uiInternal, method);
            if (thrower != null) {
                unsupported.add(describe(method) + " -> UnsupportedOperationException at " + thrower);
            }
        }

        assertThat(unsupported)
            .as("HeadlessUIInternal must override these factories, an IT reaching one dies with UnsupportedOperationException")
            .isEmpty();
    }

    private static List<Method> inheritedFactories() {
        return Arrays.stream(HeadlessUIInternal.class.getMethods())
            .filter(method -> !Modifier.isStatic(method.getModifiers()))
            .filter(method -> !Modifier.isAbstract(method.getModifiers()))
            .filter(method -> !method.isBridge() && !method.isSynthetic())
            .filter(method -> BASE_TYPES.contains(method.getDeclaringClass().getName()))
            .sorted(Comparator.comparing(HeadlessUIInternalCoverageTest::describe))
            .toList();
    }

    private static @Nullable StackTraceElement throwingBaseFrame(UIInternal uiInternal, Method method) {
        Object[] arguments = Arrays.stream(method.getParameterTypes()).map(HeadlessUIInternalCoverageTest::defaultValue).toArray();
        try {
            method.invoke(uiInternal, arguments);
            return null;
        }
        catch (InvocationTargetException e) {
            return unsupportedFromBase(e.getCause());
        }
        catch (IllegalAccessException e) {
            throw new AssertionError("cannot invoke " + describe(method), e);
        }
    }

    private static @Nullable StackTraceElement unsupportedFromBase(@Nullable Throwable thrown) {
        for (Throwable current = thrown; current != null; current = current.getCause()) {
            if (current instanceof UnsupportedOperationException) {
                StackTraceElement[] trace = current.getStackTrace();
                if (trace.length > 0 && BASE_TYPES.contains(trace[0].getClassName())) {
                    return trace[0];
                }
            }
        }
        return null;
    }

    private static @Nullable Object defaultValue(Class<?> type) {
        return type.isPrimitive() ? Array.get(Array.newInstance(type, 1), 0) : null;
    }

    private static String describe(Method method) {
        String parameters = Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(", "));
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName() + "(" + parameters + ")";
    }
}
