/*
 * Copyright 2013-2016 consulo.io
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

import com.intellij.idea.ApplicationStarter;
import com.intellij.idea.starter.ApplicationPostStarter;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.SdkImpl;
import consulo.annotations.Internal;
import consulo.start.CommandLineArgs;

/**
 * @author VISTALL
 * @since 15-May-16
 *
 * Used via reflection
 * @see com.intellij.idea.ApplicationStarter#getStarterClass(boolean, boolean)
 */
@SuppressWarnings("unused")
@Internal
public class WebPostStarter extends ApplicationPostStarter {
  public WebPostStarter(ApplicationStarter application) {
  }

  @Override
  public void main(CommandLineArgs args) {
    /*ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      @Override
      public void run() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          @Override
          public void run() {
            setupSdk("JDK", "1.6", "C:\\Program Files\\Java\\jdk1.6.0_45");
          }
        });
      }
    }, ModalityState.any());  */

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
