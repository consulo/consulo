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
package consulo.desktop.awt.action;

import consulo.annotation.component.ServiceImpl;
import consulo.ui.Size;
import consulo.ui.ex.action.ActionButton;
import consulo.ui.ex.action.ActionButtonFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 22-Jul-22
 */
@Singleton
@ServiceImpl
public class DesktopAWTActionButtonFactory implements ActionButtonFactory {
  @Nonnull
  @Override
  public ActionButton create(@Nonnull AnAction action, Presentation presentation, String place, @Nonnull Size minimumSize) {
    return new ActionButtonImpl(action, presentation, place, minimumSize);
  }
}
