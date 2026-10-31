/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.editor.richcopy.view;

import consulo.language.editor.impl.internal.richcopy.model.SyntaxInfo;
import consulo.language.editor.impl.internal.richcopy.view.HtmlSyntaxInfoReader;

import java.awt.datatransfer.DataFlavor;

public final class HtmlTransferableData extends HtmlSyntaxInfoReader implements RawTextWithMarkup {
  public static final DataFlavor FLAVOR = new DataFlavor("text/html; class=java.io.Reader; charset=UTF-8", "HTML text");

  public HtmlTransferableData(SyntaxInfo syntaxInfo, int tabSize) {
    super(syntaxInfo, tabSize);
  }

  @Override
  public DataFlavor getFlavor() {
    return FLAVOR;
  }

  @Override
  public int getOffsetCount() {
    return 0;
  }

  @Override
  public int getOffsets(int[] offsets, int index) {
    return index;
  }

  @Override
  public int setOffsets(int[] offsets, int index) {
    return index;
  }
}
