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
package consulo.web.progress.util;

import com.intellij.openapi.progress.util.ProgressWindow;
import consulo.progress.util.ProgressDialog;
import consulo.ui.Label;
import consulo.ui.ProgressBar;
import consulo.ui.Window;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.Size;
import consulo.web.application.WebApplication;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
public class WebProgressDialog implements ProgressDialog {
  private ProgressWindow myProgressWindow;

  private Window myWindow;

  private Label myTextLabel;
  private Label myTextLabel2;
  private ProgressBar myProgressBar;

  public WebProgressDialog(ProgressWindow progressWindow) {
    myProgressWindow = progressWindow;
  }

  @Override
  public void startBlocking() {

  }

  @Override
  public void hide() {
  }

  @Override
  public void background() {

  }

  @Override
  public void update() {
    if (myWindow != null) {
      WebApplication.invokeOnCurrentSession(() -> {
        System.out.println("update " + myProgressWindow.getText() + " " + myProgressWindow.getText2() + " " + myProgressWindow.getFraction());
        myTextLabel.setText(myProgressWindow.getText());
        myTextLabel2.setText(myProgressWindow.getText2());
        myProgressBar.setValue((int)myProgressWindow.getFraction() * 100);
      });
    }
  }

  @Override
  public void show() {
    WebApplication.invokeOnCurrentSession(() -> {
      VerticalLayout verticalLayout = VerticalLayout.create();

      verticalLayout.add(myTextLabel = Label.create());
      verticalLayout.add(myProgressBar = ProgressBar.create());
      verticalLayout.add(myTextLabel2 = Label.create());

      myWindow = Window.createModal("Consulo");
      myWindow.setClosable(false);
      myWindow.setResizable(false);
      myWindow.setSize(new Size(288, 123));
      myWindow.setContent(verticalLayout);
      myWindow.show();
    });
  }

  @Override
  public void runRepaintRunnable() {
    update();
  }

  @Override
  public void changeCancelButtonText(String text) {

  }

  @Override
  public void enableCancelButtonIfNeeded(boolean value) {

  }

  @Override
  public boolean isPopupWasShown() {
    return false;
  }

  @Override
  public void dispose() {
    if (myWindow != null) {
      WebApplication.invokeOnCurrentSession(() -> {
        myWindow.close();
        myWindow = null;
        myTextLabel = null;
        myTextLabel2 = null;
      });
    }
  }
}
