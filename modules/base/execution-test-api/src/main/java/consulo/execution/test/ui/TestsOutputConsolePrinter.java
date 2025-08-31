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
package consulo.execution.test.ui;

import consulo.execution.test.*;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.disposer.Disposable;

import jakarta.annotation.Nonnull;

public class TestsOutputConsolePrinter implements Printer, Disposable {
  private final ConsoleView myConsole;
  private final TestConsoleProperties myProperties;
  private final AbstractTestProxy myUnboundOutputRoot;
  private AbstractTestProxy myCurrentTest;

  // After pause action has been invoked -  all output will be redirected to special
  // myPausedPrinter which will dump all buffered data after user will continue process.
  private final DeferingPrinter myPausedPrinter = new DeferingPrinter();
  private boolean myPaused = false;

  private int myMarkOffset = 0;

  private final TestFrameworkPropertyListener<Boolean> myPropertyListener = new TestFrameworkPropertyListener<Boolean>() {
        public void onChanged(Boolean value) {
          if (!value.booleanValue()) myMarkOffset = 0;
        }
      };

  public TestsOutputConsolePrinter(@Nonnull BaseTestsOutputConsoleView testsOutputConsoleView, TestConsoleProperties properties, AbstractTestProxy unboundOutputRoot) {
    this(testsOutputConsoleView.getConsole(), properties, unboundOutputRoot);
  }

  /**
   * @deprecated left for JSTestDriver compatibility
   */
  @Deprecated
  public TestsOutputConsolePrinter(ConsoleView console, TestConsoleProperties properties, AbstractTestProxy unboundOutputRoot) {
    myConsole = console;
    myProperties = properties;
    myUnboundOutputRoot = unboundOutputRoot;
    myProperties.addListener(TestConsoleProperties.SCROLL_TO_STACK_TRACE, myPropertyListener);
  }

  public ConsoleView getConsole() {
    return myConsole;
  }

  public boolean isPaused() {
    return myPaused;
  }

  public void pause(boolean doPause) {
    myPaused = doPause;
    if (!doPause) {
      myPausedPrinter.printAndForget(this);
    }
  }

  public void print(String text, ConsoleViewContentType contentType) {
    myConsole.print(text, contentType);
  }

  public void onNewAvailable(@Nonnull Printable printable) {
    if (myPaused) {
      printable.printOn(myPausedPrinter);
    } else {
      printable.printOn(this);
    }
  }

  /**
   * Clears console, prints output of selected test and scrolls to beginning
   * of output.
   * This method must be invoked in Event Dispatch Thread
   * @param test Selected test
   */
  public void updateOnTestSelected(AbstractTestProxy test) {
    if (myCurrentTest == test) {
      return;
    }
    if (myCurrentTest != null) {
      myCurrentTest.setPrinter(null);
    }
    myMarkOffset = 0;
    Runnable clearRunnable = new Runnable() {
      public void run() {
        myConsole.clear();
      }
    };
    if (test == null) {
      myCurrentTest = null;
      CompositePrintable.invokeInAlarm(clearRunnable);
      return;
    }
    myCurrentTest = test;
    myCurrentTest.setPrinter(this);
    Runnable scrollRunnable = new Runnable() {
      @Override
      public void run() {
        scrollToBeginning();
      }
    };
    AbstractTestProxy currentProxyOrRoot = getCurrentProxyOrRoot();
    CompositePrintable.invokeInAlarm(clearRunnable);
    currentProxyOrRoot.printOn(this);
    CompositePrintable.invokeInAlarm(scrollRunnable);
  }

  private AbstractTestProxy getCurrentProxyOrRoot() {
    return isRoot() && myUnboundOutputRoot != null ? myUnboundOutputRoot : myCurrentTest;
  }

  public boolean isCurrent(CompositePrintable printable) {
    return myCurrentTest == printable || isRoot();
  }

  private boolean isRoot() {
    return myCurrentTest != null && myCurrentTest.getParent() == myUnboundOutputRoot;
  }

  public void printHyperlink(String text, HyperlinkInfo info) {
    myConsole.printHyperlink(text, info);
  }

  public void mark() {
    if (TestConsoleProperties.SCROLL_TO_STACK_TRACE.value(myProperties))
      myMarkOffset = myConsole.getContentSize();
  }

  public void dispose() {
    myProperties.removeListener(TestConsoleProperties.SCROLL_TO_STACK_TRACE, myPropertyListener);
  }

  public boolean canPause() {
    return myCurrentTest != null ? myCurrentTest.isInProgress() : false;
  }

  protected void scrollToBeginning() {
    myConsole.performWhenNoDeferredOutput(new Runnable() {
      public void run() {
        AbstractTestProxy currentProxyOrRoot = getCurrentProxyOrRoot();
        if (currentProxyOrRoot != null && !currentProxyOrRoot.isInProgress()) {
          //do not scroll to any mark during run
          myConsole.scrollTo(myMarkOffset);
        }
      }
    });
  }
}
