/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.execution.ui.console;

import jakarta.annotation.Nonnull;
import java.util.regex.Pattern;

public class PatternHyperlinkFormat {
  private final Pattern myPattern;
  private final boolean myZeroBasedLineNumbering;
  private final boolean myZeroBasedColumnNumbering;
  private final PatternHyperlinkPart[] myLinkParts;

  public PatternHyperlinkFormat(@Nonnull Pattern pattern, boolean zeroBasedLineNumbering, boolean zeroBasedColumnNumbering, @Nonnull PatternHyperlinkPart... linkParts) {
    myPattern = pattern;
    myZeroBasedLineNumbering = zeroBasedLineNumbering;
    myZeroBasedColumnNumbering = zeroBasedColumnNumbering;
    myLinkParts = linkParts;
  }

  @Nonnull
  public Pattern getPattern() {
    return myPattern;
  }

  public boolean isZeroBasedLineNumbering() {
    return myZeroBasedLineNumbering;
  }

  public boolean isZeroBasedColumnNumbering() {
    return myZeroBasedColumnNumbering;
  }

  @Nonnull
  public PatternHyperlinkPart[] getLinkParts() {
    return myLinkParts;
  }
}