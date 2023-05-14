/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.impl.diff;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.ide.impl.idea.diff.impl.DiffSettingsHolder;
import consulo.ide.impl.idea.diff.tools.util.base.TextDiffSettingsHolder;
import consulo.configurable.Configurable;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import consulo.util.collection.ArrayUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 24/09/2021
 */
@ExtensionImpl
public class DiffSettingsConfigurable extends SimpleConfigurableByProperties implements Configurable, ApplicationConfigurable {
  @RequiredUIAccess
  @Nonnull
  @Override
  protected Component createLayout(PropertyBuilder propertyBuilder, @Nonnull Disposable uiDisposable) {
    TextDiffSettingsHolder.TextDiffSettings textDiffSettings = TextDiffSettingsHolder.TextDiffSettings.getSettings();
    DiffSettingsHolder.DiffSettings diffSettings = DiffSettingsHolder.DiffSettings.getSettings();

    VerticalLayout rootLayout = VerticalLayout.create();

    VerticalLayout diffLayout = VerticalLayout.create();

    List<Integer> modes = new ArrayList<>();
    for (int mode : TextDiffSettingsHolder.CONTEXT_RANGE_MODES) {
      modes.add(mode);
    }

    ComboBox<Integer> contextRangeBox = ComboBox.create(modes);
    contextRangeBox.selectFirst();
    contextRangeBox.setTextRender(value -> {
      int index = ArrayUtil.indexOf(TextDiffSettingsHolder.CONTEXT_RANGE_MODES, value);
      return LocalizeValue.localizeTODO(TextDiffSettingsHolder.CONTEXT_RANGE_MODE_LABELS[index]);
    });

    diffLayout.add(LabeledBuilder.sided(LocalizeValue.localizeTODO("Context lines:"), contextRangeBox));
    propertyBuilder.add(contextRangeBox, textDiffSettings::getContextRange, textDiffSettings::setContextRange);

    CheckBox goToNextFileBox = CheckBox.create(LocalizeValue.localizeTODO("Go to the next file after reaching last change"));
    diffLayout.add(goToNextFileBox);
    propertyBuilder.add(goToNextFileBox, diffSettings::isGoToNextFileOnNextDifference, diffSettings::setGoToNextFileOnNextDifference);

    rootLayout.add(LabeledLayout.create(LocalizeValue.localizeTODO("Diff"), diffLayout));

    CheckBox showDiffInEditorBox = CheckBox.create(LocalizeValue.localizeTODO("Show diff in editor"));
    diffLayout.add(showDiffInEditorBox);
    propertyBuilder.add(showDiffInEditorBox, diffSettings::isShowDiffInEditor, diffSettings::setShowDiffInEditor);
    return rootLayout;
  }

  @Nonnull
  @Override
  public String getId() {
    return "diff.base";
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return "Diff & Merge";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.VCS_GROUP;
  }
}
