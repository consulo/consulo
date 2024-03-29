/*
 * Copyright 2013-2019 consulo.io
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
package consulo.desktop.awt.action;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.ide.impl.actions.AboutManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.Window;
import consulo.util.lang.SemVer;
import jakarta.inject.Singleton;

import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-15
 */
@Singleton
@ServiceImpl
public class DesktopAboutManager implements AboutManager {
  @RequiredUIAccess
  @Override
  public void showAsync(@Nullable Window parentWindow) {
    new AboutNewDialog().showAsync();
  }
}
