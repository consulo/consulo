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
package consulo.language.pattern;

import consulo.application.progress.ProgressManager;
import consulo.language.util.ProcessingContext;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * @author peter
 */
public class StringPattern extends ObjectPattern<String, StringPattern> {
  private static final InitialPatternCondition<String> CONDITION = new InitialPatternCondition<String>(String.class) {
    @Override
    public boolean accepts(@Nullable final Object o, final ProcessingContext context) {
      return o instanceof String;
    }


    @Override
    public void append(@Nonnull @NonNls final StringBuilder builder, final String indent) {
      builder.append("string()");
    }
  };

  protected StringPattern() {
    super(CONDITION);
  }

  @Nonnull
  public StringPattern startsWith(@NonNls @Nonnull final String s) {
    return with(new PatternCondition<String>("startsWith") {
      @Override
      public boolean accepts(@Nonnull final String str, final ProcessingContext context) {
        return str.startsWith(s);
      }
    });
  }

  @Nonnull
  public StringPattern endsWith(@NonNls @Nonnull final String s) {
    return with(new PatternCondition<String>("endsWith") {
      @Override
      public boolean accepts(@Nonnull final String str, final ProcessingContext context) {
        return str.endsWith(s);
      }
    });
  }

  @Nonnull
  public StringPattern contains(@NonNls @Nonnull final String s) {
    return with(new PatternCondition<String>("contains") {
      @Override
      public boolean accepts(@Nonnull final String str, final ProcessingContext context) {
        return str.contains(s);
      }

    });
  }

  @Nonnull
  public StringPattern containsChars(@NonNls @Nonnull final String s) {
    return with(new PatternCondition<String>("containsChars") {
      @Override
      public boolean accepts(@Nonnull final String str, final ProcessingContext context) {
        for (int i=0, len=s.length(); i<len; i++) {
          if (str.indexOf(s.charAt(i))>-1) return true;
        }
        return false;
      }
    });
  }

  @Nonnull
  public StringPattern matches(@NonNls @Nonnull final String s) {
    final String escaped = StringUtil.escapeToRegexp(s);
    if (escaped.equals(s)) {
      return equalTo(s);
    }
    // may throw PatternSyntaxException here
    final Pattern pattern = Pattern.compile(s);
    return with(new ValuePatternCondition<String>("matches") {
      @Override
      public boolean accepts(@Nonnull final String str, final ProcessingContext context) {
        return pattern.matcher(newBombedCharSequence(str)).matches();
      }

      @Override
      public Collection<String> getValues() {
        return Collections.singleton(s);
      }
    });
  }

  @Nonnull
  public StringPattern contains(@NonNls @Nonnull final ElementPattern<Character> pattern) {
    return with(new PatternCondition<String>("contains") {
      @Override
      public boolean accepts(@Nonnull final String str, final ProcessingContext context) {
        for (int i = 0; i < str.length(); i++) {
          if (pattern.accepts(str.charAt(i))) return true;
        }
        return false;
      }
    });
  }

  public StringPattern longerThan(final int minLength) {
    return with(new PatternCondition<String>("longerThan") {
      @Override
      public boolean accepts(@Nonnull final String s, final ProcessingContext context) {
        return s.length() > minLength;
      }
    });
  }
  public StringPattern shorterThan(final int maxLength) {
    return with(new PatternCondition<String>("shorterThan") {
      @Override
      public boolean accepts(@Nonnull final String s, final ProcessingContext context) {
        return s.length() < maxLength;
      }
    });
  }
  public StringPattern withLength(final int length) {
    return with(new PatternCondition<String>("withLength") {
      @Override
      public boolean accepts(@Nonnull final String s, final ProcessingContext context) {
        return s.length() == length;
      }
    });
  }

  @Override
  @Nonnull
  public StringPattern oneOf(@NonNls final String... values) {
    return super.oneOf(values);
  }

  @Nonnull
  public StringPattern oneOfIgnoreCase(@NonNls final String... values) {
    return with(new CaseInsensitiveValuePatternCondition("oneOfIgnoreCase", values));
  }

  @Override
  @Nonnull
  public StringPattern oneOf(final Collection<String> set) {
    return super.oneOf(set);
  }

  @Nonnull
  public static CharSequence newBombedCharSequence(@Nonnull CharSequence sequence) {
    return new StringUtil.BombedCharSequence(sequence) {
      @Override
      protected void checkCanceled() {
        ProgressManager.checkCanceled();
      }
    };
  }
}
