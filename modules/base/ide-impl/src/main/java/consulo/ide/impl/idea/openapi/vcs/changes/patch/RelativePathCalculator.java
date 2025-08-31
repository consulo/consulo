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
package consulo.ide.impl.idea.openapi.vcs.changes.patch;

import consulo.platform.Platform;
import consulo.versionControlSystem.VcsBundle;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;

public class RelativePathCalculator {
  private final int ourNumOfAllowedStepsAbove = 1;
  private static final int ourAllowedStepsDown = 2;

  private final String myShifted;
  private final String myBase;

  private String myResult;
  private boolean myRename;

  public RelativePathCalculator(String base, String shifted) {
    myShifted = shifted;
    myBase = base;
  }

  private static boolean stringEqual(@Nonnull String s1, @Nonnull String s2) {
    return Platform.current().fs().isCaseSensitive() ? s1.equals(s2) : s1.equalsIgnoreCase(s2);
  }

  public void execute() {
    if (myShifted == null || myBase == null) {
      myResult = null;
      return;
    }
    if (stringEqual(myShifted, myBase)) {
      myResult = ".";
      myRename = false;
      return;
    }
    String[] baseParts = split(myBase);
    String[] shiftedParts = split(myShifted);

    myRename = checkRename(baseParts, shiftedParts);

    int cnt = 0;
    while (true) {
      if ((baseParts.length <= cnt) || (shiftedParts.length <= cnt)) {
        // means that directory moved to a file or vise versa -> error
        return;
      }
      if (! stringEqual(baseParts[cnt], shiftedParts[cnt])) {
        break;
      }
      ++ cnt;
    }

    int stepsUp = baseParts.length - cnt - 1;
    if ((! myRename) && (stepsUp > ourNumOfAllowedStepsAbove) && ((shiftedParts.length - cnt) <= ourAllowedStepsDown)) {
      myResult = myShifted;
      return;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < stepsUp; i++) {
      sb.append("../");
    }

    for (int i = cnt; i < shiftedParts.length; i++) {
      String shiftedPart = shiftedParts[i];
      sb.append(shiftedPart);
      if (i < (shiftedParts.length - 1)) {
        sb.append('/');
      }
    }

    myResult = sb.toString();
  }

  public boolean isRename() {
    return myRename;
  }

  private boolean checkRename(String[] baseParts, String[] shiftedParts) {
    if (baseParts.length == shiftedParts.length) {
      for (int i = 0; i < baseParts.length; i++) {
        if (! stringEqual(baseParts[i], shiftedParts[i])) {
          return i == (baseParts.length - 1);
        }
      }
    }
    return false;
  }

  public String getResult() {
    return myResult;
  }

  @Nullable
  public static String getMovedString(String beforeName, String afterName) {
    if ((beforeName != null) && (afterName != null) && (! stringEqual(beforeName, afterName))) {
      RelativePathCalculator calculator = new RelativePathCalculator(beforeName, afterName);
      calculator.execute();
      String key = (calculator.isRename()) ? "change.file.renamed.to.text" : "change.file.moved.to.text";
      return VcsBundle.message(key, calculator.getResult());
    }
    return null;
  }

  public static String[] split(String s) {
    return s.replace(File.separatorChar, '/').split("/");
  }
}
