/*
 * Copyright 2013-2021 consulo.io
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
package consulo.util.nodep.classloader;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author VISTALL
 * @since 09/01/2021
 */
class ZipEntryNameIterator implements Iterator<String> {
  private final Enumeration<? extends ZipEntry> myEntries;

  ZipEntryNameIterator(ZipFile zipFile) {
    myEntries = zipFile.entries();
  }

  @Override
  public boolean hasNext() {
    return myEntries.hasMoreElements();
  }

  @Override
  public String next() {
    return myEntries.nextElement().getName();
  }
}
