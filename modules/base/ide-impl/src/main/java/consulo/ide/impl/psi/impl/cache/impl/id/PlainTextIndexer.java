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
package consulo.ide.impl.psi.impl.cache.impl.id;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.stub.*;
import consulo.language.plain.PlainTextFileType;
import consulo.language.psi.search.UsageSearchContext;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import java.util.Map;

@ExtensionImpl
public class PlainTextIndexer implements IdIndexer {
  @Override
  @Nonnull
  public Map<IdIndexEntry, Integer> map(@Nonnull final FileContent inputData) {
    final IdDataConsumer consumer = new IdDataConsumer();
    final CharSequence chars = inputData.getContentAsText();
    IdTableBuilding.scanWords((chars11, charsArray, start, end) -> {
      if (charsArray != null) {
        consumer.addOccurrence(charsArray, start, end, (int)UsageSearchContext.IN_PLAIN_TEXT);
      }
      else {
        consumer.addOccurrence(chars11, start, end, (int)UsageSearchContext.IN_PLAIN_TEXT);
      }
    }, chars, 0, chars.length());
    return consumer.getResult();
  }

  @Nonnull
  @Override
  public FileType getFileType() {
    return PlainTextFileType.INSTANCE;
  }
}
