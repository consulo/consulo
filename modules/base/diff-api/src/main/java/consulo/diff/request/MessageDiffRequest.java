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
package consulo.diff.request;

import org.jspecify.annotations.Nullable;

import consulo.diff.request.DiffRequest;
import consulo.ui.annotation.RequiredUIAccess;

public class MessageDiffRequest extends DiffRequest {
  private @Nullable String myTitle;
  
  private String myMessage;

  public MessageDiffRequest(String message) {
    this(null, message);
  }

  public MessageDiffRequest(@Nullable String title, String message) {
    myTitle = title;
    myMessage = message;
  }

  @Nullable
  @Override
  public String getTitle() {
    return myTitle;
  }

  
  public String getMessage() {
    return myMessage;
  }

  public void setTitle(@Nullable String title) {
    myTitle = title;
  }

  public void setMessage(String message) {
    myMessage = message;
  }

  @RequiredUIAccess
  @Override
  public final void onAssigned(boolean isAssigned) {
  }
}
