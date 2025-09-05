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
package consulo.versionControlSystem.impl.internal.util;

import consulo.util.collection.SmartList;
import consulo.versionControlSystem.VcsException;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author irengrig
 * @since 2011-02-25
 */
public abstract class VcsCatchingRunnable implements Runnable, Supplier<List<VcsException>> {
  private final List<VcsException> myExceptions;

  public VcsCatchingRunnable() {
    myExceptions = new SmartList<VcsException>();
  }

  @Override
  public List<VcsException> get() {
    return myExceptions;
  }

  protected abstract void runImpl() throws VcsException;

  @Override
  public void run() {
    try {
      runImpl();
    }
    catch (VcsException e) {
      myExceptions.add(e);
    }
  }
}
