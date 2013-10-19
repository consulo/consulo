/*
 * Copyright 2013 Consulo.org
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
package com.intellij.compiler.classParsing;

import com.intellij.compiler.make.CacheCorruptedException;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author VISTALL
 * @since 19:06/19.10.13
 */
public class SymbolTableDummy {

  private TIntObjectHashMap<String> myIntToString = new TIntObjectHashMap<String>();
  private TObjectIntHashMap<String> myStringToInt = new TObjectIntHashMap<String>();

  public SymbolTableDummy() throws CacheCorruptedException {

  }

  public synchronized int getId(@NotNull String symbol) throws CacheCorruptedException {
    if (symbol.length() == 0) {
      return -1;
    }
    try {
      int i = myStringToInt.get(symbol);
      if(i == 0) {
        int value = myStringToInt.size() + 1;

        myStringToInt.put(symbol, value);
        myIntToString.put(value, symbol);
        return value;
      }
      return i;
    }
    catch (RuntimeException e) {
      if (e.getCause() instanceof IOException) {
        throw new CacheCorruptedException(e.getCause());
      }
      throw e;
    }
  }

  public synchronized String getSymbol(int id) throws CacheCorruptedException {
    if (id == -1) {
      return "";
    }
    try {
      return myIntToString.get(id);
    }
    catch (RuntimeException e) {
      if (e.getCause() instanceof IOException) {
        throw new CacheCorruptedException(e.getCause());
      }
      throw e;
    }
  }

  public synchronized void dispose() throws CacheCorruptedException {
    myIntToString.clear();
    myStringToInt.clear();
  }
}