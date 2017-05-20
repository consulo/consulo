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
package consulo.fileTypes;

import com.intellij.ide.IdeBundle;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 16:39/14.07.13
 */
public final class ZipArchiveFileType extends ArchiveFileType {
  public static final String PROTOCOL = "zip";
  public static final ArchiveFileType INSTANCE = new ZipArchiveFileType();

  @NotNull
  @Override
  public String getProtocol() {
    return PROTOCOL;
  }

  @NotNull
  @Override
  public String getDescription() {
    return IdeBundle.message("filetype.description.zip.files");
  }

  @NotNull
  @Override
  public String getId() {
    return "ZIP_ARCHIVE";
  }
}
