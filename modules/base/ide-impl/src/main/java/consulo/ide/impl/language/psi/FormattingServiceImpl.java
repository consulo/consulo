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
package consulo.ide.impl.language.psi;

import com.intellij.psi.codeStyle.CodeStyleManager;
import consulo.language.impl.psi.internal.FormattingService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 17-Feb-22
 */
@Singleton
public class FormattingServiceImpl implements FormattingService {
  private final CodeStyleManager myCodeStyleManager;

  @Inject
  public FormattingServiceImpl(CodeStyleManager codeStyleManager) {
    myCodeStyleManager = codeStyleManager;
  }

  @Override
  public void performActionWithFormatterDisabled(Runnable runnable) {
    myCodeStyleManager.performActionWithFormatterDisabled(runnable);
  }
}
