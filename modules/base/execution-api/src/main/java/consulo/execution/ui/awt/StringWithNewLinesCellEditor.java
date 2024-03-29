/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.execution.ui.awt;

import consulo.ui.ex.awt.JBTextField;

import javax.swing.*;
import javax.swing.text.Document;

public class StringWithNewLinesCellEditor extends DefaultCellEditor {

  public StringWithNewLinesCellEditor() {
    super(new JBTextField() {
      @Override
      public void setDocument(Document doc) {
        super.setDocument(doc);
        doc.putProperty("filterNewlines", Boolean.FALSE);
      }
    });
  }
}
