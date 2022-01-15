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
package consulo.packaging.impl.elements;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.packaging.impl.elements.ArchivePackagingElement;
import consulo.packaging.elements.ArchivePackageWriter;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author VISTALL
 * @since 16:02/18.06.13
 */
public class ZipArchivePackagingElement extends ArchivePackagingElement {
  public static class ZipArchivePackageWriter implements ArchivePackageWriter<ZipOutputStream> {
    public static final ZipArchivePackageWriter INSTANCE = new ZipArchivePackageWriter();

    @Nonnull
    @Override
    public ZipOutputStream createArchiveObject(@Nonnull File tempFile) throws IOException {
      final BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
      return new ZipOutputStream(outputStream);
    }

    @Override
    public void addDirectory(@Nonnull ZipOutputStream zipOutputStream, @Nonnull String relativePath) throws IOException {
      ZipEntry e = new ZipEntry(relativePath);
      e.setMethod(ZipEntry.STORED);
      e.setSize(0);
      e.setCrc(0);

      zipOutputStream.putNextEntry(e);
      zipOutputStream.closeEntry();
    }

    @Override
    public void addFile(@Nonnull ZipOutputStream zipOutputStream, @Nonnull InputStream stream, @Nonnull String relativePath, long fileLength, long lastModified) throws IOException {
      ZipEntry e = new ZipEntry(relativePath);
      e.setTime(lastModified);
      e.setSize(fileLength);

      zipOutputStream.putNextEntry(e);
      FileUtil.copy(stream, zipOutputStream);
      zipOutputStream.closeEntry();
    }

    @Override
    public void close(@Nonnull ZipOutputStream zipOutputStream) throws IOException {
      zipOutputStream.close();
    }
  }

  public ZipArchivePackagingElement() {
    super(ZipArchiveElementType.getInstance());
  }

  public ZipArchivePackagingElement(@Nonnull String archiveFileName) {
    super(ZipArchiveElementType.getInstance(), archiveFileName);
  }

  @Override
  public ArchivePackageWriter<?> getPackageWriter() {
    return ZipArchivePackageWriter.INSTANCE;
  }
}
