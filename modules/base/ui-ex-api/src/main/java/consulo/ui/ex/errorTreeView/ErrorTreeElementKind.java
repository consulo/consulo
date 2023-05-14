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
package consulo.ui.ex.errorTreeView;

import consulo.ui.ex.MessageCategory;
import consulo.ui.ex.UIBundle;

import jakarta.annotation.Nonnull;

/**
 * @author Eugene Zhuravlev
 * Date: Nov 12, 2004
 */
public enum ErrorTreeElementKind {
  INFO("INFO", UIBundle.message("errortree.information")),
  ERROR("ERROR", UIBundle.message("errortree.error")),
  WARNING("WARNING", UIBundle.message("errortree.warning")),
  NOTE("NOTE", UIBundle.message("errortree.note")),
  GENERIC("GENERIC", "");

  private final String myText;
  private final String myPresentableText;

  private ErrorTreeElementKind(String text, String presentableText) {
    myText = text;
    myPresentableText = presentableText;
  }

  public String toString() {
    return myText; // for debug purposes
  }

  public String getPresentableText() {
    return myPresentableText;
  }

  @Nonnull
  public static ErrorTreeElementKind convertMessageFromCompilerErrorType(int type) {
    switch (type) {
      case MessageCategory.ERROR:
        return ERROR;
      case MessageCategory.WARNING:
        return WARNING;
      case MessageCategory.INFORMATION:
        return INFO;
      case MessageCategory.STATISTICS:
        return INFO;
      case MessageCategory.SIMPLE:
        return GENERIC;
      case MessageCategory.NOTE:
        return NOTE;
      default:
        return GENERIC;
    }
  }}
