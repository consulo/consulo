package consulo.application.progress;

import consulo.application.Application;
import consulo.localize.LocalizeValue;
import consulo.ui.ModalityState;

import jakarta.annotation.Nonnull;

public class DumbProgressIndicator implements StandardProgressIndicator {
  public static final DumbProgressIndicator INSTANCE = new DumbProgressIndicator();

  @Override
  public void start() {
  }

  @Override
  public void stop() {
  }

  @Override
  public boolean isRunning() {
    return true;
  }

  @Override
  public final void cancel() {
  }

  @Override
  public final boolean isCanceled() {
    return false;
  }

  @Override
  public final void checkCanceled() {
  }

  @Override
  public void setTextValue(LocalizeValue text) {
  }

  @Override
  public LocalizeValue getTextValue() {
    return LocalizeValue.empty();
  }

  @Override
  public void setText2Value(LocalizeValue text) {
  }

  @Override
  public LocalizeValue getText2Value() {
    return LocalizeValue.empty();
  }

  @Override
  public double getFraction() {
    return 0;
  }

  @Override
  public void setFraction(double fraction) {
  }

  @Override
  public void pushState() {
  }

  @Override
  public void popState() {
  }

  @Override
  public void startNonCancelableSection() {
  }

  @Override
  public void finishNonCancelableSection() {
  }

  @Override
  public boolean isModal() {
    return false;
  }

  @Override
  @Nonnull
  public ModalityState getModalityState() {
    return Application.get().getNoneModalityState();
  }

  @Override
  public void setModalityProgress(ProgressIndicator modalityProgress) {
  }

  @Override
  public boolean isIndeterminate() {
    return false;
  }

  @Override
  public void setIndeterminate(boolean indeterminate) {
  }

  @Override
  public boolean isPopupWasShown() {
    return false;
  }

  @Override
  public boolean isShowing() {
    return false;
  }
}
