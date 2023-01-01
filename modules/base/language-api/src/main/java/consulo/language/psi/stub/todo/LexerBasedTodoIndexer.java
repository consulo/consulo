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

package consulo.language.psi.stub.todo;

import consulo.language.psi.stub.BaseFilterLexerUtil;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.IdAndToDoScannerBasedOnFilterLexer;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * @author Eugene Zhuravlev
 *         Date: Jan 20, 2008
 */
public abstract class LexerBasedTodoIndexer implements VersionedTodoIndexer, IdAndToDoScannerBasedOnFilterLexer {
  @Override
  @Nonnull
  public Map<TodoIndexEntry, Integer> map(final FileContent inputData) {
    return BaseFilterLexerUtil.scanContent(inputData, this).todoMap;
  }
}
