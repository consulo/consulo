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
package consulo.ui.internal;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.EventDispatcher;
import consulo.ui.RequiredUIAccess;
import consulo.ui.TextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * @author VISTALL
 * @since 19-Nov-16.
 */
public class DesktopTextFieldImpl extends JBTextField implements TextField, SwingWrapper {
  private static class Listener implements DocumentListener {
    private DesktopTextFieldImpl myTextField;

    public Listener(DesktopTextFieldImpl textField) {
      myTextField = textField;
    }

    @Override
    @RequiredUIAccess
    public void insertUpdate(DocumentEvent e) {
      myTextField.fireListeners();
    }

    @Override
    @RequiredUIAccess
    public void removeUpdate(DocumentEvent e) {
      myTextField.fireListeners();
    }

    @Override
    @RequiredUIAccess
    public void changedUpdate(DocumentEvent e) {
      myTextField.fireListeners();
    }
  }

  private EventDispatcher<ValueListener> myEventDispatcher = EventDispatcher.create(ValueListener.class);

  public DesktopTextFieldImpl(String text) {
    getDocument().addDocumentListener(new Listener(this));
    setValue(text);
  }

  @SuppressWarnings("unchecked")
  @RequiredUIAccess
  private void fireListeners() {
    myEventDispatcher.getMulticaster().valueChanged(new ValueEvent(this, getValue()));
  }

  @Override
  public void addValueListener(@NotNull ValueListener<String> valueListener) {
    myEventDispatcher.addListener(valueListener);
  }

  @Override
  public void removeValueListener(@NotNull ValueListener<String> valueListener) {
    myEventDispatcher.removeListener(valueListener);
  }

  @Override
  public String getValue() {
    return StringUtil.nullize(getText());
  }

  @RequiredUIAccess
  @Override
  public void setValue(String value, boolean fireEvents) {
    setText(value);
  }
}
