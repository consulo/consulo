/*
 * Copyright 2013-2020 consulo.io
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
package consulo.localize;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-05-20
 */
final class DefaultLocalizeValue implements LocalizeValue {
  private final LocalizeKey myLocalizeKey;
  private final Object[] myArgs;

  private LocalizeManager myLocalizeManager;

  private String myText;

  private int myModificationCount = -1;

  DefaultLocalizeValue(LocalizeKey localizeKey, Object... args) {
    myLocalizeKey = localizeKey;
    myArgs = args;
  }

  @Nonnull
  private LocalizeManager getLocalizeManager() {
    if (myLocalizeManager == null) {
      myLocalizeManager = LocalizeManager.getInstance();
    }

    return myLocalizeManager;
  }

  @Nonnull
  private String value() {
    LocalizeManager manager = getLocalizeManager();
    if (myModificationCount == manager.getModificationCount()) {
      return myText;
    }

    String newText = null;
    String unformattedText = manager.getUnformattedText(myLocalizeKey);
    if (myArgs.length > 0) {

      Object[] args = new Object[myArgs.length];
      // change LocalizeValue if found in args
      for (int i = 0; i < myArgs.length; i++) {
        Object oldValue = myArgs[i];

        args[i] = oldValue instanceof LocalizeValue ? ((LocalizeValue)oldValue).getValue() : oldValue;
      }

      newText = manager.formatText(unformattedText, args);
    }
    else {
      newText = unformattedText;
    }

    myText = newText;
    myModificationCount = manager.getModificationCount();
    return unformattedText;
  }

  @Nonnull
  @Override
  public String getValue() {
    return value();
  }
}
