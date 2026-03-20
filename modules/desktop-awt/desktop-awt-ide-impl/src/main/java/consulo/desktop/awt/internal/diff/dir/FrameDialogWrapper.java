package consulo.desktop.awt.internal.diff.dir;

import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.FrameWrapper;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

public abstract class FrameDialogWrapper {
  public enum Mode {FRAME, MODAL, NON_MODAL}

  
  protected abstract JComponent getPanel();

  protected @Nullable String getDimensionServiceKey() {
    return null;
  }

  protected @Nullable JComponent getPreferredFocusedComponent() {
    return null;
  }

  protected @Nullable String getTitle() {
    return null;
  }

  protected @Nullable Project getProject() {
    return null;
  }

  
  protected Mode getMode() {
    return Mode.MODAL;
  }

  protected @Nullable Disposable getDisposable() {
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

    public MyDialogWrapper(@Nullable Project project,
                           Mode mode,
                           JComponent component,
                           @Nullable JComponent preferredFocusedComponent,
                           @Nullable String title,
                           @Nullable String dimensionServiceKey,
                           @Nullable Disposable disposable) {
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

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
      return myPreferredFocusedComponent;
    }

    @Override
    protected @Nullable String getDimensionServiceKey() {
      return myDimensionServiceKey;
    }

    // it is information dialog - no need to OK or Cancel. Close the dialog by clicking the cross button or pressing Esc.
    
    @Override
    protected Action[] createActions() {
      return new Action[0];
    }
  }

  private static class MyFrameWrapper extends FrameWrapper {
    public MyFrameWrapper(@Nullable Project project,
                          Mode mode,
                          JComponent component,
                          @Nullable JComponent preferredFocusedComponent,
                          @Nullable String title,
                          @Nullable String dimensionServiceKey,
                          @Nullable Disposable disposable) {
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
