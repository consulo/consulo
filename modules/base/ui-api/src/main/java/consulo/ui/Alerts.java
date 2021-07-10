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

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.util.lang.ThreeState;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 6/9/18
 */
public final class Alerts {
  private static final Object ourStableNull = new Object();

  @Nonnull
  @Deprecated
  @DeprecationInfo("Use #okInfo(LocalizeValue)")
  public static Alert<Object> okInfo(@Nonnull String text) {
    return okTemplate(LocalizeValue.of(text), o -> o);
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo("Use #okWarning(LocalizeValue)")
  public static Alert<Object> okWarning(@Nonnull String text) {
    return okTemplate(LocalizeValue.of(text), Alert::asWarning);
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo("Use #okError(LocalizeValue)")
  public static Alert<Object> okError(@Nonnull String text) {
    return okTemplate(LocalizeValue.of(text), Alert::asError);
  }

  @Nonnull
  public static Alert<Object> okInfo(@Nonnull LocalizeValue textValue) {
    return okTemplate(textValue, o -> o);
  }

  @Nonnull
  public static Alert<Object> okWarning(@Nonnull LocalizeValue textValue) {
    return okTemplate(textValue, Alert::asWarning);
  }

  @Nonnull
  public static Alert<Object> okError(@Nonnull LocalizeValue textValue) {
    return okTemplate(textValue, Alert::asError);
  }

  @Nonnull
  public static Alert<Object> okQuestion(@Nonnull LocalizeValue textValue) {
    return okTemplate(textValue, Alert::asQuestion);
  }

  @Nonnull
  private static Alert<Object> okTemplate(@Nonnull LocalizeValue text, Function<Alert<Object>, Alert<Object>> levelSet) {
    Alert<Object> builder = Alert.create();
    levelSet.apply(builder);
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
