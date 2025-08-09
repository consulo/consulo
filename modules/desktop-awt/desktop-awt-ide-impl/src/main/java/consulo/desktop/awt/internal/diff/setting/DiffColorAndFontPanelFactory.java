/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.diff.setting;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.localize.ApplicationLocalize;
import consulo.ide.impl.idea.application.options.colors.*;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class DiffColorAndFontPanelFactory implements ColorAndFontPanelFactory {
  @Override
  @Nonnull
  public NewColorAndFontPanel createPanel(@Nonnull ColorAndFontOptions options) {
    SchemesPanel schemesPanel = new SchemesPanel(options);

    CompositeColorDescriptionPanel descriptionPanel = new CompositeColorDescriptionPanel();
    descriptionPanel.addDescriptionPanel(new ColorAndFontDescriptionPanel(), it -> it instanceof ColorAndFontDescription);
    descriptionPanel.addDescriptionPanel(new DiffColorDescriptionPanel(options), it -> it instanceof TextAttributesDescription);

    final OptionsPanelImpl optionsPanel = new OptionsPanelImpl(options, schemesPanel, getDiffGroup(), descriptionPanel);
    final DiffPreviewPanel previewPanel = new DiffPreviewPanel();

    schemesPanel.addListener(new ColorAndFontSettingsListener.Abstract() {
      @Override
      public void schemeChanged(@Nonnull Object source) {
        previewPanel.setColorScheme(options.getSelectedScheme());
        optionsPanel.updateOptionsList();
      }
    });

    return new NewColorAndFontPanel(schemesPanel, optionsPanel, previewPanel, getDiffGroup(), null, null);
  }

  @Override
  @Nonnull
  public LocalizeValue getPanelDisplayName() {
    return getDiffGroup();
  }

  public static LocalizeValue getDiffGroup() {
    return ApplicationLocalize.titleDiff();
  }
}
