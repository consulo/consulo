/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.build.ui.impl.internal.event;

import consulo.build.ui.event.BuildEventsNls;
import consulo.build.ui.event.Failure;
import consulo.build.ui.event.FailureResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class FailureResultImpl implements FailureResult {

  private final List<Failure> myFailures;

  public FailureResultImpl() {
    this(null, null);
  }

  public FailureResultImpl(@Nullable Throwable error) {
    this(null, error);
  }

  public FailureResultImpl(@Nullable @BuildEventsNls.Message String message) {
    this(message, null);
  }

  public FailureResultImpl(@Nullable @BuildEventsNls.Message  String message, @Nullable Throwable error) {
    myFailures = new ArrayList<>();
    if (message != null || error != null) {
      myFailures.add(new FailureImpl(message, error));
    }
  }

  public FailureResultImpl(@Nonnull List<Failure> failures) {
    myFailures = failures;
  }
  
  @Override
  public List<? extends Failure> getFailures() {
    return myFailures;
  }
}
