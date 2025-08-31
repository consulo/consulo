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
package consulo.execution.ui.console;

import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Yura Cangea
 * @version 1.0
 */
public class RegexpFilter implements Filter {
  public static final String FILE_PATH_MACROS = "$FILE_PATH$";
  public static final String LINE_MACROS = "$LINE$";
  public static final String COLUMN_MACROS = "$COLUMN$";

  private static final String FILE_PATH_REGEXP = "((?:\\p{Alpha}\\:)?[0-9 a-z_A-Z\\-\\\\./]+)";
  private static final String NUMBER_REGEXP = "([0-9]+)";

  private final int myFileRegister;
  private final int myLineRegister;
  private final int myColumnRegister;

  private final Pattern myPattern;
  private final Project myProject;
  private static final String FILE_STR = "file";
  private static final String LINE_STR = "line";
  private static final String COLUMN_STR = "column";

  public RegexpFilter(Project project, @Nonnull String expression) {
    myProject = project;
    validate(expression);

    if (expression.trim().isEmpty()) {
      throw new InvalidExpressionException("expression == null or empty");
    }

    int filePathIndex = expression.indexOf(FILE_PATH_MACROS);
    int lineIndex = expression.indexOf(LINE_MACROS);
    int columnIndex = expression.indexOf(COLUMN_MACROS);

    if (filePathIndex == -1) {
      throw new InvalidExpressionException("Expression must contain " + FILE_PATH_MACROS + " macros.");
    }

    TreeMap<Integer, String> map = new TreeMap<Integer, String>();

    map.put(filePathIndex, FILE_STR);

    expression = StringUtil.replace(expression, FILE_PATH_MACROS, FILE_PATH_REGEXP);

    if (lineIndex != -1) {
      expression = StringUtil.replace(expression, LINE_MACROS, NUMBER_REGEXP);
      map.put(lineIndex, LINE_STR);
    }

    if (columnIndex != -1) {
      expression = StringUtil.replace(expression, COLUMN_MACROS, NUMBER_REGEXP);
      map.put(columnIndex, COLUMN_STR);
    }

    // The block below determines the registers based on the sorted map.
    int count = 0;
    for (Integer integer : map.keySet()) {
      count++;
      String s = map.get(integer);

      if (FILE_STR.equals(s)) {
        filePathIndex = count;
      }
      else if (LINE_STR.equals(s)) {
        lineIndex = count;
      }
      else if (COLUMN_STR.equals(s)) {
        columnIndex = count;
      }
    }

    myFileRegister = filePathIndex;
    myLineRegister = lineIndex;
    myColumnRegister = columnIndex;
    myPattern = Pattern.compile(expression, Pattern.MULTILINE);
  }

  public static void validate(String expression) {
    if (expression == null || expression.trim().isEmpty()) {
      throw new InvalidExpressionException("expression == null or empty");
    }

    expression = substituteMacrosesWithRegexps(expression);

    Pattern.compile(expression, Pattern.MULTILINE);
  }

  private static String substituteMacrosesWithRegexps(String expression) {
    int filePathIndex = expression.indexOf(FILE_PATH_MACROS);
    int lineIndex = expression.indexOf(LINE_MACROS);
    int columnIndex = expression.indexOf(COLUMN_MACROS);

    if (filePathIndex == -1) {
      throw new InvalidExpressionException("Expression must contain " + FILE_PATH_MACROS + " macros.");
    }

    expression = StringUtil.replace(expression, FILE_PATH_MACROS, FILE_PATH_REGEXP);

    if (lineIndex != -1) {
      expression = StringUtil.replace(expression, LINE_MACROS, NUMBER_REGEXP);
    }

    if (columnIndex != -1) {
      expression = StringUtil.replace(expression, COLUMN_MACROS, NUMBER_REGEXP);
    }
    return expression;
  }

  @Override
  public Result applyFilter(String line, int entireLength) {

    Matcher matcher = myPattern.matcher(line);
    if (matcher.find()) {
      return createResult(matcher, entireLength - line.length());
    }

    return null;
  }

  private Result createResult(Matcher matcher, int entireLen) {
    String filePath = matcher.group(myFileRegister);

    String lineNumber = "0";

    if (myLineRegister != -1) {
      lineNumber = matcher.group(myLineRegister);
    }

    String columnNumber = "0";
    if (myColumnRegister != -1) {
      columnNumber = matcher.group(myColumnRegister);
    }

    int line = 0;
    int column = 0;
    try {
      line = Integer.parseInt(lineNumber);
      column = Integer.parseInt(columnNumber);
    }
    catch (NumberFormatException e) {
      // Do nothing, so that line and column will remain at their initial
      // zero values.
    }

    if (line > 0) line -= 1;
    if (column > 0) column -= 1;
    // Calculate the offsets relative to the entire text.
    int highlightStartOffset = entireLen + matcher.start(myFileRegister);
    int highlightEndOffset = highlightStartOffset + filePath.length();

    HyperlinkInfo info = createOpenFileHyperlink(filePath, line, column);
    return new Result(highlightStartOffset, highlightEndOffset, info);
  }

  @Nullable
  protected HyperlinkInfo createOpenFileHyperlink(String fileName, int line, int column) {
    fileName = fileName.replace(File.separatorChar, '/');
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(fileName);
    if (file == null) return null;
    return new OpenFileHyperlinkInfo(myProject, file, line, column);
  }

  public static String[] getMacrosName() {
    return new String[]{FILE_PATH_MACROS, LINE_MACROS, COLUMN_MACROS};
  }
}
