/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.ide;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.ui.ex.awt.accessibility.ScreenReader;
import consulo.disposer.Disposable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kava.beans.PropertyChangeListener;

import jakarta.annotation.Nonnull;

/**
 * Keep {@link ScreenReader#isActive} in sync with {@link GeneralSettings#isSupportScreenReaders}
 */
@Singleton
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
@ServiceImpl
public class ScreenReaderSupportHandler implements Disposable {
  private final GeneralSettings mySettings;
  private final PropertyChangeListener myGeneralSettingsListener;

  @Inject
  public ScreenReaderSupportHandler(@Nonnull GeneralSettings generalSettings) {
    mySettings = generalSettings;
    myGeneralSettingsListener = e -> {
      if (GeneralSettings.PROP_SUPPORT_SCREEN_READERS.equals(e.getPropertyName())) {
        ScreenReader.setActive((Boolean)e.getNewValue());
      }
    };
    mySettings.addPropertyChangeListener(myGeneralSettingsListener);

    ScreenReader.setActive(mySettings.isSupportScreenReaders());
  }

  @Override
  public void dispose() {
    mySettings.removePropertyChangeListener(myGeneralSettingsListener);
  }
}
