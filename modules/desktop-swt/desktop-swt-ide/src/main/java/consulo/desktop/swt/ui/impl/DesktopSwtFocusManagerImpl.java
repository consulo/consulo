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
package consulo.desktop.swt.ui.impl;

import consulo.disposer.Disposable;
import consulo.ui.FocusManager;
import consulo.ui.event.GlobalFocusListener;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 18/12/2021
 */
public class DesktopSwtFocusManagerImpl implements FocusManager {
  public static final DesktopSwtFocusManagerImpl INSTANCE = new DesktopSwtFocusManagerImpl();

  @Nonnull
  @Override
  public Disposable addListener(@Nonnull GlobalFocusListener listener) {
    return () -> {
    };
  }
}
