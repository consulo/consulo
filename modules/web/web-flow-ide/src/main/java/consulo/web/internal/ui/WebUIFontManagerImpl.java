/*
 * Copyright 2013-2023 consulo.io
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
package consulo.web.internal.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ui.impl.internal.UIFontManagerImpl;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 28/05/2023
 */
@Singleton
@ServiceImpl
public class WebUIFontManagerImpl extends UIFontManagerImpl {
  @Nonnull
  @Override
  protected Pair<String, Integer> resolveSystemFontData() {
    // TODO wrong
    return Pair.create("Arial", 12);
  }
}
