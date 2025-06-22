/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.test.stacktrace;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.test.Printable;
import consulo.execution.test.Printer;
import consulo.execution.test.action.ViewAssertEqualsDiffAction;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.execution.ui.console.HyperlinkInfoBase;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.RelativePoint;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;

/**
 * @author anna
 * @since 2007-08-15
 */
public class DiffHyperlink implements Printable {
  private static final String NEW_LINE = "\n";
  private static final Logger LOG = Logger.getInstance(DiffHyperlink.class);

  protected final String myExpected;
  protected final String myActual;
  protected final String myFilePath;
  protected final String myActualFilePath;
  private boolean myPrintOneLine;
  private final HyperlinkInfo myDiffHyperlink = new DiffHyperlinkInfo();
  private String myTestProxyName;


  public DiffHyperlink(final String expected, final String actual, final String filePath) {
    this(expected, actual, filePath, true);
  }

  public DiffHyperlink(final String expected, final String actual, final String filePath, boolean printOneLine) {
    this(expected, actual, filePath, null, printOneLine);
  }

  public DiffHyperlink(final String expected, final String actual, final String expectedFilePath, final String actualFilePath, boolean printOneLine) {
    myExpected = expected;
    myActual = actual;
    myFilePath = normalizeSeparators(expectedFilePath);
    myActualFilePath = normalizeSeparators(actualFilePath);
    myPrintOneLine = printOneLine;
  }

  public void setTestProxyName(String name) {
    myTestProxyName = name;
  }

  private static String normalizeSeparators(String filePath) {
    return filePath == null ? null : filePath.replace(File.separatorChar, '/');
  }

  @Nullable
  public String getTestName() {
    return myTestProxyName;
  }

  protected String getTitle() {
    return ExecutionLocalize.stringsEqualFailedDialogTitle().get();
  }

  public String getDiffTitle() {
    return getTitle();
  }

  public String getLeft() {
    return myExpected;
  }

  public String getRight() {
    return myActual;
  }

  public String getFilePath() {
    return myFilePath;
  }

  public String getActualFilePath() {
    return myActualFilePath;
  }

  public void printOn(final Printer printer) {
    if (!hasMoreThanOneLine(myActual.trim()) && !hasMoreThanOneLine(myExpected.trim()) && myPrintOneLine) {
      printer.print(NEW_LINE, ConsoleViewContentType.ERROR_OUTPUT);
      printer.print(ExecutionLocalize.diffContentExpectedForFileTitle().get(), ConsoleViewContentType.SYSTEM_OUTPUT);
      printer.print(myExpected + NEW_LINE, ConsoleViewContentType.ERROR_OUTPUT);
      printer.print(ExecutionLocalize.junitActualTextLabel().get(), ConsoleViewContentType.SYSTEM_OUTPUT);
      printer.print(myActual + NEW_LINE, ConsoleViewContentType.ERROR_OUTPUT);
    }
    printer.print(" ", ConsoleViewContentType.ERROR_OUTPUT);
    printer.printHyperlink(ExecutionLocalize.junitClickToSeeDiffLink().get(), myDiffHyperlink);
    printer.print(NEW_LINE, ConsoleViewContentType.ERROR_OUTPUT);
  }

  private static boolean hasMoreThanOneLine(final String string) {
    return string.indexOf('\n') != -1 || string.indexOf('\r') != -1;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DiffHyperlink)) return false;

    DiffHyperlink hyperlink = (DiffHyperlink)o;

    if (myActual != null ? !myActual.equals(hyperlink.myActual) : hyperlink.myActual != null) return false;
    if (myExpected != null ? !myExpected.equals(hyperlink.myExpected) : hyperlink.myExpected != null) return false;
    if (myFilePath != null ? !myFilePath.equals(hyperlink.myFilePath) : hyperlink.myFilePath != null) return false;
    if (myActualFilePath != null ? !myActualFilePath.equals(hyperlink.myActualFilePath) : hyperlink.myActualFilePath != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myExpected != null ? myExpected.hashCode() : 0;
    result = 31 * result + (myActual != null ? myActual.hashCode() : 0);
    result = 31 * result + (myFilePath != null ? myFilePath.hashCode() : 0);
    result = 31 * result + (myActualFilePath != null ? myActualFilePath.hashCode() : 0);
    return result;
  }

  public class DiffHyperlinkInfo extends HyperlinkInfoBase {
    @Override
    public void navigate(@Nonnull Project project, @Nullable RelativePoint hyperlinkLocationPoint) {
      final DataManager dataManager = DataManager.getInstance();
      final DataContext dataContext = hyperlinkLocationPoint != null ? dataManager.getDataContext(hyperlinkLocationPoint.getOriginalComponent()) : dataManager.getDataContext();
      ViewAssertEqualsDiffAction.openDiff(dataContext, DiffHyperlink.this);
    }

    public DiffHyperlink getPrintable() {
      return DiffHyperlink.this;
    }
  }
}