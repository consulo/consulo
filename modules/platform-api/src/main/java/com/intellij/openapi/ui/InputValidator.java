/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.ui;

import consulo.annotations.RequiredDispatchThread;

/**
 * The validator for input dialogs.
 *
 * @see Messages#showInputDialog(String, String, javax.swing.Icon, String, InputValidator)
 * @see Messages#showInputDialog(java.awt.Component, String, String, javax.swing.Icon, String, InputValidator)
 * @see Messages#showInputDialog(com.intellij.openapi.project.Project, String, String, javax.swing.Icon, String, InputValidator)
 * @see Messages#showEditableChooseDialog(String, String, javax.swing.Icon, String[], String, InputValidator)
 */
public interface InputValidator {
  /**
   * Checks whether the <code>inputString</code> is valid. It is invoked each time
   * input changes.
   *
   * @param inputString the input to check
   * @return true if input string is valid
   */
  @RequiredDispatchThread
  boolean checkInput(String inputString);

  /**
   * This method is invoked just before message dialog is closed with OK code.
   * If <code>false</code> is returned then then the message dialog will not be closed.
   *
   * @param inputString the input to check
   * @return true if the dialog could be closed, false otherwhise.
   */
  @RequiredDispatchThread
  boolean canClose(String inputString);
}
