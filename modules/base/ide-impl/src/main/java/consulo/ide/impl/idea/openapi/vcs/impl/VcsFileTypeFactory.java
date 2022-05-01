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
package consulo.ide.impl.idea.openapi.vcs.impl;

import consulo.virtualFileSystem.fileType.FileTypeFactory;
import consulo.virtualFileSystem.fileType.FileTypeConsumer;
import consulo.ide.impl.idea.openapi.vcs.VcsConfiguration;
import consulo.ide.impl.idea.openapi.vcs.changes.patch.PatchFileType;
import javax.annotation.Nonnull;

/**
 * @author yole
 */
public class VcsFileTypeFactory extends FileTypeFactory {
  @Override
  public void createFileTypes(@Nonnull FileTypeConsumer consumer) {
    consumer.consume(PatchFileType.INSTANCE, VcsConfiguration.PATCH + ";" + VcsConfiguration.DIFF);
  }
}
