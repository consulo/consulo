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
package consulo.lang;

import com.intellij.lang.Language;
import com.intellij.openapi.util.NullableLazyValue;
import consulo.util.pointers.NamedPointer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 18:32/31.08.13
 */
public class LanguagePointerImpl implements NamedPointer<Language> {
  private String myId;

  private NullableLazyValue<Language> myValue = new NullableLazyValue<Language>() {
    @Nullable
    @Override
    protected Language compute() {
      return Language.findLanguageByID(myId);
    }
  };

  public LanguagePointerImpl(String id) {
    myId = id;
  }

  @Nonnull
  @Override
  public String getName() {
    return myId;
  }

  @Override
  public Language get() {
    return myValue.getValue();
  }
}
