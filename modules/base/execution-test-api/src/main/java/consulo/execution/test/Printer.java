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
package consulo.execution.test;

import consulo.process.internal.AnsiEscapeDecoder;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.process.ProcessOutputTypes;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;

public interface Printer {
  void print(String text, ConsoleViewContentType contentType);

  void onNewAvailable(@Nonnull Printable printable);

  void printHyperlink(String text, HyperlinkInfo info);

  void mark();

  default void printWithAnsiColoring(@Nonnull String text, @Nonnull Key processOutputType) {
    AnsiEscapeDecoder decoder = new AnsiEscapeDecoder();
    decoder.escapeText(text, ProcessOutputTypes.STDOUT, (text1, attributes) -> {
      ConsoleViewContentType contentType = ConsoleViewContentType.getConsoleViewType(attributes);
      if (contentType == null || contentType == ConsoleViewContentType.NORMAL_OUTPUT) {
        contentType = ConsoleViewContentType.getConsoleViewType(processOutputType);
      }
      print(text1, contentType);
    });
  }

  default void printWithAnsiColoring(@Nonnull String text, @Nonnull ConsoleViewContentType contentType) {
    AnsiEscapeDecoder decoder = new AnsiEscapeDecoder();
    decoder.escapeText(text, ProcessOutputTypes.STDOUT, (text1, attributes) -> {
      ConsoleViewContentType viewContentType = ConsoleViewContentType.getConsoleViewType(attributes);
      if (viewContentType == null) {
        viewContentType = contentType;
      }
      print(text1, viewContentType);
    });
  }
}
