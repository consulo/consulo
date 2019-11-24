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
package consulo.logging.impl.log4j2.attachment;

import com.intellij.util.ExceptionUtil;
import com.intellij.util.PathUtilRt;
import consulo.logging.attachment.Attachment;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author VISTALL
 * @since 2019-08-10
 */
public class AttachmentImpl implements Attachment {
  private final String myPath;
  private final byte[] myBytes;
  private boolean myIncluded = true;
  private final String myDisplayText;

  public AttachmentImpl(String path, String content) {
    myPath = path;
    myDisplayText = content;
    myBytes = content.getBytes(StandardCharsets.UTF_8);
  }

  public AttachmentImpl(String path, byte[] bytes, String displayText) {
    myPath = path;
    myBytes = bytes;
    myDisplayText = displayText;
  }

  public AttachmentImpl(@Nonnull String name, @Nonnull Throwable throwable) {
    this(name + ".trace", ExceptionUtil.getThrowableText(throwable));
  }

  @Override
  public String getDisplayText() {
    return myDisplayText;
  }

  @Override
  public String getPath() {
    return myPath;
  }

  @Override
  public String getName() {
    return PathUtilRt.getFileName(myPath);
  }

  @Override
  public String getEncodedBytes() {
    return Base64.getEncoder().encodeToString(myBytes);
  }

  @Override
  public boolean isIncluded() {
    return myIncluded;
  }

  @Override
  public void setIncluded(Boolean included) {
    myIncluded = included;
  }
}
