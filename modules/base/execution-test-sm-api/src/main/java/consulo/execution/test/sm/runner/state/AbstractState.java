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
package consulo.execution.test.sm.runner.state;

import consulo.execution.test.Printable;
import consulo.execution.test.Printer;
import consulo.execution.test.TestStateInfo;

/**
 * @author Roman Chernyatchik
 *
 * Inheritors of this class describes concreate states of tests
 * with additional info e.g. stacktraces for failed state or
 * ignored message for ignored state
 */
public abstract class AbstractState implements Printable, TestStateInfo {
  public void printOn(final Printer printer) {
    // Do nothing by default
  }
}
