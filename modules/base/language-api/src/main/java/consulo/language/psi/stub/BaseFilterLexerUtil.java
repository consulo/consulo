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
package consulo.language.psi.stub;

import consulo.language.lexer.Lexer;
import consulo.language.psi.search.IndexPattern;
import consulo.language.psi.stub.todo.TodoIndexEntry;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.LocalFileSystem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BaseFilterLexerUtil {
  private static final Key<ScanContent> scanContentKey = Key.create("id.todo.scan.content");

  public static ScanContent scanContent(FileContent content, IdAndToDoScannerBasedOnFilterLexer indexer) {
    ScanContent data = content.getUserData(scanContentKey);
    if (data != null) {
      content.putUserData(scanContentKey, null);
      return data;
    }

    boolean needTodo = content.getFile().getFileSystem() instanceof LocalFileSystem;
    boolean needIdIndex = IdTableBuilding.getFileTypeIndexer(content.getFileType()) instanceof LexerBasedIdIndexer;

    IdDataConsumer consumer = needIdIndex? new IdDataConsumer():null;
    OccurrenceConsumer todoOccurrenceConsumer = new OccurrenceConsumer(consumer, needTodo);
    Lexer filterLexer = indexer.createLexer(todoOccurrenceConsumer);
    filterLexer.start(content.getContentAsText());

    while (filterLexer.getTokenType() != null) filterLexer.advance();

    Map<TodoIndexEntry,Integer> todoMap = null;
    if (needTodo) {
      for (IndexPattern indexPattern : IndexPatternUtil.getIndexPatterns()) {
          int count = todoOccurrenceConsumer.getOccurrenceCount(indexPattern);
          if (count > 0) {
            if (todoMap == null) todoMap = new HashMap<TodoIndexEntry, Integer>();
            todoMap.put(new TodoIndexEntry(indexPattern.getPatternString(), indexPattern.isCaseSensitive()), count);
          }
        }
    }

    data = new ScanContent(
      consumer != null? consumer.getResult():Collections.<IdIndexEntry, Integer>emptyMap(),
      todoMap != null ? todoMap: Collections.<TodoIndexEntry,Integer>emptyMap()
    );
    if (needIdIndex && needTodo) content.putUserData(scanContentKey, data);
    return data;
  }

  public static class ScanContent {
    public final Map<IdIndexEntry, Integer> idMap;
    public final Map<TodoIndexEntry,Integer> todoMap;

    public ScanContent(Map<IdIndexEntry, Integer> _idMap, Map<TodoIndexEntry, Integer> _todoMap) {
      idMap = _idMap;
      todoMap = _todoMap;
    }
  }
}
