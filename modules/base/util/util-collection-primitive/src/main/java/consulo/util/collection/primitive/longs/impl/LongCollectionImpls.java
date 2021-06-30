/*
 * Copyright 2013-2021 consulo.io
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
package consulo.util.collection.primitive.longs.impl;

import consulo.util.collection.primitive.longs.LongCollection;

import java.util.PrimitiveIterator;

/**
 * @author VISTALL
 * @since 30/06/2021
 */
public class LongCollectionImpls {
  // see Arrays.hashCode
  public static int hashCode(LongCollection collection) {
    PrimitiveIterator.OfLong iterator = collection.iterator();

    int result = 1;
    while (iterator.hasNext()) {
      long element = iterator.nextLong();

      int elementHash = (int)(element ^ (element >>> 32));
      result = 31 * result + elementHash;
    }
    return result;
  }
}
