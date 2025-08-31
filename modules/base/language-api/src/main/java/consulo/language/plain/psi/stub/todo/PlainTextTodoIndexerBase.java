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
package consulo.language.plain.psi.stub.todo;

import consulo.language.pattern.StringPattern;
import consulo.language.psi.search.IndexPattern;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.IndexPatternUtil;
import consulo.language.psi.stub.OccurrenceConsumer;
import consulo.language.psi.stub.todo.TodoIndexEntry;
import consulo.language.psi.stub.todo.TodoIndexer;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class PlainTextTodoIndexerBase implements TodoIndexer {
  @Override
  @Nonnull
  public Map<TodoIndexEntry, Integer> map(@Nonnull FileContent inputData) {
    String chars = inputData.getContentAsText().toString(); // matching strings is faster than HeapCharBuffer

    IndexPattern[] indexPatterns = IndexPatternUtil.getIndexPatterns();
    if (indexPatterns.length <= 0) {
      return Collections.emptyMap();
    }
    OccurrenceConsumer occurrenceConsumer = new OccurrenceConsumer(null, true);
    for (IndexPattern indexPattern : indexPatterns) {
      Pattern pattern = indexPattern.getOptimizedIndexingPattern();
      if (pattern != null) {
        Matcher matcher = pattern.matcher(StringPattern.newBombedCharSequence(chars));
        while (matcher.find()) {
          if (matcher.start() != matcher.end()) {
            occurrenceConsumer.incTodoOccurrence(indexPattern);
          }
        }
      }
    }
    Map<TodoIndexEntry, Integer> map = new HashMap<>();
    for (IndexPattern indexPattern : indexPatterns) {
      int count = occurrenceConsumer.getOccurrenceCount(indexPattern);
      if (count > 0) {
        map.put(new TodoIndexEntry(indexPattern.getPatternString(), indexPattern.isCaseSensitive()), count);
      }
    }
    return map;
  }
}
