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
import consulo.util.lang.StringUtil;

public class LogMessage extends AbstractMessage {

  static final String NO_MESSAGE = "No message";

  private String myHeader = NO_MESSAGE;
  private final Throwable myThrowable;

  public LogMessage(String header, Throwable throwable) {
    myThrowable = throwable;
    myHeader = header;
  }

  public LogMessage(IdeaLoggingEvent aEvent) {
    super();

    myThrowable = aEvent.getThrowable();

    if (StringUtil.isNotEmpty(aEvent.getMessage())) {
      myHeader = aEvent.getMessage();
    }

    if (myThrowable != null && StringUtil.isNotEmpty(myThrowable.getMessage())) {
      if (!myHeader.equals(NO_MESSAGE)) {
        if (!myHeader.endsWith(": ") && !myHeader.endsWith(":")) {
          myHeader += ": ";
        }
        myHeader += myThrowable.getMessage();
      }
      else {
        myHeader = myThrowable.getMessage();
      }
    }
  }

  @Override
  public Throwable getThrowable() {
    return myThrowable;
  }

  @Override
  public String getMessage() {
    return myHeader;
  }

  @Override
  public String getThrowableText() {
    return ExceptionUtil.getThrowableText(getThrowable());
  }
}
