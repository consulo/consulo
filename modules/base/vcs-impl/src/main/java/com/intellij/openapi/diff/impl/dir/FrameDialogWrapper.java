package com.intellij.openapi.diff.impl.dir;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.FrameWrapper;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;

public abstract class FrameDialogWrapper {
  public enum Mode {FRAME, MODAL, NON_MODAL}

  @Nonnull
  protected abstract JComponent getPanel();

  @Nullable
  protected String getDimensionServiceKey() {
    return null;
  }

  @javax.annotation.Nullable
  protected JComponent getPreferredFocusedComponent() {
    return null;
  }

  @javax.annotation.Nullable
  protected String getTitle() {
    return null;
  }

  @javax.annotation.Nullable
  protected Project getProject() {
    return null;
  }

  @Nonnull
  protected Mode getMode() {
    return Mode.MODAL;
  }

  @Nullable
  protected Disposable getDisposable() {
    return null;
  }

  public void show() {
    switch (getMode()) {
      case FRAME:
        new MyFrameWrapper(getProject(), getMode(), getPanel(), getPreferredFocusedComponent(), getTitle(), getDimensionServiceKey(),
                           getDisposable())
                .show();
        return;
      case MODAL:
      case NON_MODAL:
        new MyDialogWrapper(getProject(), getMode(), getPanel(), getPreferredFocusedComponent(), getTitle(), getDimensionServiceKey(),
                            getDisposable())
                .show();
        return;
      default:
        throw new IllegalArgumentException(getMode().toString());
    }
  }

  private static class MyDialogWrapper extends DialogWrapper {
    private final JComponent myComponent;
    private final JComponent myPreferredFocusedComponent;
    private final String myDimensionServiceKey;

    public MyDialogWrapper(@javax.annotation.Nullable Project project,
                           @Nonnull Mode mode,
                           @Nonnull JComponent component,
                           @javax.annotation.Nullable JComponent preferredFocusedComponent,
                           @Nullable String title,
                           @javax.annotation.Nullable String dimensionServiceKey,
                           @javax.annotation.Nullable Disposable disposable) {
      super(project, true);
      myComponent = component;
      myPreferredFocusedComponent = preferredFocusedComponent;
      myDimensionServiceKey = dimensionServiceKey;

      if (title != null) {
        setTitle(title);
      }
      switch (mode) {
        case MODAL:
          setModal(true);
          break;
        case NON_MODAL:
          setModal(false);
          break;
        default:
          throw new IllegalArgumentException(mode.toString());
      }

      if (disposable != null) {
        Disposer.register(getDisposable(), disposable);
      }

      init();
    }

    @Override
    protected JComponent createCenterPanel() {
      return myComponent;
    }

    @javax.annotation.Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
      return myPreferredFocusedComponent;
    }

    @javax.annotation.Nullable
    @Override
    protected String getDimensionServiceKey() {
      return myDimensionServiceKey;
    }

    // it is information dialog - no need to OK or Cancel. Close the dialog by clicking the cross button or pressing Esc.
    @Nonnull
    @Override
    protected Action[] createActions() {
      return new Action[0];
    }
  }

  private static class MyFrameWrapper extends FrameWrapper {
    public MyFrameWrapper(@javax.annotation.Nullable Project project,
                          @Nonnull Mode mode,
                          @Nonnull JComponent component,
                          @Nullable JComponent preferredFocusedComponent,
                          @javax.annotation.Nullable String title,
                          @javax.annotation.Nullable String dimensionServiceKey,
                          @javax.annotation.Nullable Disposable disposable) {
      super(project, dimensionServiceKey);

      assert mode == Mode.FRAME;

      if (title != null) {
        setTitle(title);
      }
      setComponent(component);
      setPreferredFocusedComponent(preferredFocusedComponent);
      closeOnEsc();
      if (disposable != null) {
        addDisposable(disposable);
      }
    }
  }
}
