/*
 * Copyright 2013-2022 consulo.io
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
package consulo.desktop.awt.application.ui.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;
import consulo.ui.ex.awt.ComponentTreeEventDispatcher;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 21-Feb-22
 */
@Singleton
@ServiceImpl
public class DesktopAWTUISettings extends UISettings {
  private final ComponentTreeEventDispatcher<UISettingsListener> myDispatcher = ComponentTreeEventDispatcher.create(UISettingsListener.class);

  @Override
  protected void notifyDispatcher() {
    myDispatcher.getMulticaster().uiSettingsChanged(this);
  }
}
