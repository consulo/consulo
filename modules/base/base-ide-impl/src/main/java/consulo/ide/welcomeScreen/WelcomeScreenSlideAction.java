/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.welcomeScreen;

import com.intellij.openapi.actionSystem.AnAction;
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 14-Sep-16
 */
public abstract class WelcomeScreenSlideAction extends AnAction {
  @Nonnull
  public abstract JComponent createSlide(@Nonnull Disposable parentDisposable, @Nonnull WelcomeScreenSlider owner);
}
