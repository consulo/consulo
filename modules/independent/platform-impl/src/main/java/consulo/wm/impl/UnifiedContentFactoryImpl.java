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
package consulo.wm.impl;

import com.intellij.openapi.project.Project;
import com.intellij.ui.content.*;
import consulo.ui.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 14-Oct-17
 */
public class UnifiedContentFactoryImpl implements ContentFactory {
  @NotNull
  @Override
  public ContentManager createContentManager(@NotNull ContentUI contentUI, boolean canCloseContents, @NotNull Project project) {
    return new UnifiedContentManager(contentUI, canCloseContents, project);
  }

  @NotNull
  @Override
  public ContentManager createContentManager(boolean canCloseContents, @NotNull Project project) {
    return createContentManager(new TabbedPaneContentUI(), canCloseContents, project);
  }

  @NotNull
  @Override
  public Content createUIContent(@Nullable Component component, String displayName, boolean isLockable) {
    return new UnifiedContentImpl(component, displayName, isLockable);
  }
}
