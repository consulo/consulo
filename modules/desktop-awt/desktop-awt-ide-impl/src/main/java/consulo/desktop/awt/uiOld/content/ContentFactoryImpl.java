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
package consulo.desktop.awt.uiOld.content;

import consulo.annotation.component.ServiceImpl;
import consulo.component.ComponentManager;
import consulo.ide.impl.idea.ui.content.TabbedPaneContentUI;
import consulo.ide.impl.idea.ui.content.impl.ContentImpl;
import consulo.ide.impl.idea.ui.content.impl.DesktopContentManagerImpl;
import consulo.ide.impl.idea.ui.content.impl.TabbedContentImpl;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.content.*;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

@Singleton
@ServiceImpl
public class ContentFactoryImpl implements ContentFactory {
  @Nonnull
  @Override
  public ContentManager createContentManager(@Nonnull ContentUI contentUI, boolean canCloseContents, @Nonnull ComponentManager project) {
    return new DesktopContentManagerImpl(contentUI, canCloseContents, (Project)project);
  }

  @Nonnull
  @Override
  public ContentManager createContentManager(boolean canCloseContents, @Nonnull ComponentManager project) {
    return createContentManager(new TabbedPaneContentUI(), canCloseContents, project);
  }

  @Nonnull
  @Override
  public Content createUIContent(@Nullable Component component, String displayName, boolean isLockable) {
    if (component == null) {
      return new ContentImpl(null, displayName, isLockable);
    }

    return new ContentImpl((JComponent)TargetAWT.to(component), displayName, isLockable);
  }

  // TODO [VISTALL] AWT & Swing dependency
  // region AWT & Swing dependency
  @Nonnull
  @Override
  public ContentImpl createContent(javax.swing.JComponent component, String displayName, boolean isLockable) {
    return new ContentImpl(component, displayName, isLockable);
  }

  @Nonnull
  @Override
  public TabbedContent createTabbedContent(javax.swing.JComponent component, String displayName, boolean isPinnable, String titlePrefix) {
    return new TabbedContentImpl(component, displayName, isPinnable, titlePrefix);
  }

  // endregion
}
