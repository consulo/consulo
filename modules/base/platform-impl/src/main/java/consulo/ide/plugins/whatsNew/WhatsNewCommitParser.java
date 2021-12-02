/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.plugins.whatsNew;

import com.intellij.openapi.util.text.HtmlChunk;

import java.util.regex.Pattern;

/**
 * @author VISTALL
 * @since 02/12/2021
 */
public class WhatsNewCommitParser {
  private static final Pattern ourFixPattern = Pattern.compile("(fixed|fixes|fix)\\s+#(\\d+)");

  public static HtmlChunk parse(String commitMessage) {
    //Map<TextRange, CharSequence> parts = new LinkedHashMap<>();
    //Matcher matcher = ourFixPattern.matcher(commitMessage);
    //while (matcher.find()) {
    //  int s = matcher.regionStart();
    //  int e = matcher.regionEnd();
    //
    //  String issueId = matcher.group(1);
    //
    //  parts.put(new TextRange(s, e), issueId);
    //}
    //
    //if (parts.isEmpty()) {
    //  return HtmlChunk.span().addText(commitMessage);
    //}

    return HtmlChunk.span().addText(commitMessage);
  }
}
