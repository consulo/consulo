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
package consulo.versionControlSystem.versionBrowser;

import jakarta.annotation.Nullable;
import javax.swing.*;

/**
 * @author yole
 * @since 2006-12-05
 */
public interface ChangesBrowserSettingsEditor<T extends ChangeBrowserSettings> {
  JComponent getComponent();

  T getSettings();

  void setSettings(T settings);

  @Nullable
  String validateInput();

  void updateEnabledControls();

  String getDimensionServiceKey();
}