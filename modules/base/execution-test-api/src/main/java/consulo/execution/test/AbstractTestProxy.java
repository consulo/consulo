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
package consulo.execution.test;

import consulo.disposer.Disposer;
import consulo.execution.action.Location;
import consulo.execution.test.stacktrace.DiffHyperlink;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author anna
 * @since 23-May-2007
 */
public abstract class AbstractTestProxy extends CompositePrintable {
  public static final Key<AbstractTestProxy> KEY = Key.create("testProxy");
  public static final Key<AbstractTestProxy[]> KEY_OF_ARRAY = Key.create("testProxies");

  protected Printer myPrinter = null;

  public abstract boolean isInProgress();

  public abstract boolean isDefect();

  public abstract boolean shouldRun();

  public abstract int getMagnitude();

  public abstract boolean isLeaf();

  public abstract boolean isInterrupted();

  public abstract boolean hasPassedTests();

  public abstract boolean isIgnored();

  public abstract boolean isPassed();

  public abstract String getName();

  public abstract boolean isConfig();

  public abstract Location getLocation(@Nonnull Project project, @Nonnull GlobalSearchScope searchScope);

  public abstract Navigatable getDescriptor(@Nullable Location location, @Nonnull TestConsoleProperties properties);

  public abstract AbstractTestProxy getParent();

  public abstract List<? extends AbstractTestProxy> getChildren();

  public abstract List<? extends AbstractTestProxy> getAllTests();

  @Nullable
  public Long getDuration() {
    return null;
  }

  @Nullable
  public String getDurationString(TestConsoleProperties consoleProperties) {
    return null;
  }

  public abstract boolean shouldSkipRootNodeForExport();

  public void fireOnNewPrintable(@Nonnull Printable printable) {
    if (myPrinter != null) {
      myPrinter.onNewAvailable(printable);
    }
  }

  public void setPrinter(Printer printer) {
    myPrinter = printer;
    for (AbstractTestProxy testProxy : getChildren()) {
      testProxy.setPrinter(printer);
    }
  }

  /**
   * Stores printable information in internal buffer and notifies
   * proxy's printer about new text available
   * @param printable Printable info
   */
  @Override
  public void addLast(@Nonnull Printable printable) {
    super.addLast(printable);
    fireOnNewPrintable(printable);
  }

  @Override
  public void insert(@Nonnull Printable printable, int i) {
    super.insert(printable, i);
    fireOnNewPrintable(printable);
  }

  @Override
  public void dispose() {
    super.dispose();
    for (AbstractTestProxy proxy : getChildren()) {
      Disposer.dispose(proxy);
    }
  }

  /**
   * to be deleted in 2017.1
   */
  @Deprecated
  public static void flushOutput(AbstractTestProxy testProxy) {
    testProxy.flush();

    AbstractTestProxy parent = testProxy.getParent();
    while (parent != null) {
      List<? extends AbstractTestProxy> children = parent.getChildren();
      if (!testProxy.isInProgress() && testProxy.equals(children.get(children.size() - 1))) {
        parent.flush();
      } else {
        break;
      }
      testProxy = parent;
      parent = parent.getParent();
    }
  }

  @Override
  public int getExceptionMark() {
    if (myExceptionMark == 0 && getChildren().size() > 0) {
      return getChildren().get(0).getExceptionMark();
    }
    return myExceptionMark;
  }

  @Nonnull
  public List<DiffHyperlink> getDiffViewerProviders() {
    DiffHyperlink provider = getDiffViewerProvider();
    return provider == null ? Collections.emptyList() : Collections.singletonList(provider);
  }

  @Nullable
  public DiffHyperlink getDiffViewerProvider() {
    return null;
  }

  @Nullable
  public String getLocationUrl() {
    return null;
  }

  @Nullable
  public String getMetainfo() {
    return null;
  }
}
