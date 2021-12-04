/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.mock;

import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class MockFileTypeManager extends FileTypeManagerEx {

  private FileType fileType;

  public MockFileTypeManager(FileType fileType) {
    this.fileType = fileType;
  }

  @Override
  public void registerFileType(FileType fileType) {
  }

  @Override
  public void unregisterFileType(FileType fileType) {
  }

  public void save() {
  }

  @Override
  @Nonnull
  public String getExtension(String fileName) {
    return "";
  }

  @Override
  public void fireFileTypesChanged() {
  }

  @Override
  @Nonnull
  public FileType getFileTypeByFileName(@Nonnull String fileName) {
    return fileType;
  }

  @Nonnull
  @Override
  public FileType getFileTypeByFileName(@Nonnull @NonNls CharSequence fileName) {
    return null;
  }

  @Override
  @Nonnull
  public FileType getFileTypeByFile(@Nonnull VirtualFile file) {
    return fileType;
  }

  @Override
  @Nonnull
  public FileType getFileTypeByExtension(@Nonnull String extension) {
    return fileType;
  }

  @Override
  @Nonnull
  public FileType[] getRegisteredFileTypes() {
    return FileType.EMPTY_ARRAY;
  }

  @Override
  public boolean isFileIgnored(@NonNls @Nonnull VirtualFile file) {
    return false;
  }

  @Override
  public void registerFileType(@Nonnull FileType type, @Nonnull List<? extends FileNameMatcher> defaultAssociations) {

  }

  @Override
  public boolean isFileIgnored(@NonNls @Nonnull String name) {
    return false;
  }

  @Override
  @Nonnull
  public String[] getAssociatedExtensions(@Nonnull FileType type) {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @Override
  public void fireBeforeFileTypesChanged() {
  }

  @Override
  public void addFileTypeListener(@Nonnull FileTypeListener listener) {
  }

  @Override
  public void removeFileTypeListener(@Nonnull FileTypeListener listener) {
  }

  @Override
  public FileType getKnownFileTypeOrAssociate(@Nonnull VirtualFile file) {
    return file.getFileType();
  }

  @Override
  public FileType getKnownFileTypeOrAssociate(@Nonnull VirtualFile file, @Nonnull Project project) {
    return getKnownFileTypeOrAssociate(file);
  }

  @Override
  @Nonnull
  public List<FileNameMatcher> getAssociations(@Nonnull FileType type) {
    return Collections.emptyList();
  }

  @Override
  public void associate(@Nonnull FileType type, @Nonnull FileNameMatcher matcher) {
  }

  @Override
  public void removeAssociation(@Nonnull FileType type, @Nonnull FileNameMatcher matcher) {
  }

  @Override
  @Nonnull
  public FileType getStdFileType(@Nonnull @NonNls final String fileTypeName) {
    if ("ARCHIVE".equals(fileTypeName) || "CLASS".equals(fileTypeName)) return UnknownFileType.INSTANCE;
    if ("PLAIN_TEXT".equals(fileTypeName)) return PlainTextFileType.INSTANCE;
    if ("JAVA".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.ide.highlighter.JavaFileType", fileTypeName);
    if ("XML".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.ide.highlighter.XmlFileType", fileTypeName);
    if ("DTD".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.ide.highlighter.DTDFileType", fileTypeName);
    if ("JSP".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.ide.highlighter.NewJspFileType", fileTypeName);
    if ("JSPX".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.ide.highlighter.JspxFileType", fileTypeName);
    if ("HTML".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.ide.highlighter.HtmlFileType", fileTypeName);
    if ("XHTML".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.ide.highlighter.XHtmlFileType", fileTypeName);
    if ("JavaScript".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.lang.javascript.JavaScriptFileType", fileTypeName);
    if ("Properties".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.lang.properties.PropertiesFileType", fileTypeName);
    return new MockLanguageFileType(PlainTextLanguage.INSTANCE, fileTypeName.toLowerCase());
  }

  private static FileType loadFileTypeSafe(final String className, String fileTypeName) {
    try {
      return (FileType)Class.forName(className).getField("INSTANCE").get(null);
    }
    catch (Exception e) {
      return new MockLanguageFileType(PlainTextLanguage.INSTANCE, fileTypeName.toLowerCase());
    }
  }

  @Override
  public boolean isFileOfType(@Nonnull VirtualFile file, @Nonnull FileType type) {
    return false;
  }

  @javax.annotation.Nullable
  @Override
  public FileType findFileTypeByName(String fileTypeName) {
    return null;
  }
}
