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
package consulo.ide.impl.idea.openapi.vcs.impl.projectlevelman;

import consulo.application.Application;
import consulo.ide.impl.idea.openapi.vcs.VcsShowConfirmationOptionImpl;
import consulo.ide.impl.idea.openapi.vcs.VcsShowOptionsSettingImpl;
import consulo.ide.impl.idea.util.containers.Convertor;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.VcsShowConfirmationOption;
import consulo.versionControlSystem.VcsShowSettingOption;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OptionsAndConfirmations {
  private final Map<String, VcsShowOptionsSettingImpl> myOptions;
  private final Map<String, VcsShowConfirmationOptionImpl> myConfirmations;

  public OptionsAndConfirmations() {
    myOptions = new LinkedHashMap<String, VcsShowOptionsSettingImpl>();
    myConfirmations = new LinkedHashMap<String, VcsShowConfirmationOptionImpl>();
  }

  public void init(final Convertor<String, VcsShowConfirmationOption.Value> initOptions) {
    createSettingFor(VcsConfiguration.StandardOption.ADD);
    createSettingFor(VcsConfiguration.StandardOption.REMOVE);
    createSettingFor(VcsConfiguration.StandardOption.CHECKOUT);
    createSettingFor(VcsConfiguration.StandardOption.UPDATE);
    createSettingFor(VcsConfiguration.StandardOption.STATUS);
    createSettingFor(VcsConfiguration.StandardOption.EDIT);

    myConfirmations.put(
      VcsConfiguration.StandardConfirmation.ADD.getId(),
      new VcsShowConfirmationOptionImpl(
        VcsConfiguration.StandardConfirmation.ADD.getId(),
        VcsLocalize.labelTextWhenFilesCreatedWithIdea(Application.get().getName()).get(),
        VcsLocalize.radioAfterCreationDoNotAdd().get(),
        VcsLocalize.radioAfterCreationShowOptions().get(),
        VcsLocalize.radioAfterCreationAddSilently().get()
      )
    );

    myConfirmations.put(
      VcsConfiguration.StandardConfirmation.REMOVE.getId(),
      new VcsShowConfirmationOptionImpl(
        VcsConfiguration.StandardConfirmation.REMOVE.getId(),
        VcsLocalize.labelTextWhenFilesAreDeletedWithIdea(Application.get().getName()).get(),
        VcsLocalize.radioAfterDeletionDoNotRemove().get(),
        VcsLocalize.radioAfterDeletionShowOptions().get(),
        VcsLocalize.radioAfterDeletionRemoveSilently().get()
      )
    );

    restoreReadConfirm(VcsConfiguration.StandardConfirmation.ADD, initOptions);
    restoreReadConfirm(VcsConfiguration.StandardConfirmation.REMOVE, initOptions);
  }

  private void restoreReadConfirm(final VcsConfiguration.StandardConfirmation confirm,
                                  final Convertor<String, VcsShowConfirmationOption.Value> initOptions) {
    final VcsShowConfirmationOption.Value initValue = initOptions.convert(confirm.getId());
    if (initValue != null) {
      getConfirmation(confirm).setValue(initValue);
    }
  }

  @Nonnull
  public VcsShowConfirmationOptionImpl getConfirmation(VcsConfiguration.StandardConfirmation option) {
    return myConfirmations.get(option.getId());
  }

  private void createSettingFor(final VcsConfiguration.StandardOption option) {
    if (!myOptions.containsKey(option.getId())) {
      myOptions.put(option.getId(), new VcsShowOptionsSettingImpl(option));
    }
  }

  @Nonnull
  public VcsShowSettingOption getOptions(VcsConfiguration.StandardOption option) {
    return myOptions.get(option.getId());
  }

  public List<VcsShowOptionsSettingImpl> getAllOptions() {
    return new ArrayList<>(myOptions.values());
  }

  public List<VcsShowConfirmationOptionImpl> getAllConfirmations() {
    return new ArrayList<>(myConfirmations.values());
  }

  @Nonnull
  public VcsShowSettingOption getOrCreateCustomOption(@Nonnull String vcsActionName, @Nonnull AbstractVcs vcs) {
    final VcsShowOptionsSettingImpl option = getOrCreateOption(vcsActionName);
    option.addApplicableVcs(vcs);
    return option;
  }

  private VcsShowOptionsSettingImpl getOrCreateOption(String actionName) {
    if (!myOptions.containsKey(actionName)) {
      myOptions.put(actionName, new VcsShowOptionsSettingImpl(actionName));
    }
    return myOptions.get(actionName);
  }

  // open for serialization purposes
  Map<String, VcsShowOptionsSettingImpl> getOptions() {
    return myOptions;
  }

  // open for serialization purposes
  Map<String, VcsShowConfirmationOptionImpl> getConfirmations() {
    return myConfirmations;
  }
}
