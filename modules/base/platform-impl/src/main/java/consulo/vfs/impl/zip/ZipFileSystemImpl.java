/*
 * Copyright 2013-2016 consulo.io
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
package consulo.vfs.impl.zip;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.fileTypes.ZipArchiveFileType;
import consulo.vfs.impl.archive.ArchiveFile;
import consulo.vfs.impl.archive.ArchiveFileSystemBase;
import javax.annotation.Nonnull;

import java.io.IOException;

/**
 * @author VISTALL
 * @since 16:43/14.07.13
 */
public class ZipFileSystemImpl extends ArchiveFileSystemBase {
  public ZipFileSystemImpl() {
    super(ZipArchiveFileType.PROTOCOL);
  }

  @Override
  protected boolean isCorrectFileType(@Nonnull VirtualFile local) {
    // special hack for jar files. eat them even when jar file type is register not as zip file system
    return super.isCorrectFileType(local) || Comparing.equal(local.getExtension(), "jar");
  }

  @Nonnull
  @Override
  public ArchiveFile createArchiveFile(@Nonnull String filePath) throws IOException {
    return new ZipArchiveFile(filePath);
  }
}
