/*
 * Copyright 2013-2016 consulo.io
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
package consulo.language;

import consulo.component.util.pointer.NamedPointer;
import consulo.util.lang.lazy.LazyValue;

import jakarta.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 18:32/31.08.13
 */
class LanguagePointerImpl implements NamedPointer<Language> {
  private String myId;

  private final Supplier<Language> myValue;

  public LanguagePointerImpl(String id) {
    myId = id;
    myValue = LazyValue.nullable(() -> Language.findLanguageByID(myId));
  }

  @Nonnull
  @Override
  public String getName() {
    return myId;
  }

  @Override
  public Language get() {
    return myValue.get();
  }
}
