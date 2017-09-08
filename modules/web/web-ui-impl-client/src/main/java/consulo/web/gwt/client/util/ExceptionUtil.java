/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.gwt.client.util;

/**
 * @author VISTALL
 * @since 08-Aug-17
 */
public class ExceptionUtil {
  public static String toString(Throwable e) {
    return getMessage(e);
  }

  private static String getMessage(Throwable throwable) {
    String ret = "";
    while (throwable != null) {
      if (throwable instanceof com.google.gwt.event.shared.UmbrellaException) {
        for (Throwable thr2 : ((com.google.gwt.event.shared.UmbrellaException) throwable).getCauses()) {
          if (!ret.equals("")) {
            ret += "\nCaused by: ";
          }
          ret += thr2.toString();
          ret += "\n  at " + getMessage(thr2);
        }
      }
      else if (throwable instanceof com.google.web.bindery.event.shared.UmbrellaException) {
        for (Throwable thr2 : ((com.google.web.bindery.event.shared.UmbrellaException) throwable).getCauses()) {
          if (!ret.equals("")) {
            ret += "\nCaused by: ";
          }
          ret += thr2.toString();
          ret += "\n  at " + getMessage(thr2);
        }
      }
      else {
        if (!ret.equals("")) {
          ret += "\nCaused by: ";
        }
        ret += throwable.toString();
        for (StackTraceElement sTE : throwable.getStackTrace())
          ret += "\n  at " + sTE;
      }
      throwable = throwable.getCause();
    }

    return ret;
  }
}
