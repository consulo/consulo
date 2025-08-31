// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.search;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.ApplicationManager;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiFile;
import consulo.application.util.query.Query;
import consulo.application.util.query.QueryFactory;
import jakarta.annotation.Nonnull;

/**
 * Allows to search for occurrences of specified regular expressions in the comments of source files.
 *
 * @author yole
 * @see IndexPatternProvider
 * @see PsiTodoSearchHelper#findFilesWithTodoItems()
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class IndexPatternSearch extends QueryFactory<IndexPatternOccurrence, IndexPatternSearch.SearchParameters> {
  private static IndexPatternSearch ourInstance;

  private static IndexPatternSearch getInstance() {
    IndexPatternSearch result = ourInstance;
    if (result == null) {
      result = ApplicationManager.getApplication().getComponent(IndexPatternSearch.class);
      ourInstance = result;
    }
    return result;
  }

  public static final class SearchParameters {
    private final PsiFile myFile;
    private final IndexPattern myPattern;
    private final IndexPatternProvider myPatternProvider;
    private final TextRange myRange;
    private final boolean myMultiLine;

    public SearchParameters(@Nonnull PsiFile file, @Nonnull IndexPattern pattern) {
      this(file, pattern, null);
    }

    public SearchParameters(@Nonnull PsiFile file, @Nonnull IndexPattern pattern, TextRange range) {
      myFile = file;
      myRange = range;
      myPatternProvider = null;
      myPattern = pattern;
      myMultiLine = false;
    }

    public SearchParameters(@Nonnull PsiFile file, @Nonnull IndexPatternProvider patternProvider) {
      this(file, patternProvider, null, false);
    }

    public SearchParameters(@Nonnull PsiFile file, @Nonnull IndexPatternProvider patternProvider, boolean multiLine) {
      this(file, patternProvider, null, multiLine);
    }

    public SearchParameters(@Nonnull PsiFile file, @Nonnull IndexPatternProvider patternProvider, TextRange range) {
      this(file, patternProvider, range, false);
    }

    private SearchParameters(@Nonnull PsiFile file, @Nonnull IndexPatternProvider patternProvider, TextRange range, boolean multiLine) {
      myFile = file;
      myPatternProvider = patternProvider;
      myRange = range;
      myPattern = null;
      myMultiLine = multiLine;
    }

    @Nonnull
    public PsiFile getFile() {
      return myFile;
    }

    public IndexPattern getPattern() {
      return myPattern;
    }

    public IndexPatternProvider getPatternProvider() {
      return myPatternProvider;
    }

    public TextRange getRange() {
      return myRange;
    }

    public boolean isMultiLine() {
      return myMultiLine;
    }
  }

  /**
   * Returns a query which can be used to process occurrences of the specified pattern
   * in the specified file. The query is executed by parsing the contents of the file.
   *
   * @param file    the file in which occurrences should be searched.
   * @param pattern the pattern to search for.
   * @return the query instance.
   */
  @Nonnull
  public static Query<IndexPatternOccurrence> search(@Nonnull PsiFile file, @Nonnull IndexPattern pattern) {
    SearchParameters parameters = new SearchParameters(file, pattern);
    return getInstance().createQuery(parameters);
  }

  /**
   * Returns a query which can be used to process occurrences of the specified pattern
   * in the specified text range. The query is executed by parsing the contents of the file.
   *
   * @param file        the file in which occurrences should be searched.
   * @param pattern     the pattern to search for.
   * @param startOffset the start offset of the range to search.
   * @param endOffset   the end offset of the range to search.
   * @return the query instance.
   */
  @Nonnull
  public static Query<IndexPatternOccurrence> search(@Nonnull PsiFile file, @Nonnull IndexPattern pattern, int startOffset, int endOffset) {
    SearchParameters parameters = new SearchParameters(file, pattern, new TextRange(startOffset, endOffset));
    return getInstance().createQuery(parameters);
  }

  /**
   * Returns a query which can be used to process occurrences of any pattern from the
   * specified provider in the specified file. The query is executed by parsing the
   * contents of the file.
   *
   * @param file            the file in which occurrences should be searched.
   * @param patternProvider the provider the patterns from which are searched.
   * @return the query instance.
   */
  @Nonnull
  public static Query<IndexPatternOccurrence> search(@Nonnull PsiFile file, @Nonnull IndexPatternProvider patternProvider) {
    SearchParameters parameters = new SearchParameters(file, patternProvider);
    return getInstance().createQuery(parameters);
  }

  /**
   * Returns a query which can be used to process occurrences of any pattern from the
   * specified provider in the specified file. The query is executed by parsing the
   * contents of the file.
   *
   * @param file                 the file in which occurrences should be searched.
   * @param patternProvider      the provider the patterns from which are searched.
   * @param multiLineOccurrences whether continuation of occurrences on following lines should be detected
   *                             (will be returned as {@link IndexPatternOccurrence#getAdditionalTextRanges()}
   * @return the query instance.
   */
  @Nonnull
  public static Query<IndexPatternOccurrence> search(@Nonnull PsiFile file, @Nonnull IndexPatternProvider patternProvider, boolean multiLineOccurrences) {
    SearchParameters parameters = new SearchParameters(file, patternProvider, multiLineOccurrences);
    return getInstance().createQuery(parameters);
  }

  /**
   * Returns a query which can be used to process occurrences of any pattern from the
   * specified provider in the specified text range. The query is executed by parsing the
   * contents of the file.
   *
   * @param file            the file in which occurrences should be searched.
   * @param patternProvider the provider the patterns from which are searched.
   * @param startOffset     the start offset of the range to search.
   * @param endOffset       the end offset of the range to search.
   * @return the query instance.
   */
  @Nonnull
  public static Query<IndexPatternOccurrence> search(@Nonnull PsiFile file, @Nonnull IndexPatternProvider patternProvider, int startOffset, int endOffset) {
    SearchParameters parameters = new SearchParameters(file, patternProvider, new TextRange(startOffset, endOffset));
    return getInstance().createQuery(parameters);
  }

  /**
   * Returns the number of occurrences of any pattern from the specified provider
   * in the specified file. The returned value is taken from the index, and the file
   * is not parsed.
   *
   * @param file            the file in which occurrences should be searched.
   * @param patternProvider the provider the patterns from which are searched.
   * @return the number of pattern occurrences.
   */
  public static int getOccurrencesCount(@Nonnull PsiFile file, @Nonnull IndexPatternProvider patternProvider) {
    return getInstance().getOccurrencesCountImpl(file, patternProvider);
  }

  /**
   * Returns the number of occurrences of the specified pattern
   * in the specified file. The returned value is taken from the index, and the file
   * is not parsed.
   *
   * @param file    the file in which occurrences should be searched.
   * @param pattern the pattern to search for.
   * @return the number of pattern occurrences.
   */
  public static int getOccurrencesCount(@Nonnull PsiFile file, @Nonnull IndexPattern pattern) {
    return getInstance().getOccurrencesCountImpl(file, pattern);
  }

  protected abstract int getOccurrencesCountImpl(@Nonnull PsiFile file, @Nonnull IndexPatternProvider provider);

  protected abstract int getOccurrencesCountImpl(@Nonnull PsiFile file, @Nonnull IndexPattern pattern);
}
