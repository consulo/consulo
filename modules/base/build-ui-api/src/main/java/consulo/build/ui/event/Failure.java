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
package consulo.build.ui.event;

import consulo.project.ui.notification.Notification;
import consulo.navigation.Navigatable;

import org.jspecify.annotations.Nullable;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public interface Failure {
  @BuildEventsNls.Message
  @Nullable String getMessage();

  @BuildEventsNls.Description
  @Nullable String getDescription();

  default @Nullable Throwable getError() {return null;}

  List<? extends Failure> getCauses();

  default @Nullable Notification getNotification() {return null;}

  default @Nullable Navigatable getNavigatable() {return null;}
}
