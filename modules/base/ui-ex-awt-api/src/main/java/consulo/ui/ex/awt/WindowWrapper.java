package consulo.ui.ex.awt;

import consulo.project.Project;
import consulo.disposer.Disposable;


import org.jspecify.annotations.Nullable;
import javax.swing.*;
import java.awt.*;

public interface WindowWrapper extends Disposable {
  enum Mode {FRAME, MODAL, NON_MODAL}

  void show();

  @Nullable
  Project getProject();

  
  JComponent getComponent();

  
  Mode getMode();

  
  Window getWindow();

  void setTitle(@Nullable String title);

  void setImage(@Nullable Image image);

  void close();
}
