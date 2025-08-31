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
package consulo.ui.ex.awt.transferable;

import consulo.logging.Logger;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.StringReader;

public class TextTransferable implements Transferable {
  private static final Logger LOG = Logger.getInstance(TextTransferable.class);

  private final String myHtmlContent;
  private final String myPlainContent;

  private static DataFlavor html;
  private static DataFlavor[] flavors;

  private static DataFlavor[] getFlavours() {
    if (flavors == null) {
      try {
        html = new DataFlavor("text/html;class=java.lang.String");
      }
      catch (ClassNotFoundException e) {
        LOG.error(e);
        html = null;
      }

      flavors = (html == null) ? new DataFlavor[]{DataFlavor.stringFlavor, DataFlavor.plainTextFlavor} :
                new DataFlavor[]{DataFlavor.stringFlavor, DataFlavor.plainTextFlavor, html};
    }
    return flavors;
  }

  public TextTransferable(String data) {
    this(data, data);
  }

  public TextTransferable(String htmlContent, String plainContent) {
    myHtmlContent = htmlContent;
    myPlainContent = plainContent;
  }

  @Override
  public DataFlavor[] getTransferDataFlavors() {
    return getFlavours().clone();
  }

  @Override
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    for (DataFlavor f : getFlavours()) {
      if (flavor.equals(f)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
    if (flavor.equals(html)) {
      return myHtmlContent;
    }
    else if (flavor.equals(DataFlavor.plainTextFlavor)) {
      return new StringReader(myPlainContent == null ? "" : myPlainContent);
    }
    else if (flavor.equals(DataFlavor.stringFlavor)) {
      return myPlainContent;
    }
    throw new UnsupportedFlavorException(flavor);
  }
}
