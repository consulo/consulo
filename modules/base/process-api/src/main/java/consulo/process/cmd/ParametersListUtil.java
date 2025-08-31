// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.process.cmd;

import consulo.util.collection.ArrayUtil;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Function;

/**
 * @author nik
 */
public class ParametersListUtil {
  public static final Function<String, List<String>> DEFAULT_LINE_PARSER = text -> parse(text, true);
  public static final Function<List<String>, String> DEFAULT_LINE_JOINER = strings -> StringUtil.join(strings, " ");
  public static final Function<String, List<String>> COLON_LINE_PARSER = text -> {
    ArrayList<String> result = new ArrayList<>();
    StringTokenizer tokenizer = new StringTokenizer(text, ";", false);
    while (tokenizer.hasMoreTokens()) {
      result.add(tokenizer.nextToken());
    }
    return result;
  };
  public static final Function<List<String>, String> COLON_LINE_JOINER = strings -> StringUtil.join(strings, ";");

  /**
   * <p>Joins list of parameters into single string, which may be then parsed back into list by {@link #parseToArray(String)}.</p>
   * <p/>
   * <p>
   * <strong>Conversion rules:</strong>
   * <ul>
   * <li>double quotes are escaped by backslash ({@code &#92;});</li>
   * <li>empty parameters parameters and parameters with spaces inside are surrounded with double quotes ({@code "});</li>
   * <li>parameters are separated by single whitespace.</li>
   * </ul>
   * </p>
   * <p/>
   * <p><strong>Examples:</strong></p>
   * <p>
   * {@code ['a', 'b'] => 'a  b'}<br/>
   * {@code ['a="1 2"', 'b'] => '"a &#92;"1 2&#92;"" b'}
   * </p>
   *
   * @param parameters a list of parameters to join.
   * @return a string with parameters.
   */
  @Nonnull
  public static String join(@Nonnull List<? extends CharSequence> parameters) {
    return encode(parameters);
  }

  /**
   * @param commandLineArgumentEncoder used to handle (quote or escape) special characters in command line argument
   * @see ParametersListUtil#join(List)
   */
  @Nonnull
  public static String join(@Nonnull List<? extends CharSequence> parameters, @Nonnull CommandLineArgumentEncoder commandLineArgumentEncoder) {
    return encode(parameters, commandLineArgumentEncoder);
  }

  @Nonnull
  public static String join(String... parameters) {
    return encode(Arrays.asList(parameters));
  }

  /**
   * @see #parse(String)
   */
  @Nonnull
  public static String[] parseToArray(@Nonnull String string) {
    List<String> params = parse(string);
    return ArrayUtil.toStringArray(params);
  }

  /**
   * <p>Splits single parameter string (as created by {@link #join(List)}) into list of parameters.</p>
   * <p/>
   * <p>
   * <strong>Conversion rules:</strong>
   * <ul>
   * <li>starting/whitespaces are trimmed;</li>
   * <li>parameters are split by whitespaces, whitespaces itself are dropped</li>
   * <li>parameters inside double quotes ({@code "a b"}) are kept as single one;</li>
   * <li>double quotes are dropped, escaped double quotes ({@code &#92;"}) are un-escaped.</li>
   * <li>For single quotes support see {@link #parse(String, boolean, boolean)}</li>
   * </ul>
   * </p>
   * <p/>
   * <p><strong>Examples:</strong></p>
   * <p>
   * {@code ' a  b ' => ['a', 'b']}<br/>
   * {@code 'a="1 2" b' => ['a=1 2', 'b']}<br/>
   * {@code 'a " " b' => ['a', ' ', 'b']}<br/>
   * {@code '"a &#92;"1 2&#92;"" b' => ['a="1 2"', 'b']}
   * </p>
   *
   * @param parameterString parameter string to split.
   * @return array of parameters.
   */
  @Nonnull
  public static List<String> parse(@Nonnull String parameterString) {
    return parse(parameterString, false);
  }

  @Nonnull
  public static List<String> parse(@Nonnull String parameterString, boolean keepQuotes) {
    return parse(parameterString, keepQuotes, false);
  }

  @Nonnull
  public static List<String> parse(@Nonnull String parameterString, boolean keepQuotes, boolean supportSingleQuotes) {
    return parse(parameterString, keepQuotes, supportSingleQuotes, false);
  }

  @Nonnull
  public static List<String> parse(@Nonnull String parameterString, boolean keepQuotes, boolean supportSingleQuotes, boolean keepEmptyParameters) {
    if (!keepEmptyParameters) {
      parameterString = parameterString.trim();
    }

    ArrayList<String> params = new ArrayList<>();
    if (parameterString.isEmpty()) {
      return params;
    }
    StringBuilder token = new StringBuilder(128);
    boolean inQuotes = false;
    boolean escapedQuote = false;
    IntSet possibleQuoteChars = IntSets.newHashSet();
    possibleQuoteChars.add('"');
    if (supportSingleQuotes) {
      possibleQuoteChars.add('\'');
    }
    char currentQuote = 0;
    boolean nonEmpty = false;

    for (int i = 0; i < parameterString.length(); i++) {
      char ch = parameterString.charAt(i);
      if ((inQuotes ? currentQuote == ch : possibleQuoteChars.contains(ch))) {
        if (!escapedQuote) {
          inQuotes = !inQuotes;
          currentQuote = ch;
          nonEmpty = true;
          if (!keepQuotes) {
            continue;
          }
        }
        escapedQuote = false;
      }
      else if (Character.isWhitespace(ch)) {
        if (!inQuotes) {
          if (keepEmptyParameters || token.length() > 0 || nonEmpty) {
            params.add(token.toString());
            token.setLength(0);
            nonEmpty = false;
          }
          continue;
        }
      }
      else if (ch == '\\' && i < parameterString.length() - 1) {
        char nextchar = parameterString.charAt(i + 1);
        if (inQuotes ? currentQuote == nextchar : possibleQuoteChars.contains(nextchar)) {
          escapedQuote = true;
          if (!keepQuotes) {
            continue;
          }
        }
      }

      token.append(ch);
    }

    if (keepEmptyParameters || token.length() > 0 || nonEmpty) {
      params.add(token.toString());
    }

    return params;
  }

  @Nonnull
  private static String encode(@Nonnull List<? extends CharSequence> parameters) {
    return encode(parameters, CommandLineArgumentEncoder.DEFAULT_ENCODER);
  }

  @Nonnull
  private static String encode(@Nonnull List<? extends CharSequence> parameters, @Nonnull CommandLineArgumentEncoder commandLineArgumentEncoder) {
    if (parameters.isEmpty()) {
      return "";
    }

    StringBuilder buffer = new StringBuilder();
    StringBuilder paramBuilder = new StringBuilder();
    for (CharSequence parameter : parameters) {
      if (buffer.length() > 0) {
        buffer.append(' ');
      }

      paramBuilder.append(parameter);
      commandLineArgumentEncoder.encodeArgument(paramBuilder);
      buffer.append(paramBuilder);
      paramBuilder.setLength(0);
    }
    return buffer.toString();
  }
}
