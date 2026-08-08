/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.impl.wm.impl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.component.ComponentManager;
import consulo.ide.impl.idea.ui.content.TabbedPaneContentUI;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.ContentUI;
import jakarta.inject.Singleton;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 14-Oct-17
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedContentFactoryImpl implements ContentFactory {
  private static final Logger LOG = Logger.getInstance(UnifiedContentFactoryImpl.class);

  
  @Override
  public ContentManager createContentManager(ContentUI contentUI, boolean canCloseContents, ComponentManager project) {
    return new UnifiedContentManager(contentUI, canCloseContents, (Project)project);
  }

  
  @Override
  public ContentManager createContentManager(boolean canCloseContents, ComponentManager project) {
    return createContentManager(new TabbedPaneContentUI(), canCloseContents, project);
  }

  
  @Override
  public Content createUIContent(@Nullable Component component, String displayName, boolean isLockable) {
    return new UnifiedContentImpl(component, displayName, isLockable);
  }

  /**
   * A swing component cannot be shown by a frontend without an awt hierarchy. The content is built while a tool
   * window initializes, and throwing there takes the whole ui down rather than the one tool window still asking
   * for a JComponent - so an empty content is answered instead, and the rest of the ide comes up.
   */
  @Override
  public Content createContent(javax.swing.JComponent component, String displayName, boolean isLockable) {
    LOG.warn("Content of '" + displayName + "' is a swing component, which this frontend cannot show");

    return new UnifiedContentImpl(Label.create(LocalizeValue.of("Unsupported UI")), displayName, isLockable);
  }
}
