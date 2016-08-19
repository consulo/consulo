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

package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.projectRoots.ex.SdkRoot;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import consulo.vfs.ArchiveFileSystem;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author mike
 */
public class SimpleSdkRoot implements SdkRoot {
  @NonNls
  private static final String ATTRIBUTE_URL = "url";

  private String myUrl;
  private VirtualFile myFile;
  private final VirtualFile[] myFileArray = new VirtualFile[1];
  private boolean myInitialized = false;

  public SimpleSdkRoot(@NotNull VirtualFile file) {
    myFile = file;
    myUrl = myFile.getUrl();
  }

  public SimpleSdkRoot(@NotNull String url) {
    myUrl = url;
  }

  SimpleSdkRoot() {
  }

  public VirtualFile getFile() {
    return myFile;
  }

  @Override
  @NotNull
  public String getPresentableString() {
    String path = VirtualFileManager.extractPath(myUrl);
    if (path.endsWith(ArchiveFileSystem.ARCHIVE_SEPARATOR)) {
      path = path.substring(0, path.length() - ArchiveFileSystem.ARCHIVE_SEPARATOR.length());
    }
    return path.replace('/', File.separatorChar);
  }

  @Override
  @NotNull
  public VirtualFile[] getVirtualFiles() {
    if (!myInitialized) initialize();

    if (myFile == null) {
      return VirtualFile.EMPTY_ARRAY;
    }

    myFileArray[0] = myFile;
    return myFileArray;
  }

  @Override
  @NotNull
  public String[] getUrls() {
    return new String[]{myUrl};
  }

  @Override
  public boolean isValid() {
    if (!myInitialized) {
      initialize();
    }

    return myFile != null && myFile.isValid();
  }

  @Override
  public void update() {
    initialize();
  }

  private void initialize() {
    myInitialized = true;

    if (myFile == null || !myFile.isValid()) {
      myFile = VirtualFileManager.getInstance().findFileByUrl(myUrl);
      if (myFile != null && !myFile.isValid()) {
        myFile = null;
      }
    }
  }

  public String getUrl() {
    return myUrl;
  }

  public void readExternal(Element element) {
    myUrl = element.getAttributeValue(ATTRIBUTE_URL);
  }

  public void writeExternal(Element element) {
    if (!myInitialized) {
      initialize();
    }

    element.setAttribute(ATTRIBUTE_URL, myUrl);
  }
}
