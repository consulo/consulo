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
package consulo.ide.impl.idea.openapi.vcs.impl.projectlevelman;

import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.versionControlSystem.VcsShowConfirmationOption;
import consulo.versionControlSystem.internal.VcsShowConfirmationOptionImpl;
import consulo.versionControlSystem.internal.VcsShowOptionsSettingImpl;
import java.util.HashMap;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;

import java.util.Map;

public class ProjectLevelVcsManagerSerialization {
  @NonNls private static final String OPTIONS_SETTING = "OptionsSetting";
  @NonNls private static final String CONFIRMATIONS_SETTING = "ConfirmationsSetting";
  @NonNls private static final String VALUE_ATTTIBUTE = "value";
  @NonNls private static final String ID_ATTRIBUTE = "id";

  // read-only can be kept here
  private final Map<String, VcsShowConfirmationOption.Value> myReadValue;

  public ProjectLevelVcsManagerSerialization() {
    myReadValue = new HashMap<>();
  }

  private static VcsShowOptionsSettingImpl getOrCreateOption(Map<String, VcsShowOptionsSettingImpl> options, String actionName) {
    if (!options.containsKey(actionName)) {
      options.put(actionName, new VcsShowOptionsSettingImpl(actionName));
    }
    return options.get(actionName);
  }

  public void readExternalUtil(final Element element, final OptionsAndConfirmations optionsAndConfirmations) throws InvalidDataException {
    final Map<String, VcsShowOptionsSettingImpl> options = optionsAndConfirmations.getOptions();
    for (Element subElement : element.getChildren(OPTIONS_SETTING)) {
      final String id = subElement.getAttributeValue(ID_ATTRIBUTE);
      final String value = subElement.getAttributeValue(VALUE_ATTTIBUTE);
      if (id != null && value != null) {
        try {
          getOrCreateOption(options, id).setValue(Boolean.parseBoolean(value));
        }
        catch (Exception ignored) {
        }
      }
    }
    myReadValue.clear();
    for (Element subElement : element.getChildren(CONFIRMATIONS_SETTING)) {
      final String id = subElement.getAttributeValue(ID_ATTRIBUTE);
      final String value = subElement.getAttributeValue(VALUE_ATTTIBUTE);
      if (id != null && value != null) {
        try {
          myReadValue.put(id, VcsShowConfirmationOption.Value.fromString(value));
        }
        catch (Exception ignored) {
        }
      }
    }
  }

  public void writeExternalUtil(@Nonnull Element element, @Nonnull OptionsAndConfirmations optionsAndConfirmations) throws WriteExternalException {
    final Map<String, VcsShowOptionsSettingImpl> options = optionsAndConfirmations.getOptions();
    final Map<String, VcsShowConfirmationOptionImpl> confirmations = optionsAndConfirmations.getConfirmations();

    for (VcsShowOptionsSettingImpl setting : options.values()) {
      if (!setting.getValue()) {
        Element settingElement = new Element(OPTIONS_SETTING);
        element.addContent(settingElement);
        settingElement.setAttribute(VALUE_ATTTIBUTE, Boolean.toString(setting.getValue()));
        settingElement.setAttribute(ID_ATTRIBUTE, setting.getDisplayName());
      }
    }

    for (VcsShowConfirmationOptionImpl setting : confirmations.values()) {
      if (setting.getValue() != VcsShowConfirmationOption.Value.SHOW_CONFIRMATION) {
        final Element settingElement = new Element(CONFIRMATIONS_SETTING);
        element.addContent(settingElement);
        settingElement.setAttribute(VALUE_ATTTIBUTE, setting.getValue().toString());
        settingElement.setAttribute(ID_ATTRIBUTE, setting.getDisplayName());
      }
    }
  }

  public VcsShowConfirmationOption.Value getInitOptionValue(final String id) {
    return myReadValue.get(id);
  }
}
