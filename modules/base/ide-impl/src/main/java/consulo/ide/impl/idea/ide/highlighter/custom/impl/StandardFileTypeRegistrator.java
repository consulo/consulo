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

package consulo.ide.impl.idea.ide.highlighter.custom.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.codeInsight.editorActions.TypedHandler;
import consulo.ide.impl.idea.ide.highlighter.FileTypeRegistrator;
import consulo.ide.impl.idea.openapi.fileTypes.impl.AbstractFileType;
import consulo.language.Commenter;
import consulo.language.Language;
import consulo.language.editor.internal.BraceMatcherInternal;
import consulo.language.custom.CustomSyntaxTableFileType;
import consulo.language.internal.custom.SyntaxTable;
import consulo.language.plain.PlainTextLanguage;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;

@ExtensionImpl
public class StandardFileTypeRegistrator implements FileTypeRegistrator {
  @Override
  public void initFileType(final FileType fileType) {
    if (fileType instanceof AbstractFileType) {
      init(((AbstractFileType)fileType));
    }
  }

  private static void init(final AbstractFileType abstractFileType) {
    SyntaxTable table = abstractFileType.getSyntaxTable();

    if (!isEmpty(table.getStartComment()) && !isEmpty(table.getEndComment()) ||
        !isEmpty(table.getLineComment())) {
      abstractFileType.setCommenter(new MyCommenter(abstractFileType));
    }

    if (table.isHasBraces() || table.isHasBrackets() || table.isHasParens()) {
      BraceMatcherInternal.registerBraceMatcher(abstractFileType, CustomFileTypeBraceMatcher.createBraceMatcher());
    }

    TypedHandler.registerQuoteHandler(abstractFileType, new CustomFileTypeQuoteHandler(abstractFileType));

  }

  private static class MyCommenter implements Commenter {
    private final CustomSyntaxTableFileType myAbstractFileType;

    public MyCommenter(final CustomSyntaxTableFileType abstractFileType) {

      myAbstractFileType = abstractFileType;
    }

    @Override
    public String getLineCommentPrefix() {
      return myAbstractFileType.getSyntaxTable().getLineComment();
    }

    @Override
    public String getBlockCommentPrefix() {
      return myAbstractFileType.getSyntaxTable().getStartComment();
    }

    @Override
    public String getBlockCommentSuffix() {
      return myAbstractFileType.getSyntaxTable().getEndComment();
    }

    @Override
    public String getCommentedBlockCommentPrefix() {
      return null;
    }

    @Override
    public String getCommentedBlockCommentSuffix() {
      return null;
    }

    @Nonnull
    @Override
    public Language getLanguage() {
      return PlainTextLanguage.INSTANCE;
    }
  }

  private static boolean isEmpty(String str) {
    return str==null || str.length() == 0;
  }

}
