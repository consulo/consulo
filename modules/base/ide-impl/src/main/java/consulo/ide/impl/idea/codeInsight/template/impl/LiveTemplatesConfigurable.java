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

package consulo.ide.impl.idea.codeInsight.template.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.language.editor.CodeInsightBundle;
import consulo.disposer.Disposer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

@ExtensionImpl
public class LiveTemplatesConfigurable implements SearchableConfigurable, Configurable.NoScroll, ApplicationConfigurable {

  private TemplateListPanel myPanel;

  @Override
  public boolean isModified() {
    return myPanel != null && myPanel.isModified();
  }

  @Override
  public JComponent createComponent() {
    myPanel = new TemplateListPanel();
    return myPanel;
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.EDITOR_GROUP;
  }

  @Override
  public String getDisplayName() {
    return CodeInsightBundle.message("templates.settings.page.title");
  }

  @Override
  public void reset() {
    myPanel.reset();
  }

  @Override
  public void apply() throws ConfigurationException {
    myPanel.apply();
  }

  @Override
  public void disposeUIResources() {
    if (myPanel != null) {
      Disposer.dispose(myPanel);
    }
    myPanel = null;
  }

  @Override
  @Nonnull
  public String getId() {
    return "editing.templates";
  }


  public TemplateListPanel getTemplateListPanel() {
    return myPanel;
  }
}
