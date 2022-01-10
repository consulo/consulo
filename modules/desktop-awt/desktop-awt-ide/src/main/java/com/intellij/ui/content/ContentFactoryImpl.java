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
package com.intellij.ui.content;

import com.intellij.openapi.project.Project;
import com.intellij.ui.content.impl.ContentImpl;
import com.intellij.ui.content.impl.DesktopContentManagerImpl;
import consulo.ui.Component;
import consulo.wm.impl.UnifiedContentImpl;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Singleton
public class ContentFactoryImpl implements ContentFactory {
  @Nonnull
  @Override
  public ContentManager createContentManager(@Nonnull ContentUI contentUI, boolean canCloseContents, @Nonnull Project project) {
    return new DesktopContentManagerImpl(contentUI, canCloseContents, project);
  }

  @Nonnull
  @Override
  public ContentManager createContentManager(boolean canCloseContents, @Nonnull Project project) {
    return createContentManager(new TabbedPaneContentUI(), canCloseContents, project);
  }

  @Nonnull
  @Override
  public Content createUIContent(@Nullable Component component, String displayName, boolean isLockable) {
    return new UnifiedContentImpl(component, displayName, isLockable);
  }

  // TODO [VISTALL] AWT & Swing dependency
  // region AWT & Swing dependency
  @Nonnull
  @Override
  public ContentImpl createContent(javax.swing.JComponent component, String displayName, boolean isLockable) {
    return new ContentImpl(component, displayName, isLockable);
  }
  // endregion
}
