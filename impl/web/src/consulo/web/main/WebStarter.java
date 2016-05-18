/*
 * Copyright 2013-2016 must-be.org
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
package consulo.web.main;

import com.intellij.idea.IdeaApplication;
import com.intellij.idea.starter.ApplicationStarter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.SdkImpl;
import com.intellij.util.ThreeState;
import consulo.web.AppInit;

/**
 * @author VISTALL
 * @see com.intellij.idea.IdeaApplication#getStarterClass(boolean, boolean)
 * @since 15-May-16
 * <p/>
 * Used via reflection
 */
public class WebStarter extends ApplicationStarter {
  public WebStarter(IdeaApplication application) {
  }

  @Override
  public void main(String[] args) {
    AppInit.inited = ThreeState.YES;

    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      @Override
      public void run() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          @Override
          public void run() {
          //  setupSdk("JDK", "1.6", "C:\\Program Files\\Java\\jdk1.6.0_45");
          }
        });
      }
    }, ModalityState.any());

    System.out.println("We started");
  }

  private static void setupSdk(String sdkTypeName, String name, String home) {
    SdkType sdkType = null;
    for (SdkType temp : SdkType.EP_NAME.getExtensions()) {
      if (temp.getName().equals(sdkTypeName)) {
        sdkType = temp;
        break;
      }
    }

    assert sdkType != null;
    SdkImpl sdk = new SdkImpl(name, sdkType, home, sdkType.getVersionString(home));

    sdkType.setupSdkPaths(sdk);

    SdkTable.getInstance().addSdk(sdk);
  }
}
