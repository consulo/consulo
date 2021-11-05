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
package com.intellij.testFramework;

import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author cdr
 */
public class FileTreeAccessFilter implements VirtualFileFilter {
  private static final FileType CLASS = FileTypeManager.getInstance().getStdFileType("CLASS");
  private static final LanguageFileType JAVA = (LanguageFileType)FileTypeManager.getInstance().getStdFileType("JAVA");

  protected final Set<VirtualFile> myAddedClasses = new HashSet<VirtualFile>();
  private boolean myTreeAccessAllowed;

  @Override
  public boolean accept(VirtualFile file) {
    if (file instanceof VirtualFileWindow) return false;

    if (myAddedClasses.contains(file) || myTreeAccessAllowed) return false;

    FileType fileType = file.getFileType();
    return (fileType == JAVA || fileType == CLASS) && !file.getName().equals("package-info.java");
  }

  public void allowTreeAccessForFile(@Nonnull VirtualFile file) {
    myAddedClasses.add(file);
  }

  public void allowTreeAccessForAllFiles() {
    myTreeAccessAllowed = true;
  }

  public String toString() {
    return "JAVA {allowed=" + myTreeAccessAllowed + " files=" + new ArrayList<VirtualFile>(myAddedClasses) + "}";
  }
}
