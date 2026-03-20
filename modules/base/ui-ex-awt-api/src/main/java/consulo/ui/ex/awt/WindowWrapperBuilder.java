package consulo.ui.ex.awt;

import consulo.project.Project;
import consulo.ui.ex.awt.WindowWrapper.Mode;
import consulo.disposer.Disposer;
import consulo.util.lang.StringUtil;

import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class WindowWrapperBuilder {
  
  private final Mode myMode;
  
  private final JComponent myComponent;
  private @Nullable Project myProject;
  private @Nullable Component myParent;
  private @Nullable String myTitle;
  private @Nullable JComponent myPreferredFocusedComponent;
  private @Nullable String myDimensionServiceKey;
  private @Nullable Runnable myOnShowCallback;

  public WindowWrapperBuilder(Mode mode, JComponent component) {
    myMode = mode;
    myComponent = component;
  }

  
  public WindowWrapperBuilder setProject(@Nullable Project project) {
    myProject = project;
    return this;
  }

  
  public WindowWrapperBuilder setParent(@Nullable Component parent) {
    myParent = parent;
    return this;
  }

  
  public WindowWrapperBuilder setTitle(@Nullable String title) {
    myTitle = title;
    return this;
  }

  
  public WindowWrapperBuilder setPreferredFocusedComponent(@Nullable JComponent preferredFocusedComponent) {
    myPreferredFocusedComponent = preferredFocusedComponent;
    return this;
  }

  
  public WindowWrapperBuilder setDimensionServiceKey(@Nullable String dimensionServiceKey) {
    myDimensionServiceKey = dimensionServiceKey;
    return this;
  }

  
  public WindowWrapperBuilder setOnShowCallback(Runnable callback) {
    myOnShowCallback = callback;
    return this;
  }

  
  public WindowWrapper build() {
    switch (myMode) {
      case FRAME:
        return new FrameWindowWrapper(this);
      case MODAL:
      case NON_MODAL:
        return new DialogWindowWrapper(this);
      default:
        throw new IllegalArgumentException(myMode.toString());
    }
  }

  private static class DialogWindowWrapper implements WindowWrapper {
    private final @Nullable Project myProject;
    
    private final JComponent myComponent;
    
    private final Mode myMode;

    
    private final DialogWrapper myDialog;

    public DialogWindowWrapper(WindowWrapperBuilder builder) {
      myProject = builder.myProject;
      myComponent = builder.myComponent;
      myMode = builder.myMode;

      if (builder.myParent != null) {
        myDialog = new MyDialogWrapper(builder.myParent, builder.myComponent, builder.myDimensionServiceKey, builder.myPreferredFocusedComponent);
      }
      else {
        myDialog = new MyDialogWrapper(builder.myProject, builder.myComponent, builder.myDimensionServiceKey, builder.myPreferredFocusedComponent);
      }

      final Runnable onShowCallback = builder.myOnShowCallback;
      if (onShowCallback != null) {
        myDialog.getWindow().addWindowListener(new WindowAdapter() {
          @Override
          public void windowOpened(WindowEvent e) {
            onShowCallback.run();
          }
        });
      }

      setTitle(builder.myTitle);
      switch (builder.myMode) {
        case MODAL:
          myDialog.setModal(true);
          break;
        case NON_MODAL:
          myDialog.setModal(false);
          break;
        default:
          throw new IllegalArgumentException(builder.myMode.toString());
      }
      myDialog.init();
      Disposer.register(myDialog.getDisposable(), this);
    }

    @Override
    public void dispose() {
      Disposer.dispose(myDialog.getDisposable());
    }

    @Override
    public void show() {
      myDialog.show();
    }

    @Override
    public @Nullable Project getProject() {
      return myProject;
    }

    
    @Override
    public JComponent getComponent() {
      return myComponent;
    }

    
    @Override
    public Mode getMode() {
      return myMode;
    }

    
    @Override
    public Window getWindow() {
      return myDialog.getWindow();
    }

    @Override
    public void setTitle(@Nullable String title) {
      myDialog.setTitle(StringUtil.notNullize(title));
    }

    @Override
    public void setImage(@Nullable Image image) {
    }

    @Override
    public void close() {
      myDialog.close(DialogWrapper.CANCEL_EXIT_CODE);
    }

    private static class MyDialogWrapper extends DialogWrapper {
      
      private final JComponent myComponent;
      private final @Nullable String myDimensionServiceKey;
      private final @Nullable JComponent myPreferredFocusedComponent;

      public MyDialogWrapper(@Nullable Project project,
                             JComponent component,
                             @Nullable String dimensionServiceKey,
                             @Nullable JComponent preferredFocusedComponent) {
        super(project, true);
        myComponent = component;
        myDimensionServiceKey = dimensionServiceKey;
        myPreferredFocusedComponent = preferredFocusedComponent;
      }

      public MyDialogWrapper(Component parent,
                             JComponent component,
                             @Nullable String dimensionServiceKey,
                             @Nullable JComponent preferredFocusedComponent) {
        super(parent, true);
        myComponent = component;
        myDimensionServiceKey = dimensionServiceKey;
        myPreferredFocusedComponent = preferredFocusedComponent;
      }

      @Override
      protected @Nullable Border createContentPaneBorder() {
        return null;
      }

      @Override
      protected JComponent createCenterPanel() {
        return myComponent;
      }

      // it is information dialog - no need to OK or Cancel. Close the dialog by clicking the cross button or pressing Esc.
      
      @Override
      protected Action[] createActions() {
        return new Action[0];
      }

      @Override
      protected @Nullable JComponent createSouthPanel() {
        return null;
      }

      @Override
      protected @Nullable String getDimensionServiceKey() {
        return myDimensionServiceKey;
      }

      @Override
      public @Nullable JComponent getPreferredFocusedComponent() {
        return myPreferredFocusedComponent;
      }
    }
  }

  private static class FrameWindowWrapper implements WindowWrapper {
    private final @Nullable Project myProject;
    
    private final JComponent myComponent;
    
    private final Mode myMode;
    private final @Nullable Runnable myOnShowCallback;

    
    private final FrameWrapper myFrame;

    public FrameWindowWrapper(WindowWrapperBuilder builder) {
      myProject = builder.myProject;
      myComponent = builder.myComponent;
      myMode = builder.myMode;
      myOnShowCallback = builder.myOnShowCallback;

      myFrame = new FrameWrapper(builder.myProject, builder.myDimensionServiceKey);

      assert builder.myMode == Mode.FRAME;

      myFrame.setComponent(builder.myComponent);
      myFrame.setPreferredFocusedComponent(builder.myPreferredFocusedComponent);
      myFrame.setTitle(builder.myTitle);
      myFrame.closeOnEsc();
      Disposer.register(myFrame, this);
    }

    @Override
    public void show() {
      myFrame.show();
      if (myOnShowCallback != null) myOnShowCallback.run();
    }

    @Override
    public @Nullable Project getProject() {
      return myProject;
    }

    
    @Override
    public JComponent getComponent() {
      return myComponent;
    }

    
    @Override
    public Mode getMode() {
      return myMode;
    }

    
    @Override
    public Window getWindow() {
      return myFrame.getFrame();
    }

    @Override
    public void setTitle(@Nullable String title) {
      title = StringUtil.notNullize(title);
      myFrame.setTitle(title);

      Window window = getWindow();
      if (window instanceof JFrame) ((JFrame)window).setTitle(title);
      if (window instanceof JDialog) ((JDialog)window).setTitle(title);
    }

    @Override
    public void setImage(@Nullable Image image) {
      myFrame.setImage(image);
    }

    @Override
    public void close() {
      myFrame.close();
    }

    @Override
    public void dispose() {
      Disposer.dispose(myFrame);
    }
  }
}
