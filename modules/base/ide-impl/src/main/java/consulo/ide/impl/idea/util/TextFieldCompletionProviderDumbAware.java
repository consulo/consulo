package consulo.ide.impl.idea.util;

import consulo.application.dumb.DumbAware;

/**
 * @author sergey.evdokimov
 */
public abstract class TextFieldCompletionProviderDumbAware extends TextFieldCompletionProvider implements DumbAware {

  protected TextFieldCompletionProviderDumbAware() {
  }

  protected TextFieldCompletionProviderDumbAware(boolean caseInsensitivity) {
    super(caseInsensitivity);
  }
}
