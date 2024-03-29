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
package consulo.ide.impl.idea.util.ui;

import consulo.ui.ex.awt.BrowserHyperlinkListener;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.StringHtmlUtil;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import java.io.IOException;
import java.io.StringWriter;

public class HtmlPanel extends JEditorPane implements HyperlinkListener {
  public HtmlPanel() {
    super(UIUtil.HTML_MIME, "");
    setEditable(false);
    setOpaque(false);
    putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    addHyperlinkListener(this);
  }

  @Override
  public void hyperlinkUpdate(HyperlinkEvent e) {
    BrowserHyperlinkListener.INSTANCE.hyperlinkUpdate(e);
  }

  @Override
  public String getSelectedText() {
    Document doc = getDocument();
    int start = getSelectionStart();
    int end = getSelectionEnd();

    try {
      Position p0 = doc.createPosition(start);
      Position p1 = doc.createPosition(end);
      StringWriter sw = new StringWriter(p1.getOffset() - p0.getOffset());
      getEditorKit().write(sw, doc, p0.getOffset(), p1.getOffset() - p0.getOffset());

      return StringHtmlUtil.removeHtmlTags(sw.toString());
    }
    catch (BadLocationException | IOException ignored) {
    }
    return super.getSelectedText();
  }
}
