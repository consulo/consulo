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
package consulo.ide.impl.idea.vcs.log.ui.frame;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class HtmlTableBuilder {
  @Nonnull
  private final StringBuilder myStringBuilder = new StringBuilder();

  public HtmlTableBuilder() {
    myStringBuilder.append("<table>");
  }

  public HtmlTableBuilder startRow() {
    myStringBuilder.append("<tr>");
    return this;
  }

  public HtmlTableBuilder endRow() {
    myStringBuilder.append("</tr>");
    return this;
  }

  public HtmlTableBuilder append(@Nonnull String value) {
    return append(value, null);
  }

  public HtmlTableBuilder append(@Nonnull String value, @Nullable String align) {
    myStringBuilder.append("<td");
    if (align != null) {
      myStringBuilder.append(" align=\"").append(align).append("\"");
    }
    myStringBuilder.append(">").append(value).append("</td>");
    return this;
  }

  public String build() {
    myStringBuilder.append("</table>");
    return myStringBuilder.toString();
  }
}
