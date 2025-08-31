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

import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.HyperlinkInfo;

import jakarta.annotation.Nonnull;

public class DeferingPrinter implements Printer {
  private CompositePrintable myCompositePrintable;

  public DeferingPrinter() {
    myCompositePrintable = new CompositePrintable();
  }

  @Override
  public void print(final String text, final ConsoleViewContentType contentType) {
    myCompositePrintable.addLast(new Printable() {
      @Override
      public void printOn(Printer printer) {
        printer.print(text, contentType);
      }
    });
  }

  @Override
  public void onNewAvailable(@Nonnull Printable printable) {
    myCompositePrintable.addLast(printable);
  }

  @Override
  public void printHyperlink(String text, HyperlinkInfo info) {
    myCompositePrintable.addLast(new HyperLink(text, info));
  }

  @Override
  public void mark() {
    myCompositePrintable.addLast(new PrinterMark());
  }

  public void printAndForget(Printer printer) {
    myCompositePrintable.printOn(printer);
    myCompositePrintable.clear();
  }
}
