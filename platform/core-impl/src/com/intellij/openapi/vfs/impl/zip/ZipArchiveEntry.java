/*
 * Copyright 2013 must-be.org
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
package com.intellij.openapi.vfs.impl.zip;

import com.intellij.openapi.vfs.ArchiveEntry;

import java.util.zip.ZipEntry;

/**
 * @author VISTALL
 * @since 18:41/13.07.13
 *
 * Class extracted from JarHandler base - due common api ill used in PHP and others plugins
 */
public class ZipArchiveEntry implements ArchiveEntry {
  private final ZipEntry myEntry;

  public ZipArchiveEntry(ZipEntry entry) {
    myEntry = entry;
  }

  public ZipEntry getEntry() {
    return myEntry;
  }

  @Override
  public String getName() {
    return myEntry.getName();
  }

  @Override
  public long getSize() {
    return myEntry.getSize();
  }

  @Override
  public long getTime() {
    return myEntry.getTime();
  }

  @Override
  public boolean isDirectory() {
    return myEntry.isDirectory();
  }
}
