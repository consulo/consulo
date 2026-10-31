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
package consulo.language.editor.impl.internal.richcopy.view;

import consulo.application.util.registry.Registry;
import consulo.language.editor.impl.internal.richcopy.model.SyntaxInfo;
import consulo.logging.Logger;
import consulo.util.io.FileUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * @author Denis Zhdanov
 * @since 3/28/13 7:09 PM
 */
public abstract class AbstractSyntaxAwareReader extends Reader {
  private static final Logger LOG = Logger.getInstance(AbstractSyntaxAwareReader.class);

  protected String myRawText;
  protected final SyntaxInfo mySyntaxInfo;

  private transient @Nullable Reader myDelegate;

  public AbstractSyntaxAwareReader(SyntaxInfo syntaxInfo) {
    mySyntaxInfo = syntaxInfo;
  }

  @Override
  public int read() throws IOException {
    return getDelegate().read();
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    return getDelegate().read(cbuf, off, len);
  }

  @Override
  public void close() throws IOException {
    myDelegate = null;
  }

  public void setRawText(String rawText) {
    myRawText = rawText;
  }

  private Reader getDelegate() {
    if (myDelegate != null) {
      return myDelegate;
    }

    myDelegate = new StringReader(getBuffer().toString());
    return myDelegate;
  }

  public final CharSequence getBuffer() {
    StringBuilder buffer = new StringBuilder();
    try {
      build(buffer, Registry.intValue("editor.richcopy.max.size.megabytes") * FileUtil.MEGABYTE);
    }
    catch (Exception e) {
      LOG.error(e);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Resulting text: \n" + buffer);
    }
    return buffer;
  }

  protected abstract void build(StringBuilder holder, int maxLength);
}
