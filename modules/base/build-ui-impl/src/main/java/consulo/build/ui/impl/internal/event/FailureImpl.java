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
import consulo.project.ui.notification.Notification;
import consulo.navigation.Navigatable;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class FailureImpl implements Failure {

  private final @BuildEventsNls.Message String myMessage;
  private final @BuildEventsNls.Description String myDescription;
  private final List<? extends Failure> myCauses;
  private final @Nullable Throwable myError;
  private final @Nullable Notification myNotification;
  private final @Nullable Navigatable myNavigatable;

  public FailureImpl(@BuildEventsNls.Message String message, Throwable error) {
    this(message, null, Collections.emptyList(), error, null, null);
  }

  public FailureImpl(@BuildEventsNls.Message String message,
                     Throwable error,
                     @Nullable Notification notification,
                     @Nullable Navigatable navigatable) {
    this(message, null, Collections.emptyList(), error, notification, navigatable);
  }

  public FailureImpl(@BuildEventsNls.Message String message, @BuildEventsNls.Description String description) {
    this(message, description, Collections.emptyList(), null, null, null);
  }

  public FailureImpl(@BuildEventsNls.Message String message,
                     @BuildEventsNls.Description String description,
                     List<? extends Failure> causes) {
    this(message, description, causes, null, null, null);
  }

  public FailureImpl(@BuildEventsNls.Message String message,
                      @BuildEventsNls.Description String description,
                      List<? extends Failure> causes,
                      @Nullable Throwable error,
                      @Nullable Notification notification,
                      @Nullable Navigatable navigatable) {
    myMessage = message;
    myDescription = description;
    myCauses = causes;
    myError = error;
    myNotification = notification;
    myNavigatable = navigatable;
  }

  @Override
  public @Nullable String getMessage() {
    return myMessage;
  }

  @Override
  public @Nullable String getDescription() {
    return myDescription;
  }

  @Override
  public @Nullable Throwable getError() {
    return myError;
  }

  @Override
  public List<? extends Failure> getCauses() {
    return myCauses;
  }

  @Override
  public @Nullable Notification getNotification() {
    return myNotification;
  }

  @Override
  public @Nullable Navigatable getNavigatable() {
    return myNavigatable;
  }
}
