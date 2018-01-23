/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.rt.coverage.util;

import com.intellij.openapi.util.text.StringHash;
import gnu.trove.TLongObjectHashMap;

/**
 * @author Pavel.Sher
 */
public class StringsPool {
  private final static TLongObjectHashMap myReusableStrings = new TLongObjectHashMap(30000);
  private final static String EMPTY = "";

  public static String getFromPool(String value) {
    if (value == null) return null;
    if (value.length() == 0) return EMPTY;

    final long hash = StringHash.calc(value);
    String reused = (String) myReusableStrings.get(hash);
    if (reused != null) return reused;
    // new String() is required because value often is passed as substring which has a reference to original char array
    // see {@link String.substring(int, int} method implementation.
    //noinspection RedundantStringConstructorCall
    reused = new String(value);
    myReusableStrings.put(hash, reused);
    return reused;
  }
}
