// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.file.light;

import consulo.language.Language;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author gregsh
 */
public class ReadOnlyLightVirtualFile extends LightVirtualFile {
  public ReadOnlyLightVirtualFile(String name, Language language, CharSequence text) {
    super(name, language, text);
    super.setWritable(false);
  }

  @Override
  public final void setContent(Object requestor, CharSequence content, boolean fireEvent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final void setWritable(boolean writable) {
    if (writable) throw new UnsupportedOperationException();
  }

  
  @Override
  public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
    throw new UnsupportedOperationException();
  }
}
