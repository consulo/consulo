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
package consulo.logging.internal;

import consulo.util.lang.ExceptionUtil;
import jakarta.annotation.Nullable;

/**
 * @author kir
 */
public class IdeaLoggingEvent {
  private final String myMessage;
  private final Throwable myThrowable;

  public IdeaLoggingEvent(String message, Throwable throwable) {
    myMessage = message;
    myThrowable = throwable;
  }

  public String getMessage() {
    return myMessage;
  }

  public Throwable getThrowable() {
    return myThrowable;
  }

  public String getThrowableText() {
    if (myThrowable == null) return "";
    
    return ExceptionUtil.getThrowableText(myThrowable);
  }

  @Nullable
  public Object getData() {
    return null;
  }

  @Override
  public String toString() {
    return "IdeaLoggingEvent[message=" + myMessage + ", throwable=" + getThrowableText() + "]";
  }
}
