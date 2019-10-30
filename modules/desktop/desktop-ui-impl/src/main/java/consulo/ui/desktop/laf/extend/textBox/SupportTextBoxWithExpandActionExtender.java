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
package consulo.ui.desktop.laf.extend.textBox;

import com.intellij.ui.components.fields.ExpandableSupport;
import com.intellij.util.Function;

import javax.annotation.Nonnull;
import javax.swing.text.JTextComponent;

/**
 * @author VISTALL
 * @since 2019-04-26
 */
public class SupportTextBoxWithExpandActionExtender {
  public static final SupportTextBoxWithExpandActionExtender INSTANCE = new SupportTextBoxWithExpandActionExtender();
  @Nonnull
  public <T extends JTextComponent> ExpandableSupport<T> createExpandableSupport(@Nonnull T component, Function<? super String, String> onShow, Function<? super String, String> onHide) {
    return new ConsuloExpandableSupport<>(component, onShow, onHide);
  }
}
