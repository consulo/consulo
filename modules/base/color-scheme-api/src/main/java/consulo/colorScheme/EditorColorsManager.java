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
package consulo.colorScheme;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.colorScheme.event.EditorColorsListener;
import consulo.colorScheme.internal.EditorColorsManagerInternal;
import consulo.disposer.Disposable;

import jakarta.annotation.Nonnull;

import java.util.Map;

@ServiceAPI(ComponentScope.APPLICATION)
public sealed interface EditorColorsManager permits EditorColorsManagerInternal {
    @Deprecated
    public static final String DEFAULT_SCHEME_NAME = EditorColorsScheme.DEFAULT_SCHEME_NAME;

    public static EditorColorsManager getInstance() {
        return Application.get().getInstance(EditorColorsManager.class);
    }

    void addColorsScheme(@Nonnull EditorColorsScheme scheme);

    void removeAllSchemes();

    @Nonnull
    EditorColorsScheme[] getAllSchemes();

    @Nonnull
    Map<String, EditorColorsScheme> getBundledSchemes();

    void setGlobalScheme(EditorColorsScheme scheme);

    @Nonnull
    EditorColorsScheme getGlobalScheme();

    @Nonnull
    default EditorColorsScheme getCurrentScheme() {
        return getGlobalScheme();
    }

    EditorColorsScheme getScheme(String schemeName);

    boolean isDefaultScheme(EditorColorsScheme scheme);

    /**
     * @deprecated use {@link EditorColorsListener.class} instead
     */
    @SuppressWarnings("MethodMayBeStatic")
    @Deprecated
    default void addEditorColorsListener(@Nonnull EditorColorsListener listener) {
        ApplicationManager.getApplication().getMessageBus().connect().subscribe(EditorColorsListener.class, listener);
    }

    /**
     * @deprecated use {@link EditorColorsListener.class} instead
     */
    @SuppressWarnings("MethodMayBeStatic")
    @Deprecated
    default void addEditorColorsListener(@Nonnull EditorColorsListener listener, @Nonnull Disposable disposable) {
        ApplicationManager.getApplication().getMessageBus().connect(disposable).subscribe(EditorColorsListener.class, listener);
    }

    @Nonnull
    default EditorColorsScheme getSchemeForCurrentUITheme() {
        return getGlobalScheme();
    }

    boolean isUseOnlyMonospacedFonts();

    void setUseOnlyMonospacedFonts(boolean b);
}
