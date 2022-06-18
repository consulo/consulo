/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.application.Application;
import org.intellij.lang.annotations.JdkConstants;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * @author egor
 */
@Service(ComponentScope.APPLICATION)
public interface SuitableFontProvider {
  @Nonnull
  static SuitableFontProvider getInstance() {
    return Application.get().getInstance(SuitableFontProvider.class);
  }

  Font getFontAbleToDisplay(char c, int size, @JdkConstants.FontStyle int style, @Nonnull String defaultFontFamily);
}
