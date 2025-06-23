/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.annotation.DeprecationInfo;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@Deprecated
@DeprecationInfo("Use Alert/Alerts class from ui-api")
@SuppressWarnings("ALL")
public abstract class MessageDialogBuilder<T extends MessageDialogBuilder> {
  protected final String myMessage;
  protected final String myTitle;

  protected String myYesText;
  protected String myNoText;

  protected Project myProject;
  protected Image myIcon;
  protected DialogWrapper.DoNotAskOption myDoNotAskOption;

  private MessageDialogBuilder(@Nonnull String title, @Nonnull String message) {
    myTitle = title;
    myMessage = message;
  }

  @Nonnull
  public static YesNo yesNo(@Nonnull String title, @Nonnull String message) {
    return new YesNo(title, message).icon(Messages.getQuestionIcon());
  }

  public static YesNoCancel yesNoCancel(@Nonnull String title, @Nonnull String message) {
    return new YesNoCancel(title, message).icon(Messages.getQuestionIcon());
  }

  protected abstract T getThis();

  @Nonnull
  public T project(@Nullable Project project) {
    myProject = project;
    return getThis();
  }

  /**
   * @see {@link Messages#getInformationIcon()}
   * @see {@link Messages#getWarningIcon()}
   * @see {@link Messages#getErrorIcon()}
   * @see {@link Messages#getQuestionIcon()}
   */
  public T icon(@Nullable Image icon) {
    myIcon = icon;
    return getThis();
  }

  @Nonnull
  public T doNotAsk(@Nonnull DialogWrapper.DoNotAskOption doNotAskOption) {
    myDoNotAskOption = doNotAskOption;
    return getThis();
  }

  public T yesText(@Nonnull String yesText) {
    myYesText = yesText;
    return getThis();
  }

  public T noText(@Nonnull String noText) {
    myNoText = noText;
    return getThis();
  }

  public static final class YesNo extends MessageDialogBuilder<YesNo> {
    private YesNo(@Nonnull String title, @Nonnull String message) {
      super(title, message);
    }

    @Override
    protected YesNo getThis() {
      return this;
    }

    @Messages.YesNoResult
    public int show() {
      String yesText = ObjectUtil.chooseNotNull(myYesText, Messages.YES_BUTTON);
      String noText = ObjectUtil.chooseNotNull(myNoText, Messages.NO_BUTTON);

      return Messages.showDialog(myProject, myMessage, myTitle, new String[]{yesText, noText}, 0, myIcon, myDoNotAskOption) == 0 ? Messages.YES : Messages.NO;
    }

    public boolean isYes() {
      return show() == Messages.YES;
    }
  }

  public static final class YesNoCancel extends MessageDialogBuilder<YesNoCancel> {
    private String myCancelText;

    private YesNoCancel(@Nonnull String title, @Nonnull String message) {
      super(title, message);
    }

    public YesNoCancel cancelText(@Nonnull String cancelText) {
      myCancelText = cancelText;
      return getThis();
    }

    @Override
    protected YesNoCancel getThis() {
      return this;
    }

    @Messages.YesNoCancelResult
    public int show() {
      String yesText = ObjectUtil.chooseNotNull(myYesText, Messages.YES_BUTTON);
      String noText = ObjectUtil.chooseNotNull(myNoText, Messages.NO_BUTTON);
      String cancelText = ObjectUtil.chooseNotNull(myCancelText, Messages.CANCEL_BUTTON);

      int buttonNumber = Messages.showDialog(myProject, myMessage, myTitle, new String[]{yesText, noText, cancelText}, 0, myIcon, myDoNotAskOption);
      return buttonNumber == 0 ? Messages.YES : buttonNumber == 1 ? Messages.NO : Messages.CANCEL;
    }
  }
}
