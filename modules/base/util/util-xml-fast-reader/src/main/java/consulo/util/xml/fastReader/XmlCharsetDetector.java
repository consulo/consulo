/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.util.xml.fastReader;

import consulo.util.collection.ArrayUtil;
import consulo.util.io.CharsetToolkit;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;

/**
 * @author nik
 */
public class XmlCharsetDetector {
  private static final String XML_PROLOG_START = "<?xml";
  private static final byte[] XML_PROLOG_START_BYTES = XML_PROLOG_START.getBytes(StandardCharsets.UTF_8);
  private static final String ENCODING = "encoding";
  private static final byte[] ENCODING_BYTES = ENCODING.getBytes(StandardCharsets.UTF_8);
  private static final String XML_PROLOG_END = "?>";
  private static final byte[] XML_PROLOG_END_BYTES = XML_PROLOG_END.getBytes(StandardCharsets.UTF_8);

  @Nullable
  public static String extractXmlEncodingFromProlog(byte[] bytes) {
    int index = 0;
    if (CharsetToolkit.hasUTF8Bom(bytes)) {
      index = CharsetToolkit.UTF8_BOM.length;
    }

    index = skipWhiteSpace(index, bytes);
    if (!ArrayUtil.startsWith(bytes, index, XML_PROLOG_START_BYTES)) return null;
    index += XML_PROLOG_START_BYTES.length;
    while (index < bytes.length) {
      index = skipWhiteSpace(index, bytes);
      if (ArrayUtil.startsWith(bytes, index, XML_PROLOG_END_BYTES)) return null;
      if (ArrayUtil.startsWith(bytes, index, ENCODING_BYTES)) {
        index += ENCODING_BYTES.length;
        index = skipWhiteSpace(index, bytes);
        if (index >= bytes.length || bytes[index] != '=') continue;
        index++;
        index = skipWhiteSpace(index, bytes);
        if (index >= bytes.length || bytes[index] != '\'' && bytes[index] != '\"') continue;
        byte quote = bytes[index];
        index++;
        StringBuilder encoding = new StringBuilder();
        while (index < bytes.length) {
          if (bytes[index] == quote) return encoding.toString();
          encoding.append((char)bytes[index++]);
        }
      }
      index++;
    }
    return null;
  }

  @Nullable
  public static String extractXmlEncodingFromProlog(@Nonnull CharSequence text) {
    int index = 0;

    index = skipWhiteSpace(index, text);
    if (!StringUtil.startsWith(text, index, XML_PROLOG_START)) return null;
    index += XML_PROLOG_START.length();
    while (index < text.length()) {
      index = skipWhiteSpace(index, text);
      if (StringUtil.startsWith(text, index, XML_PROLOG_END)) return null;
      if (StringUtil.startsWith(text, index, ENCODING)) {
        index += ENCODING.length();
        index = skipWhiteSpace(index, text);
        if (index >= text.length() || text.charAt(index) != '=') continue;
        index++;
        index = skipWhiteSpace(index, text);
        if (index >= text.length()) continue;
        char quote = text.charAt(index);
        if (quote != '\'' && quote != '\"') continue;
        index++;
        StringBuilder encoding = new StringBuilder();
        while (index < text.length()) {
          char c = text.charAt(index);
          if (c == quote) return encoding.toString();
          encoding.append(c);
          index++;
        }
      }
      index++;
    }
    return null;
  }

  private static int skipWhiteSpace(int start, @Nonnull byte[] bytes) {
    while (start < bytes.length) {
      char c = (char)bytes[start];
      if (!Character.isWhitespace(c)) break;
      start++;
    }
    return start;
  }

  private static int skipWhiteSpace(int start, @Nonnull CharSequence text) {
    while (start < text.length()) {
      char c = text.charAt(start);
      if (!Character.isWhitespace(c)) break;
      start++;
    }
    return start;
  }
}
