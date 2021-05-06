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
package consulo.ui.desktop.internal.validableComponent;

import com.intellij.ui.DocumentAdapter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;

/**
 * @author VISTALL
 * @since 2019-11-04
 */
public abstract class DocumentSwingValidator<V, C extends JComponent> extends SwingValidableComponent<V, C> {
  protected void addDocumentListenerForValidator(Document document) {
    document.addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        myValidator.validateValue(toAWTComponent(), getPrevValue(e), true);
      }
    });
  }

  protected V getPrevValue(DocumentEvent e) {
    return getValue();
  }
}
