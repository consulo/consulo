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
package com.intellij.openapi.editor.colors;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.util.messages.Topic;
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import java.util.Map;

public abstract class EditorColorsManager {
  public static final Topic<EditorColorsListener> TOPIC = Topic.create("EditorColorsListener", EditorColorsListener.class);

  public static final String DEFAULT_SCHEME_NAME = "Default";

  public static EditorColorsManager getInstance() {
    return ServiceManager.getService(EditorColorsManager.class);
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
   * @deprecated use {@link #TOPIC} instead
   */
  @SuppressWarnings("MethodMayBeStatic")
  @Deprecated
  public final void addEditorColorsListener(@Nonnull EditorColorsListener listener) {
    ApplicationManager.getApplication().getMessageBus().connect().subscribe(TOPIC, listener);
  }

  /**
   * @deprecated use {@link #TOPIC} instead
   */
  @SuppressWarnings("MethodMayBeStatic")
  @Deprecated
  public final void addEditorColorsListener(@Nonnull EditorColorsListener listener, @Nonnull Disposable disposable) {
    ApplicationManager.getApplication().getMessageBus().connect(disposable).subscribe(TOPIC, listener);
  }

  @Nonnull
  public EditorColorsScheme getSchemeForCurrentUITheme() {
    return getGlobalScheme();
  }

  public abstract boolean isUseOnlyMonospacedFonts();
  public abstract void setUseOnlyMonospacedFonts(boolean b);
}
