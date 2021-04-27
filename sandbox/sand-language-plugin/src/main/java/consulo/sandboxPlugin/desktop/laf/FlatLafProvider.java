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

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import consulo.desktop.impl.ui.LookAndFeelProvider;
import consulo.desktop.ui.laf.LookAndFeelInfoWithClassLoader;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2019-11-03
 */
public class FlatLafProvider implements LookAndFeelProvider {
  @Override
  public void register(@Nonnull Consumer<UIManager.LookAndFeelInfo> consumer) {
    consumer.accept(LookAndFeelInfoWithClassLoader.simple("Flat Laf - Light", FlatLightLaf.class));
    consumer.accept(LookAndFeelInfoWithClassLoader.simple("Flat Laf - Dark", FlatDarkLaf.class));
    consumer.accept(LookAndFeelInfoWithClassLoader.simple("Flat Laf - IntelliJ", FlatIntelliJLaf.class));
    consumer.accept(LookAndFeelInfoWithClassLoader.simple("Flat Laf - Darcula", FlatDarculaLaf.class));
  }
}
