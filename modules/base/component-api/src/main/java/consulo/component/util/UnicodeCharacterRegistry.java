/*
 * Copyright 2013-2022 consulo.io
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
package consulo.component.util;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.ValueIterator;
import consulo.annotation.UsedInPlugin;
import consulo.hacking.java.base.CharacterNameHacking;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2022-03-07
 */
@UsedInPlugin
public final class UnicodeCharacterRegistry {
  public final static class UnicodeCharacter {
    private final String myName;
    private final int myCodePoint;

    public UnicodeCharacter(String name, int codePoint) {
      myName = name;
      myCodePoint = codePoint;
    }

    public int getCodePoint() {
      return myCodePoint;
    }

    public String getName() {
      return myName;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      UnicodeCharacter that = (UnicodeCharacter)o;
      return myCodePoint == that.myCodePoint;
    }

    @Override
    public int hashCode() {
      return Objects.hash(myCodePoint);
    }

    @Override
    public String toString() {
      return "UnicodeCharacter{" + "myName='" + myName + '\'' + ", myCodePoint=" + myCodePoint + '}';
    }
  }

  private interface Provider {
    List<UnicodeCharacter> list();
  }

  private static class ProviderFromJava implements Provider {
    @Override
    public List<UnicodeCharacter> list() {
      List<UnicodeCharacter> characters = new ArrayList<>(50_000);

      CharacterNameHacking.iterate(name -> {
        int codePoint = CharacterNameHacking.getCodePoint(name);
        characters.add(new UnicodeCharacter(name, codePoint));
      });
      return characters;
    }
  }

  private static class ProviderICU4J implements Provider {
    @Override
    public List<UnicodeCharacter> list() {
      List<UnicodeCharacter> characters = new ArrayList<>(150_000);

      ValueIterator iterator = UCharacter.getNameIterator();
      ValueIterator.Element result = new ValueIterator.Element();
      iterator.setRange(UCharacter.MIN_VALUE, UCharacter.MAX_VALUE);
      while (iterator.next(result)) {
        characters.add(new UnicodeCharacter((String)result.value, result.integer));
      }

      return characters;
    }
  }

  // FIXME [VISTALL] ProviderICU4J return bigger list of characters, we can fallback to ProviderFromJava
  private static Provider ourProvider = Boolean.getBoolean("consulo.use.java.unicode.registry") ? new ProviderFromJava() : new ProviderICU4J();

  @Nullable
  private static List<UnicodeCharacter> ourCharacters = null;

  public static List<UnicodeCharacter> listCharacters() {
    if (ourCharacters == null) {
      ourCharacters = ourProvider.list();
    }
    return ourCharacters;
  }
}
