/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.CodeDocumentationAwareCommenter;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiComment;

/**
 * @author Alexander Podkhalyuzin
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface CommentCompleteHandler {
  ExtensionPointName<CommentCompleteHandler> EP_NAME = ExtensionPointName.create(CommentCompleteHandler.class);

  boolean isCommentComplete(PsiComment comment, CodeDocumentationAwareCommenter commenter, Editor editor);

  boolean isApplicable(PsiComment comment, CodeDocumentationAwareCommenter commenter);
}
