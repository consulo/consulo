/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.fileTypes.impl;

import consulo.fileTypes.ZipArchiveFileType;
import com.intellij.openapi.fileTypes.*;
import javax.annotation.Nonnull;

/**
 * @author yole
 */
public class PlatformFileTypeFactory extends FileTypeFactory {
  @Override
  public void createFileTypes(@Nonnull final FileTypeConsumer consumer) {
    // eat jar file type, but java plugin will rewrite it
    consumer.consume(ZipArchiveFileType.INSTANCE, "zip;ear;ane;egg;jar");
    consumer.consume(PlainTextFileType.INSTANCE, "txt;sh;bat;cmd;policy;log;cgi;MF;jad;jam;htaccess");
    consumer.consume(NativeFileType.INSTANCE, "doc;docx;xls;xlsx;ppt;pptx;mdb;vsd;pdf;hlp;chm;odt");
    consumer.consume(UnknownFileType.INSTANCE);
  }
}
