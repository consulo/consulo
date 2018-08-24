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
package com.intellij.psi.search;

import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Dmitry Avdeev
 */
public class FileTypeIndex extends ScalarIndexExtension<FileType>
        implements FileBasedIndex.InputFilter, KeyDescriptor<FileType>, DataIndexer<FileType, Void, FileContent> {
  private static final EnumeratorStringDescriptor ENUMERATOR_STRING_DESCRIPTOR = new EnumeratorStringDescriptor();

  @Nonnull
  public static Collection<VirtualFile> getFiles(@Nonnull FileType fileType, @Nonnull GlobalSearchScope scope) {
    return FileBasedIndex.getInstance().getContainingFiles(NAME, fileType, scope);
  }

  public static final ID<FileType, Void> NAME = ID.create("filetypes");

  private final FileTypeManager myFileTypeManager;

  @Inject
  public FileTypeIndex(FileTypeManager fileTypeRegistry) {
    myFileTypeManager = fileTypeRegistry;
  }

  @Nonnull
  @Override
  public ID<FileType, Void> getName() {
    return NAME;
  }

  @Nonnull
  @Override
  public DataIndexer<FileType, Void, FileContent> getIndexer() {
    return this;
  }

  @Nonnull
  @Override
  public KeyDescriptor<FileType> getKeyDescriptor() {
    return this;
  }

  @Nonnull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return this;
  }

  @Override
  public boolean dependsOnFileContent() {
    return false;
  }

  @Override
  public int getVersion() {
    FileType[] types = myFileTypeManager.getRegisteredFileTypes();
    int version = 1;
    for (FileType type : types) {
      version += type.getId().hashCode();
    }

    version *= 31;
    for (FileTypeRegistry.FileTypeDetector detector : Extensions.getExtensions(FileTypeRegistry.FileTypeDetector.EP_NAME)) {
      version += detector.getVersion();
    }
    return version;
  }

  @Override
  public boolean acceptInput(@Nullable Project project, @Nonnull VirtualFile file) {
    return !file.isDirectory();
  }

  @Override
  public void save(@Nonnull DataOutput out, FileType value) throws IOException {
    ENUMERATOR_STRING_DESCRIPTOR.save(out, value.getName());
  }

  @Override
  public FileType read(@Nonnull DataInput in) throws IOException {
    String read = ENUMERATOR_STRING_DESCRIPTOR.read(in);
    return myFileTypeManager.findFileTypeByName(read);
  }

  @Override
  public int getHashCode(FileType value) {
    return value.getName().hashCode();
  }

  @Override
  public boolean isEqual(FileType val1, FileType val2) {
    return Comparing.equal(val1, val2);
  }

  @Nonnull
  @Override
  public Map<FileType, Void> map(@Nonnull FileContent inputData) {
    return Collections.singletonMap(inputData.getFileType(), null);
  }

  public static boolean containsFileOfType(@Nonnull FileType type, @Nonnull GlobalSearchScope scope) {
    return !FileBasedIndex.getInstance().processValues(NAME, type, null, new FileBasedIndex.ValueProcessor<Void>() {
      @Override
      public boolean process(VirtualFile file, Void value) {
        return false;
      }
    }, scope);
  }
}
