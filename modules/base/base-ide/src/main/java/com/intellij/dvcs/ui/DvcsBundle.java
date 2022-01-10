/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.dvcs.ui;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.PropertyKey;

/**
 * @author VISTALL
 * @since 29.04.14
 */
public class DvcsBundle extends AbstractBundle {
  private static final DvcsBundle ourInstance = new DvcsBundle();

  private DvcsBundle() {
    super("messages.DvcsBundle");
  }

  public static String message(@PropertyKey(resourceBundle = "messages.DvcsBundle") String key) {
    return ourInstance.getMessage(key);
  }

  public static String message(@PropertyKey(resourceBundle = "messages.DvcsBundle") String key, Object... params) {
    return ourInstance.getMessage(key, params);
  }
}
