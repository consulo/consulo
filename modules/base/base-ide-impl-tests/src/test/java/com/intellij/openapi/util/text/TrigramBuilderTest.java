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
package com.intellij.openapi.util.text;

import com.intellij.openapi.util.Ref;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import junit.framework.TestCase;

import java.util.Arrays;

public class TrigramBuilderTest extends TestCase {
  public void testBuilder() {
    final Ref<Integer> trigramCountRef = new Ref<Integer>();
    final IntList list = IntLists.newArrayList();

    TrigramBuilder.processTrigrams("String$CharData", new TrigramBuilder.TrigramProcessor() {
      @Override
      public boolean test(int value) {
        list.add(value);
        return true;
      }

      @Override
      public boolean consumeTrigramsCount(int count) {
        trigramCountRef.set(count);
        return true;
      }
    });

    int[] ints = list.toArray();
    Arrays.sort(ints);

    list.clear();
    list.addAll(ints);

    Integer trigramCount = trigramCountRef.get();
    assertNotNull(trigramCount);

    int expectedTrigramCount = 13;
    assertEquals(expectedTrigramCount, (int)trigramCount);
    assertEquals(expectedTrigramCount, list.size());

    int[] expected = {buildTrigram("$Ch"), buildTrigram("arD"), buildTrigram("ata"), 6514785, 6578548, 6759523, 6840690, 6909543, 7235364, 7496801, 7498094, 7566450, 7631465, };
    for(int i = 0; i < expectedTrigramCount; ++i) assertEquals(expected[i], list.get(i));
  }

  private static int buildTrigram(String s) {
    int tc1 = StringUtil.toLowerCase(s.charAt(0));
    int tc2 = (tc1 << 8) + StringUtil.toLowerCase(s.charAt(1));
    return (tc2 << 8) + StringUtil.toLowerCase(s.charAt(2));
  }
}
