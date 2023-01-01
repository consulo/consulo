/*
 * Copyright 2013-2022 consulo.io
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
package consulo.virtualFileSystem.light;

import consulo.util.lang.LocalTimeCounter;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.Charset;

/**
 * @author VISTALL
 * @since 16-Apr-22
 */
public abstract class TextLightVirtualFileBase extends LightVirtualFileBase {
  protected CharSequence myContent = "";
  protected boolean myReadOnly;

  public TextLightVirtualFileBase(String name, FileType fileType, long modificationStamp) {
    super(name, fileType, modificationStamp);
  }

  public void setContent(Object requestor, @Nonnull CharSequence content, boolean fireEvent) {
    setContent(content);
    setModificationStamp(LocalTimeCounter.currentTime());
  }

  protected void setContent(@Nonnull CharSequence content) {
    assert !myReadOnly;
    //StringUtil.assertValidSeparators(content);
    myContent = content;
  }

  public void markReadOnly() {
    setWritable(false);
    myReadOnly = true;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return VirtualFileUtil.byteStreamSkippingBOM(contentsToByteArray(), this);
  }

  @Override
  @Nonnull
  public OutputStream getOutputStream(Object requestor, final long newModificationStamp, long newTimeStamp) throws IOException {
    return VirtualFileUtil.outputStreamAddingBOM(new ByteArrayOutputStream() {
      @Override
      public void close() {
        setModificationStamp(newModificationStamp);

        try {
          String content = toString(getCharset().name());
          setContent(content);
        }
        catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
      }
    }, this);
  }

  @Override
  @Nonnull
  public byte[] contentsToByteArray() throws IOException {
    final Charset charset = getCharset();
    final String s = getContent().toString();
    return s.getBytes(charset.name());
  }

  @Nonnull
  public CharSequence getContent() {
    return myContent;
  }
}
