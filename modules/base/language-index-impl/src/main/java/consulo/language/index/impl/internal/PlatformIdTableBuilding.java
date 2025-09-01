/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.index.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.HighlighterIterator;
import consulo.index.io.DataIndexer;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.custom.CustomSyntaxTableFileType;
import consulo.language.editor.highlight.HighlighterFactory;
import consulo.language.editor.internal.EditorHighlighterCache;
import consulo.language.file.LanguageFileType;
import consulo.language.internal.SubstitutedFileType;
import consulo.language.internal.custom.CustomHighlighterTokenType;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.search.IndexPattern;
import consulo.language.psi.stub.BaseFilterLexer;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.IndexPatternUtil;
import consulo.language.psi.stub.OccurrenceConsumer;
import consulo.language.psi.stub.todo.TodoIndexEntry;
import consulo.language.psi.stub.todo.TodoIndexer;
import consulo.language.psi.stub.todo.VersionedTodoIndexer;
import consulo.language.util.CommentUtilCore;
import consulo.language.version.LanguageVersion;
import consulo.language.version.LanguageVersionUtil;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author dmitrylomov
 */
public class PlatformIdTableBuilding {
  private static final TokenSet ABSTRACT_FILE_COMMENT_TOKENS = TokenSet.create(CustomHighlighterTokenType.LINE_COMMENT, CustomHighlighterTokenType.MULTI_LINE_COMMENT);

  private PlatformIdTableBuilding() {
  }

  @Nullable
  @RequiredReadAction
  public static DataIndexer<TodoIndexEntry, Integer, FileContent> getTodoIndexer(
    FileType fileType,
    Project project,
    VirtualFile virtualFile
  ) {
    DataIndexer<TodoIndexEntry, Integer, FileContent> extIndexer;
    if (fileType instanceof SubstitutedFileType && !((SubstitutedFileType)fileType).isSameFileType()) {
      SubstitutedFileType sft = (SubstitutedFileType)fileType;
      extIndexer = new CompositeTodoIndexer(
        fileType,
        getTodoIndexer(sft.getOriginalFileType(), project, virtualFile),
        getTodoIndexer(sft.getFileType(), project, virtualFile)
      );
    }
    else {
      extIndexer = TodoIndexer.forFileType(fileType);
    }
    if (extIndexer != null) {
      return extIndexer;
    }

    if (fileType instanceof LanguageFileType) {
      Language lang = ((LanguageFileType)fileType).getLanguage();
      ParserDefinition parserDef = ParserDefinition.forLanguage(lang);
      LanguageVersion languageVersion = LanguageVersionUtil.findLanguageVersion(lang, project, virtualFile);
      TokenSet commentTokens = parserDef != null ? parserDef.getCommentTokens(languageVersion) : null;
      if (commentTokens != null) {
        return new TokenSetTodoIndexer(commentTokens, languageVersion, virtualFile, project);
      }
    }

    if (fileType instanceof CustomSyntaxTableFileType) {
      return new TokenSetTodoIndexer(ABSTRACT_FILE_COMMENT_TOKENS, null, virtualFile, project);
    }

    return null;
  }

  public static boolean isTodoIndexerRegistered(@Nonnull FileType fileType) {
    return TodoIndexer.forFileType(fileType) != null;
  }

  private static class CompositeTodoIndexer implements VersionedTodoIndexer {
    private final DataIndexer<TodoIndexEntry, Integer, FileContent>[] myIndexers;
    private final FileType myFileType;

    @SafeVarargs
    public CompositeTodoIndexer(@Nonnull FileType fileType, @Nonnull DataIndexer<TodoIndexEntry, Integer, FileContent>... indexers) {
      myFileType = fileType;
      myIndexers = indexers;
    }

    @Nonnull
    @Override
    public FileType getFileType() {
      return myFileType;
    }

    @Nonnull
    @Override
    public Map<TodoIndexEntry, Integer> map(FileContent inputData) {
      Map<TodoIndexEntry, Integer> result = new HashMap<>();
      for (DataIndexer<TodoIndexEntry, Integer, FileContent> indexer : myIndexers) {
        for (Map.Entry<TodoIndexEntry, Integer> entry : indexer.map(inputData).entrySet()) {
          TodoIndexEntry key = entry.getKey();
          if (result.containsKey(key)) {
            result.put(key, result.get(key) + entry.getValue());
          }
          else {
            result.put(key, entry.getValue());
          }
        }
      }
      return result;
    }

    @Override
    public int getVersion() {
      int version = VersionedTodoIndexer.super.getVersion();
      for (DataIndexer dataIndexer : myIndexers) {
        version += dataIndexer instanceof VersionedTodoIndexer ? ((VersionedTodoIndexer)dataIndexer).getVersion() : 0xFF;
      }
      return version;
    }
  }

  private static class TokenSetTodoIndexer implements DataIndexer<TodoIndexEntry, Integer, FileContent> {
    @Nonnull
    private final TokenSet myCommentTokens;
    @Nullable
    private final LanguageVersion myLanguageVersion;
    private final VirtualFile myFile;
    private final Project myProject;

    public TokenSetTodoIndexer(@Nonnull TokenSet commentTokens, @Nullable LanguageVersion languageVersion, @Nonnull VirtualFile file, @Nullable Project project) {
      myCommentTokens = commentTokens;
      myLanguageVersion = languageVersion;
      myFile = file;
      myProject = project;
    }

    @Override
    @Nonnull
    @RequiredReadAction
    public Map<TodoIndexEntry, Integer> map(FileContent inputData) {
      if (IndexPatternUtil.getIndexPatternCount() > 0) {
        CharSequence chars = inputData.getContentAsText();
        OccurrenceConsumer occurrenceConsumer = new OccurrenceConsumer(null, true);
        EditorHighlighter highlighter;

        EditorHighlighter editorHighlighter = inputData.getUserData(EditorHighlighter.KEY);
        if (editorHighlighter != null && EditorHighlighterCache.checkCanUseCachedEditorHighlighter(chars, editorHighlighter)) {
          highlighter = editorHighlighter;
        }
        else {
          highlighter = HighlighterFactory.createHighlighter(null, myFile);
          highlighter.setText(chars);
        }

        int documentLength = chars.length();
        BaseFilterLexer.TodoScanningState todoScanningState = null;
        HighlighterIterator iterator = highlighter.createIterator(0);

        Map<Language, LanguageVersion> languageVersionCache = new LinkedHashMap<>();
        if (myLanguageVersion != null) {
          languageVersionCache.put(myLanguageVersion.getLanguage(), myLanguageVersion);
        }

        while (!iterator.atEnd()) {
          IElementType token = (IElementType)iterator.getTokenType();

          if (myCommentTokens.contains(token) || isCommentToken(languageVersionCache, token)) {
            int start = iterator.getStart();
            if (start >= documentLength) break;
            int end = iterator.getEnd();

            todoScanningState = BaseFilterLexer.advanceTodoItemsCount(chars.subSequence(start, Math.min(end, documentLength)), occurrenceConsumer, todoScanningState);
            if (end > documentLength) break;
          }
          iterator.advance();
        }
        Map<TodoIndexEntry, Integer> map = new HashMap<>();
        for (IndexPattern pattern : IndexPatternUtil.getIndexPatterns()) {
          int count = occurrenceConsumer.getOccurrenceCount(pattern);
          if (count > 0) {
            map.put(new TodoIndexEntry(pattern.getPatternString(), pattern.isCaseSensitive()), count);
          }
        }
        return map;
      }
      return Collections.emptyMap();
    }

    @RequiredReadAction
    private boolean isCommentToken(Map<Language, LanguageVersion> cache, IElementType token) {
      Language language = token.getLanguage();
      LanguageVersion languageVersion = cache.get(language);
      if (languageVersion == null) {
        cache.put(language, languageVersion = LanguageVersionUtil.findLanguageVersion(language, myProject, myFile));
      }
      return CommentUtilCore.isCommentToken(token, languageVersion);
    }
  }
}
