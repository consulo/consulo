/*
 * Copyright 2013-2019 consulo.io
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
package consulo.localize.impl;

import com.intellij.CommonBundle;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
class LocalizeKeyImpl implements LocalizeKey {
  private final String myId;

  private LocalizeLibraryImpl myLibrary;

  LocalizeKeyImpl(String id) {
    myId = id;
  }

  void setLibrary(LocalizeLibraryImpl library) {
    myLibrary = library;
  }

  @Nonnull
  @Override
  public LocalizeValue getValue() {
    return () -> myLibrary.getText(myId);
  }

  @Nonnull
  @Override
  public LocalizeValue getValue(Object arg) {
    return () -> CommonBundle.format(myLibrary.getText(myId), arg);
  }

  @Nonnull
  @Override
  public LocalizeValue getValue(Object arg0, Object arg1) {
    return () -> CommonBundle.format(myLibrary.getText(myId), arg0, arg1);
  }

  @Nonnull
  @Override
  public LocalizeValue getValue(Object arg0, Object arg1, Object arg2) {
    return () -> CommonBundle.format(myLibrary.getText(myId), arg0, arg1, arg2);
  }

  @Nonnull
  @Override
  public LocalizeValue getValue(Object arg0, Object arg1, Object arg2, Object arg3) {
    return () -> CommonBundle.format(myLibrary.getText(myId), arg0, arg1, arg2, arg3);
  }

  @Nonnull
  @Override
  public LocalizeValue getValue(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
    return () -> CommonBundle.format(myLibrary.getText(myId), arg0, arg1, arg2, arg3, arg4);
  }
}
