/*
 * Copyright 2013-2024 consulo.io
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
package consulo.virtualFileSystem.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.container.plugin.PluginManager;
import consulo.virtualFileSystem.archive.ZipArchiveFileType;
import consulo.virtualFileSystem.fileType.FileTypeConsumer;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 06/07/24
 */
@ExtensionImpl
public class DefaultFileTypeFactory extends FileTypeFactory {
  @Override
  public void createFileTypes(@Nonnull FileTypeConsumer consumer) {
    // eat jar file type, but java plugin will rewrite it
    consumer.consume(ZipArchiveFileType.INSTANCE, "zip;ear;ane;egg;jar");

    consumer.consume(UnknownFileType.INSTANCE);

    // consulo plugin
    consumer.consume(ZipArchiveFileType.INSTANCE, PluginManager.CONSULO_PLUGIN_EXTENSION);
  }
}
