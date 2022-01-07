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
package com.intellij.openapi.editor.richcopy.view;

import consulo.logging.Logger;
import com.intellij.openapi.editor.richcopy.model.SyntaxInfo;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.StringBuilderSpinAllocator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * @author Denis Zhdanov
 * @since 3/28/13 7:09 PM
 */
public abstract class AbstractSyntaxAwareReaderTransferableData extends Reader implements RawTextWithMarkup
{

  private static final Logger LOG = Logger.getInstance(AbstractSyntaxAwareReaderTransferableData.class);

  protected String myRawText;
  @Nonnull
  protected final SyntaxInfo mySyntaxInfo;
  @Nonnull
  private final DataFlavor myDataFlavor;

  @Nullable
  private transient Reader myDelegate;

  public AbstractSyntaxAwareReaderTransferableData(@Nonnull SyntaxInfo syntaxInfo, @Nonnull DataFlavor dataFlavor) {
    mySyntaxInfo = syntaxInfo;
    myDataFlavor = dataFlavor;
  }

  @Override
  public DataFlavor getFlavor() {
    return myDataFlavor;
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

  @Override
  public int read() throws IOException {
    return getDelegate().read();
  }

  @Override
  public int read(@Nonnull char[] cbuf, int off, int len) throws IOException {
    return getDelegate().read(cbuf, off, len);
  }

  @Override
  public void close() throws IOException {
    myDelegate = null;
  }

  @Override
  public void setRawText(String rawText) {
    myRawText = rawText;
  }

  @Nonnull
  private Reader getDelegate() {
    if (myDelegate != null) {
      return myDelegate;
    }

    int maxLength = Registry.intValue("editor.richcopy.max.size.megabytes") * FileUtilRt.MEGABYTE;
    final StringBuilder buffer = StringBuilderSpinAllocator.alloc();
    try {
      try {
        build(buffer, maxLength);
      }
      catch (Exception e) {
        LOG.error(e);
      }
      String s = buffer.toString();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Resulting text: \n" + s);
      }
      myDelegate = new StringReader(s);
      return myDelegate;
    }
    finally {
      StringBuilderSpinAllocator.dispose(buffer);
    }
  }

  protected abstract void build(@Nonnull StringBuilder holder, int maxLength);
}
