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
package consulo.desktop.util.awt;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SystemProperties;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * @author VISTALL
 * @since 2019-02-24
 */
public class StringHtmlUtil {
  private static final Logger LOG = Logger.getInstance(StringHtmlUtil.class);

  private static class MyHtml2Text extends HTMLEditorKit.ParserCallback {
    @Nonnull
    private final StringBuilder myBuffer = new StringBuilder();

    public void parse(Reader in) throws IOException {
      myBuffer.setLength(0);
      new ParserDelegator().parse(in, this, Boolean.TRUE);
    }

    @Override
    public void handleText(char[] text, int pos) {
      myBuffer.append(text);
    }

    @Override
    public void handleStartTag(HTML.Tag tag, MutableAttributeSet set, int i) {
      handleTag(tag);
    }

    @Override
    public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet set, int i) {
      handleTag(tag);
    }

    private void handleTag(HTML.Tag tag) {
      if (tag.breaksFlow() && myBuffer.length() > 0) {
        myBuffer.append(SystemProperties.getLineSeparator());
      }
    }

    public String getText() {
      return myBuffer.toString();
    }
  }

  private static final MyHtml2Text html2TextParser = new MyHtml2Text();

  public static String removeHtmlTags(@Nullable String htmlString) {
    if (StringUtil.isEmpty(htmlString)) return htmlString;
    try {
      html2TextParser.parse(new StringReader(htmlString));
    }
    catch (IOException e) {
      LOG.error(e);
    }
    return html2TextParser.getText();
  }
}
