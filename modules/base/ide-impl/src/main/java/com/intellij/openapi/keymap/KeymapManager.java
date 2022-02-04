/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.keymap;

import consulo.application.internal.PerApplicationInstance;
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

public abstract class KeymapManager {
  public static final String DEFAULT_IDEA_KEYMAP = "$default";
  public static final String MAC_OS_X_KEYMAP = "Mac OS X";
  public static final String X_WINDOW_KEYMAP = "Default for XWin";
  public static final String KDE_KEYMAP = "Default for KDE";
  public static final String GNOME_KEYMAP = "Default for GNOME";
  public static final String MAC_OS_X_10_5_PLUS_KEYMAP = "Mac OS X 10.5+";

  private static final Supplier<KeymapManager> ourInstance = PerApplicationInstance.of(KeymapManager.class);

  @Nonnull
  public static KeymapManager getInstance(){
    return ourInstance.get();
  }

  public abstract Keymap getActiveKeymap();

  @Nullable
  public abstract Keymap getKeymap(@Nonnull String name);

  /**
   * @deprecated use {@link KeymapManager#addKeymapManagerListener(KeymapManagerListener, Disposable)} instead
   */
  public abstract void addKeymapManagerListener(@Nonnull KeymapManagerListener listener);

  public abstract void addKeymapManagerListener(@Nonnull KeymapManagerListener listener, @Nonnull Disposable parentDisposable);

  public abstract void removeKeymapManagerListener(@Nonnull KeymapManagerListener listener);
}
