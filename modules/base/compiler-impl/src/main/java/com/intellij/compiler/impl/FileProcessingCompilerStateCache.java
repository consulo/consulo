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

/*
 * @author: Eugene Zhuravlev
 * Date: Apr 1, 2003
 * Time: 1:17:50 PM
 */
package com.intellij.compiler.impl;

import com.intellij.openapi.compiler.ValidityState;
import com.intellij.openapi.compiler.ValidityStateFactory;
import consulo.logging.Logger;

import javax.annotation.Nullable;

import java.io.*;
import java.util.Collection;

public class FileProcessingCompilerStateCache {
  private static final Logger LOG = Logger.getInstance(FileProcessingCompilerStateCache.class);
  private final StateCache<MyState> myCache;

  public FileProcessingCompilerStateCache(File storeDirectory, final ValidityStateFactory stateFactory) throws IOException {
    myCache = new StateCache<MyState>(new File(storeDirectory, "timestamps")) {
      @Override
      public MyState read(DataInput stream) throws IOException {
        return new MyState(stream.readLong(), stateFactory.createValidityState(stream));
      }

      @Override
      public void write(MyState state, DataOutput out) throws IOException {
        out.writeLong(state.getTimestamp());
        final ValidityState extState = state.getExtState();
        if (extState != null) {
          extState.save(out);
        }
      }
    };
  }

  public void update(File file, ValidityState extState) throws IOException {
    myCache.update(file, new MyState(file.lastModified(), extState));
  }

  public void remove(File url) throws IOException {
    myCache.remove(url);
  }

  public long getTimestamp(File url) throws IOException {
    final Serializable savedState = myCache.getState(url);
    if (savedState != null) {
      LOG.assertTrue(savedState instanceof MyState);
    }
    MyState state = (MyState)savedState;
    return (state != null)? state.getTimestamp() : -1L;
  }

  public ValidityState getExtState(File url) throws IOException {
    MyState state = myCache.getState(url);
    return (state != null)? state.getExtState() : null;
  }

  public void force() {
    myCache.force();
  }

  public Collection<File> getFiles() throws IOException {
    return myCache.getFiles();
  }

  public boolean wipe() {
    return myCache.wipe();
  }

  public void close() {
    try {
      myCache.close();
    }
    catch (IOException ignored) {
      LOG.info(ignored);
    }
  }

  private static class MyState implements Serializable {
    private final long myTimestamp;
    private final ValidityState myExtState;

    public MyState(long timestamp, @Nullable ValidityState extState) {
      myTimestamp = timestamp;
      myExtState = extState;
    }

    public long getTimestamp() {
      return myTimestamp;
    }

    public @Nullable ValidityState getExtState() {
      return myExtState;
    }
  }

}
