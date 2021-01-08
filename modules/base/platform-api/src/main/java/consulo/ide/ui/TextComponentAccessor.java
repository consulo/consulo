/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.ui;

import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * Text component accessor. It wraps access to the content of text component
 * and it might perform some translations between text representation and
 * component objects.
 *
 * @author dyoma
 */
public interface TextComponentAccessor<T extends Component> {
  /**
   * The accessor that gets and changes whole text
   */
  TextComponentAccessor<TextBox> TEXT_BOX_WHOLE_TEXT = new TextComponentAccessor<TextBox>() {
    @RequiredUIAccess
    @Override
    public String getValue(TextBox textBox) {
      return textBox.getValueOrError();
    }

    @RequiredUIAccess
    @Override
    public void setValue(TextBox textBox, String text, boolean fireListeners) {
      textBox.setValue(text, fireListeners);
    }
  };

  /**
   * Get text from component
   *
   * @param component a component to examine
   * @return the text (possibly adjusted)
   */
  @RequiredUIAccess
  String getValue(T component);

  /**
   * Set text to the component
   *
   * @param component the component
   * @param text      the text to set
   */
  @RequiredUIAccess
  default void setValue(T component, String text) {
    setValue(component, text, true);
  }

  /**
   * Set text to the component
   *
   * @param component the component
   * @param text      the text to set
   * @param fireListeners fire listeners
   */
  @RequiredUIAccess
  void setValue(T component, String text, boolean fireListeners);
}
