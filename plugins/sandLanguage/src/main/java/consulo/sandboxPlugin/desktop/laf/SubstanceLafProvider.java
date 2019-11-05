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
package consulo.sandboxPlugin.desktop.laf;

import com.intellij.openapi.util.SystemInfo;
import consulo.desktop.impl.ui.LookAndFeelProvider;
import consulo.desktop.ui.laf.LookAndFeelInfoWithClassLoader;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2019-11-03
 */
public class SubstanceLafProvider implements LookAndFeelProvider {
  @Override
  public void register(@Nonnull Consumer<UIManager.LookAndFeelInfo> consumer) {
    if (SystemInfo.isJavaVersionAtLeast(9)) {
      addSubstanceLookAndFeel(consumer);
    }
  }

  private void addSubstanceLookAndFeel(@Nonnull Consumer<UIManager.LookAndFeelInfo> consumer) {
    // FIXME [VISTALL] issue with resource inside library and classloading
    UIManager.put("ClassLoader", getClass().getClassLoader());

    consumer.accept(LookAndFeelInfoWithClassLoader.simple("Substance - Gemini", SubstanceGeminiLookAndFeel2.class));
  }
}
