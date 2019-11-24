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

import consulo.logging.attachment.Attachment;
import consulo.logging.attachment.AttachmentFactory;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-08-10
 */
public class AttachmentFactoryImpl implements AttachmentFactory {
  @Nonnull
  @Override
  public Attachment create(String path, String content) {
    return new AttachmentImpl(path, content);
  }

  @Nonnull
  @Override
  public Attachment create(String path, byte[] bytes, String displayText) {
    return new AttachmentImpl(path, bytes, displayText);
  }

  @Nonnull
  @Override
  public Attachment create(@Nonnull String name, @Nonnull Throwable throwable) {
    return new AttachmentImpl(name, throwable);
  }
}
