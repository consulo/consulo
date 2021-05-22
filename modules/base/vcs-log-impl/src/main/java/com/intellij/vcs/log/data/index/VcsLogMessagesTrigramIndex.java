/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.vcs.log.data.index;

import com.intellij.openapi.util.text.TrigramBuilder;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.StorageException;
import com.intellij.util.io.VoidDataExternalizer;
import com.intellij.vcs.log.VcsFullCommitDetails;
import com.intellij.vcs.log.impl.FatalErrorHandler;
import consulo.disposer.Disposable;
import consulo.util.collection.primitive.ints.IntSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.intellij.vcs.log.data.index.VcsLogPersistentIndex.getVersion;

public class VcsLogMessagesTrigramIndex extends VcsLogFullDetailsIndex<Void> {
  public static final String TRIGRAMS = "trigrams";

  public VcsLogMessagesTrigramIndex(@Nonnull String logId,
                                    @Nonnull FatalErrorHandler fatalErrorHandler,
                                    @Nonnull Disposable disposableParent) throws IOException {
    super(logId, TRIGRAMS, getVersion(), new TrigramMessageIndexer(), VoidDataExternalizer.INSTANCE,
          fatalErrorHandler, disposableParent);
  }

  @Nullable
  public IntSet getCommitsForSubstring(@Nonnull String string) throws StorageException {
    MyTrigramProcessor trigramProcessor = new MyTrigramProcessor();
    TrigramBuilder.processTrigrams(string, trigramProcessor);

    if (trigramProcessor.map.isEmpty()) return null;

    return getCommitsWithAllKeys(trigramProcessor.map.keySet());
  }

  public static class TrigramMessageIndexer implements DataIndexer<Integer, Void, VcsFullCommitDetails> {
    @Nonnull
    @Override
    public Map<Integer, Void> map(@Nonnull VcsFullCommitDetails inputData) {
      MyTrigramProcessor trigramProcessor = new MyTrigramProcessor();
      TrigramBuilder.processTrigrams(inputData.getFullMessage(), trigramProcessor);

      return trigramProcessor.map;
    }
  }

  private static class MyTrigramProcessor extends TrigramBuilder.TrigramProcessor {
    Map<Integer, Void> map;

    @Override
    public boolean consumeTrigramsCount(int count) {
      map = new HashMap<>(count);
      return true;
    }

    @Override
    public boolean test(int value) {
      map.put(value, null);
      return true;
    }
  }
}
