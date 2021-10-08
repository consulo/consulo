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
 * @since 2020-07-30
 */
abstract class BaseLocalizeValue implements LocalizeValue {
  protected static final Object[] ourEmptyArgs = new Object[0];

  protected final Object[] myArgs;

  private LocalizeManager myLocalizeManager;

  private String myText;

  private long myModificationCount = -1;

  BaseLocalizeValue(Object... args) {
    myArgs = args;
  }

  @Nonnull
  private LocalizeManager getLocalizeManager() {
    if (myLocalizeManager == null) {
      myLocalizeManager = LocalizeManager.get();
    }

    return myLocalizeManager;
  }

  @Override
  public long getModificationCount() {
    return myModificationCount;
  }

  @Nonnull
  protected abstract String getUnformattedText(@Nonnull LocalizeManager localizeManager);

  @Nonnull
  protected String calcValue(LocalizeManager manager) {
    String newText;
    String unformattedText = getUnformattedText(manager);
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

    return newText;
  }

  @Nonnull
  @Override
  public String getValue() {
    LocalizeManager manager = getLocalizeManager();
    if (myModificationCount == manager.getModificationCount()) {
      return myText;
    }

    String newText = calcValue(manager);

    myText = newText;
    myModificationCount = manager.getModificationCount();
    return newText;
  }
}
