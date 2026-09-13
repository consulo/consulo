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
package consulo.language.editor.navigation;

import consulo.application.AccessToken;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Carries the contexts collected by {@link NavigationContextCollector} through a
 * navigation:
 * <ul>
 * <li>{@link #withContexts} installs them in a thread-local scope for the duration of the
 * gesture, so any code executing within — resolve, target computation, handlers — can read
 * them via {@link #currentContext} without parameter plumbing;</li>
 * <li>{@link #currentContexts} captures the list for continuation on another thread or a
 * deferred callback (an ambiguity popup navigating later), where {@link #withContexts}
 * restores it;</li>
 * <li>the navigation action finally stamps the list onto the opened target editor under
 * {@link #NAVIGATION_CONTEXTS}, where presentation layers consume it.</li>
 * </ul>
 */
public final class NavigationContexts {
    public static final Key<List<Object>> NAVIGATION_CONTEXTS = Key.create("navigation.contexts");

    private static final ThreadLocal<List<Object>> CURRENT = ThreadLocal.withInitial(List::of);

    private NavigationContexts() {
    }

    public static List<Object> collect(Project project, Editor editor, PsiFile file, int offset) {
        List<Object> contexts = new ArrayList<>();
        project.getExtensionPoint(NavigationContextCollector.class).forEachExtensionSafe(collector -> {
            Object context = collector.collectContext(editor, file, offset);
            if (context != null) {
                contexts.add(context);
            }
        });
        return List.copyOf(contexts);
    }

    public static AccessToken withContexts(List<Object> contexts) {
        List<Object> previous = CURRENT.get();
        CURRENT.set(contexts);
        return new AccessToken() {
            @Override
            public void finish() {
                CURRENT.set(previous);
            }
        };
    }

    public static List<Object> currentContexts() {
        return CURRENT.get();
    }

    public static @Nullable <T> T currentContext(Class<T> type) {
        for (Object context : CURRENT.get()) {
            if (type.isInstance(context)) {
                return type.cast(context);
            }
        }
        return null;
    }

    public static @Nullable <T> T findContext(@Nullable List<Object> contexts, Class<T> type) {
        if (contexts == null) {
            return null;
        }
        for (Object context : contexts) {
            if (type.isInstance(context)) {
                return type.cast(context);
            }
        }
        return null;
    }
}
