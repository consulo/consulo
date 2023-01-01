/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.content;

import consulo.annotation.component.ServiceImpl;
import consulo.content.internal.ContentInternalHelper;
import consulo.project.ProjectBundle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.awt.Messages;
import jakarta.inject.Singleton;

import javax.annotation.Nullable;
import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author VISTALL
 * @since 20-Aug-22
 */
@Singleton
@ServiceImpl
public class ContentInternalHelperImpl implements ContentInternalHelper {
  @Override
  @Nullable
  public String showSpecifyJavadocUrlDialog(JComponent parent, String initialValue) {
    return Messages
            .showInputDialog(parent, ProjectBundle.message("sdk.configure.javadoc.url.prompt"), ProjectBundle.message("sdk.configure.javadoc.url.title"), Messages.getQuestionIcon(), initialValue,
                             new InputValidator() {
                               @RequiredUIAccess
                               @Override
                               public boolean checkInput(String inputString) {
                                 return true;
                               }

                               @RequiredUIAccess
                               @Override
                               public boolean canClose(String inputString) {
                                 try {
                                   new URL(inputString);
                                   return true;
                                 }
                                 catch (MalformedURLException e1) {
                                   Messages.showErrorDialog(e1.getMessage(), ProjectBundle.message("sdk.configure.javadoc.url.title"));
                                 }
                                 return false;
                               }
                             });
  }
}
