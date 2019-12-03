/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui;

import consulo.util.lang.ThreeState;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 6/9/18
 */
public final class Alerts {
  private static final Object ourStableNull = new Object();

  @Nonnull
  public static Alert<Object> okError(@Nonnull String text) {
    Alert<Object> builder = Alert.create();
    builder.asError();
    builder.text(text);

    builder.button(Alert.OK, ourStableNull);
    builder.asDefaultButton();
    builder.asExitButton();
    return builder;
  }

  @Nonnull
  public static Alert<Boolean> okCancel() {
    Alert<Boolean> builder = Alert.<Boolean>create();

    builder.button(Alert.OK, Boolean.TRUE);
    builder.asDefaultButton();

    builder.button(Alert.CANCEL, Boolean.FALSE);
    builder.asExitButton();

    return builder;
  }

  @Nonnull
  public static Alert<Boolean> yesNo() {
    Alert<Boolean> builder = Alert.<Boolean>create();

    builder.button(Alert.YES, Boolean.TRUE);
    builder.asDefaultButton();

    builder.button(Alert.NO, Boolean.FALSE);
    builder.asExitButton();

    return builder;
  }

  @Nonnull
  public static Alert<ThreeState> yesNoCancel() {
    Alert<ThreeState> builder = Alert.<ThreeState>create();

    builder.button(Alert.OK, ThreeState.YES);
    builder.asDefaultButton();

    builder.button(Alert.NO, ThreeState.NO);

    builder.button(Alert.CANCEL, ThreeState.UNSURE);
    builder.asExitButton();

    return builder;
  }
}
