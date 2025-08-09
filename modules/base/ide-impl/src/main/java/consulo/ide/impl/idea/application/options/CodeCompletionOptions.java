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

package consulo.ide.impl.idea.application.options;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.localize.ApplicationLocalize;
import consulo.configurable.*;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl
public class CodeCompletionOptions extends SimpleConfigurable<CodeCompletionPanel> implements SearchableConfigurable, ApplicationConfigurable {

  @RequiredUIAccess
  @Nonnull
  @Override
  protected CodeCompletionPanel createPanel(Disposable uiDisposable) {
    return new CodeCompletionPanel(ActionManager.getInstance());
  }

  @RequiredUIAccess
  @Override
  protected boolean isModified(@Nonnull CodeCompletionPanel component) {
    return component.isModified();
  }

  @RequiredUIAccess
  @Override
  protected void apply(@Nonnull CodeCompletionPanel component) throws ConfigurationException {
    component.apply();
  }

  @RequiredUIAccess
  @Override
  protected void reset(@Nonnull CodeCompletionPanel component) {
    component.reset();
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return ApplicationLocalize.titleCodeCompletion();
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.EDITOR_GROUP;
  }

  @Override
  @Nonnull
  public String getId() {
    return "editor.preferences.completion";
  }
}
