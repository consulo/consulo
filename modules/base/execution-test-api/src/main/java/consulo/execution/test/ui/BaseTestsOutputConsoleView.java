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
package consulo.execution.test.ui;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.test.*;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.*;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.ui.ex.HelpIdProvider;
import consulo.ui.ex.action.AnAction;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.function.BiPredicate;

public abstract class BaseTestsOutputConsoleView implements ConsoleView, ObservableConsoleView, HelpIdProvider {
  private ConsoleView myConsole;
  private TestsOutputConsolePrinter myPrinter;
  protected TestConsoleProperties myProperties;
  protected TestResultsPanel myTestResultsPanel;

  public BaseTestsOutputConsoleView(final TestConsoleProperties properties, final AbstractTestProxy unboundOutputRoot) {
    myProperties = properties;

    TextConsoleBuilderFactory factory = TextConsoleBuilderFactory.getInstance();

    TextConsoleBuilder builder = factory.createBuilder(properties.getProject(), myProperties.getScope());
    builder.setViewer(!properties.isEditable());
    builder.setUsePredefinedMessageFilter(properties.isUsePredefinedMessageFilter());
    builder.setState(new TestConsoleState(!properties.isEditable()));

    myConsole = builder.getConsole();
    myPrinter = new TestsOutputConsolePrinter(this, properties, unboundOutputRoot);
    myProperties.setConsole(this);

    Disposer.register(this, myProperties);
    Disposer.register(this, myConsole);
  }

  public void initUI() {
    myTestResultsPanel = createTestResultsPanel();
    myTestResultsPanel.initUI();
    Disposer.register(this, myTestResultsPanel);
  }

  protected abstract TestResultsPanel createTestResultsPanel();

  @Override
  public void attachToProcess(final ProcessHandler processHandler) {
    myConsole.attachToProcess(processHandler);
  }

  @Override
  public void print(final String s, final ConsoleViewContentType contentType) {
    printNew(printer -> printer.print(s, contentType));
  }

  @Override
  public void allowHeavyFilters() {
  }

  @Override
  public void setProcessTextFilter(@Nullable BiPredicate<ProcessEvent, Key> filter) {
    myConsole.setProcessTextFilter(filter);
  }

  @Nullable
  @Override
  public BiPredicate<ProcessEvent, Key> getProcessTextFilter() {
    return myConsole.getProcessTextFilter();
  }

  @Override
  public void clear() {
    myConsole.clear();
  }

  @Override
  public void scrollTo(final int offset) {
    myConsole.scrollTo(offset);
  }

  @Override
  public void setOutputPaused(final boolean value) {
    if (myPrinter != null) {
      myPrinter.pause(value);
    }
  }

  @Override
  public boolean isOutputPaused() {
    //noinspection SimplifiableConditionalExpression
    return myPrinter == null ? true : myPrinter.isPaused();
  }

  @Override
  public boolean hasDeferredOutput() {
    return myConsole.hasDeferredOutput();
  }

  @Override
  public void performWhenNoDeferredOutput(final Runnable runnable) {
    myConsole.performWhenNoDeferredOutput(runnable);
  }

  @Override
  public void setHelpId(final String helpId) {
    myConsole.setHelpId(helpId);
  }

  @Override
  public void addMessageFilter(final Filter filter) {
    myConsole.addMessageFilter(filter);
  }

  @Override
  public void printHyperlink(final String hyperlinkText, final HyperlinkInfo info) {
    printNew(new HyperLink(hyperlinkText, info));
  }

  @Override
  public int getContentSize() {
    return myConsole.getContentSize();
  }

  @Override
  public boolean canPause() {
    return myPrinter != null && myPrinter.canPause() && myConsole.canPause();
  }

  @Override
  public JComponent getComponent() {
    return myTestResultsPanel;
  }

  @Override
  public JComponent getPreferredFocusableComponent() {
    return myConsole.getPreferredFocusableComponent();
  }

  @Override
  public void dispose() {
    myPrinter = null;
    myProperties = null;
    myConsole = null;
  }

  @Override
  public void addChangeListener(@Nonnull final ChangeListener listener, @Nonnull final Disposable parent) {
    if (myConsole instanceof ObservableConsoleView) {
      ((ObservableConsoleView)myConsole).addChangeListener(listener, parent);
    } else {
      throw new UnsupportedOperationException(myConsole.getClass().getName());
    }
  }

  @Override
  @Nonnull
  public AnAction[] createConsoleActions() {
    return AnAction.EMPTY_ARRAY;
  }

  @Nonnull
  public ConsoleView getConsole() {
    return myConsole;
  }

  public TestsOutputConsolePrinter getPrinter() {
    return myPrinter;
  }

  private void printNew(final Printable printable) {
    if (myPrinter != null) {
      myPrinter.onNewAvailable(printable);
    }
  }

  public TestConsoleProperties getProperties() {
    return myProperties;
  }

  @javax.annotation.Nullable
  @Override
  public String getHelpId() {
    return "reference.runToolWindow.testResultsTab";
  }
}
