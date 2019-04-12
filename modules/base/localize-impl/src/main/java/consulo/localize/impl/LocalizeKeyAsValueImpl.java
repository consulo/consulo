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

import consulo.localize.LocalizeKeyAsValue;
import consulo.localize.LocalizeValue;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
public class LocalizeKeyAsValueImpl implements LocalizeKeyAsValue {
  private final String myId;

  public LocalizeKeyAsValueImpl(String id) {
    myId = id;
  }

  @Nonnull
  @Override
  public LocalizeValue getValue(Object arg) {
    return null;
  }

  @Nonnull
  @Override
  public LocalizeValue getValue(Object arg0, Object arg1) {
    return null;
  }

  @Nonnull
  @Override
  public LocalizeValue getValue(Object arg0, Object arg1, Object arg2) {
    return null;
  }

  @Nonnull
  @Override
  public LocalizeValue getValue(Object arg0, Object arg1, Object arg2, Object arg3) {
    return null;
  }

  @Nonnull
  @Override
  public LocalizeValue getValue(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
    return null;
  }

  @Nonnull
  @Override
  public String getValue() {
    return null;
  }
}
