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

package consulo.language.psi.stub;

import consulo.util.collection.primitive.ints.IntIntMap;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.lang.StringUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eugene Zhuravlev
 * @since 2008-02-06
 */
public class IdDataConsumer {
  private final IntIntMap myResult = IntMaps.newIntIntHashMap();

  public Map<IdIndexEntry, Integer> getResult() {
    Map<IdIndexEntry, Integer> result = new HashMap<>(myResult.size());
    myResult.forEach((key, value) -> result.put(new IdIndexEntry(key), value));
    return result;
  }
  
  public void addOccurrence(CharSequence charSequence, int start, int end, int occurrenceMask) {
    int hashCode = StringUtil.stringHashCode(charSequence, start, end);
    addOccurrence(hashCode,occurrenceMask);
    int hashCodeNoCase = StringUtil.stringHashCodeInsensitive(charSequence, start, end);
    if (hashCodeNoCase != hashCode) {
      addOccurrence(hashCodeNoCase,occurrenceMask);
    }
  }

  public void addOccurrence(char[] chars, int start, int end, int occurrenceMask) {
    int hashCode = StringUtil.stringHashCode(chars, start, end);
    addOccurrence(hashCode,occurrenceMask);
    
    int hashCodeNoCase = StringUtil.stringHashCodeInsensitive(chars, start, end);
    if (hashCodeNoCase != hashCode) {
      addOccurrence(hashCodeNoCase,occurrenceMask);
    }
  }

  private void addOccurrence(int hashcode, int occurrenceMask) {
    if (occurrenceMask != 0) {
      int old = myResult.getInt(hashcode);
      int v = old | occurrenceMask;
      if (v != old) {
        myResult.putInt(hashcode, v);
      }
    }
  }
}
