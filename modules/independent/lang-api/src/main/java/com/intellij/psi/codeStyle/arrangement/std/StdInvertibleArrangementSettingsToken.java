/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.psi.codeStyle.arrangement.std;

import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class StdInvertibleArrangementSettingsToken extends StdArrangementSettingsToken implements InvertibleArrangementSettingsToken {
  private static final String NOT = "not ";

  private StdInvertibleArrangementSettingsToken(@Nonnull String id,
                                                @Nonnull String uiName,
                                                @Nonnull StdArrangementTokenType tokenType) {
    super(id, uiName, tokenType);
  }

  @Nonnull
  public static StdInvertibleArrangementSettingsToken invertibleTokenById(@NonNls @Nonnull String id,
                                                                          @Nonnull StdArrangementTokenType tokenType) {
    return new StdInvertibleArrangementSettingsToken(id, id.toLowerCase().replace("_", " "), tokenType);
  }

  @Nonnull
  @Override
  public String getInvertedRepresentationValue() {
    return NOT + getRepresentationValue();
  }
}
