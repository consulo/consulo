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
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import java.util.Map;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class EditorColorsManager {
  public static final String DEFAULT_SCHEME_NAME = "Default";

  public static EditorColorsManager getInstance() {
    return Application.get().getInstance(EditorColorsManager.class);
  }

  public abstract void addColorsScheme(@Nonnull EditorColorsScheme scheme);

  public abstract void removeAllSchemes();

  @Nonnull
  public abstract EditorColorsScheme[] getAllSchemes();

  @Nonnull
  public abstract Map<String, EditorColorsScheme> getBundledSchemes();

  public abstract void setGlobalScheme(EditorColorsScheme scheme);

  @Nonnull
  public abstract EditorColorsScheme getGlobalScheme();

  @Nonnull
  public EditorColorsScheme getCurrentScheme() {
    return getGlobalScheme();
  }

  public abstract EditorColorsScheme getScheme(String schemeName);

  public abstract boolean isDefaultScheme(EditorColorsScheme scheme);

  /**
   * @deprecated use {@link EditorColorsListener.class} instead
   */
  @SuppressWarnings("MethodMayBeStatic")
  @Deprecated
  public final void addEditorColorsListener(@Nonnull EditorColorsListener listener) {
    ApplicationManager.getApplication().getMessageBus().connect().subscribe(EditorColorsListener.class, listener);
  }

  /**
   * @deprecated use {@link EditorColorsListener.class} instead
   */
  @SuppressWarnings("MethodMayBeStatic")
  @Deprecated
  public final void addEditorColorsListener(@Nonnull EditorColorsListener listener, @Nonnull Disposable disposable) {
    ApplicationManager.getApplication().getMessageBus().connect(disposable).subscribe(EditorColorsListener.class, listener);
  }

  @Nonnull
  public EditorColorsScheme getSchemeForCurrentUITheme() {
    return getGlobalScheme();
  }

  public abstract boolean isUseOnlyMonospacedFonts();
  public abstract void setUseOnlyMonospacedFonts(boolean b);
}
