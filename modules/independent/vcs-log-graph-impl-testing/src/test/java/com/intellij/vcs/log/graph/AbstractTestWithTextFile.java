/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.vcs.log.graph;

import com.intellij.openapi.util.io.FileUtil;

import java.io.IOException;
import java.io.InputStream;

abstract class AbstractTestWithTextFile {

  protected final String myDirectory;

  protected AbstractTestWithTextFile(String directory) {
    myDirectory = "/" + directory;
  }

  protected String loadText(String filename) throws IOException {
    InputStream resourceAsStream = AbstractTestWithTextFile.class.getResourceAsStream(myDirectory + "/" + filename);
    return FileUtil.loadTextAndClose(resourceAsStream, true);
  }
}
