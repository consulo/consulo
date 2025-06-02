/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileTypes.impl;

import consulo.application.impl.internal.util.AsyncFileServiceImpl;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileNameMatcherFactory;
import consulo.virtualFileSystem.internal.FileTypeAssocTable;

import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author peter
 */
public class IgnoredPatternSet {
  private final Set<String> myMasks = new LinkedHashSet<String>();
  private final FileTypeAssocTable<Boolean> myIgnorePatterns = new FileTypeAssocTable<Boolean>();

  Set<String> getIgnoreMasks() {
    return Collections.unmodifiableSet(myMasks);
  }

  public void setIgnoreMasks(@Nonnull Set<String> files) {
    clearPatterns();

    for (String ignoredFile : files) {
      addIgnoreMask(ignoredFile);
    }
  }

  public void setIgnoreMasks(@Nonnull String list) {
    clearPatterns();

    StringTokenizer tokenizer = new StringTokenizer(list, ";");
    while (tokenizer.hasMoreTokens()) {
      String ignoredFile = tokenizer.nextToken();
      if (ignoredFile != null) {
        addIgnoreMask(ignoredFile);
      }
    }
  }

  void addIgnoreMask(@Nonnull String ignoredFile) {
    if (myIgnorePatterns.findAssociatedFileType(ignoredFile) == null) {
      myMasks.add(ignoredFile);
      myIgnorePatterns.addAssociation(FileNameMatcherFactory.getInstance().createMatcher(ignoredFile), Boolean.TRUE);
    }
  }

  public boolean isIgnored(@Nonnull CharSequence fileName) {
    if (Objects.equals(myIgnorePatterns.findAssociatedFileType(fileName), Boolean.TRUE)) {
      return true;
    }

    //Quite a hack, but still we need to have some name, which
    //won't be caught by VFS for sure.
    return StringUtil.endsWith(fileName, AsyncFileServiceImpl.ASYNC_DELETE_EXTENSION);
  }

  void clearPatterns() {
    myMasks.clear();
    myIgnorePatterns.removeAllAssociations(Boolean.TRUE);
  }
}
