package org.consulo.compiler.impl.resourceCompiler;

import com.intellij.CommonBundle;
import com.intellij.compiler.MalformedPatternException;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.projectRoot.TextConfigurable;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import org.apache.oro.text.regex.*;
import org.consulo.compiler.CompilerSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.java.compiler.JpsJavaCompilerConfigurationSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author VISTALL
 * @since 20:16/24.05.13
 */
public class ResourceCompilerSettings implements CompilerSettings, PersistentStateComponent<Element> {
  private static class CompiledPattern {
    @NotNull final Pattern fileName;
    @Nullable final Pattern dir;
    @Nullable final Pattern srcRoot;

    private CompiledPattern(Pattern fileName, Pattern dir, Pattern srcRoot) {
      this.fileName = fileName;
      this.dir = dir;
      this.srcRoot = srcRoot;
    }
  }

  private static final Logger LOG = Logger.getInstance(ResourceCompilerSettings.class);

  private Project myProject;
  // extensions of the files considered as resource files
  private final List<Pattern> myRegexpResourcePatterns = new ArrayList<Pattern>();
  // extensions of the files considered as resource files. If present, overrides patterns in old regexp format stored in myRegexpResourcePatterns
  private final List<String> myWildcardPatterns = new ArrayList<String>();
  private final List<CompiledPattern> myCompiledPatterns = new ArrayList<CompiledPattern>();
  private final List<CompiledPattern> myNegatedCompiledPatterns = new ArrayList<CompiledPattern>();
  private boolean myWildcardPatternsInitialized = false;

  public ResourceCompilerSettings(Project project) {
    myProject = project;

    loadDefaultWildcardPatterns();
  }


  public void convertPatterns() {
    if (!needPatternConversion()) {
      return;
    }
    try {
      boolean ok;
      try {
        ok = doConvertPatterns();
      }
      catch (MalformedPatternException e) {
        ok = false;
      }
      if (!ok) {
        final String initialPatternString = patternsToString(getRegexpPatterns());
        final String message = CompilerBundle
          .message("message.resource.patterns.format.changed", ApplicationNamesInfo.getInstance().getProductName(), initialPatternString,
                   CommonBundle.getOkButtonText(), CommonBundle.getCancelButtonText());
        final String wildcardPatterns = Messages
          .showInputDialog(myProject, message, CompilerBundle.message("pattern.conversion.dialog.title"), Messages.getWarningIcon(),
                           initialPatternString, new InputValidator() {
            public boolean checkInput(String inputString) {
              return true;
            }

            public boolean canClose(String inputString) {
              final StringTokenizer tokenizer = new StringTokenizer(inputString, ";", false);
              StringBuilder malformedPatterns = new StringBuilder();

              while (tokenizer.hasMoreTokens()) {
                String pattern = tokenizer.nextToken();
                try {
                  addWildcardResourcePattern(pattern);
                }
                catch (MalformedPatternException e) {
                  malformedPatterns.append("\n\n");
                  malformedPatterns.append(pattern);
                  malformedPatterns.append(": ");
                  malformedPatterns.append(e.getMessage());
                }
              }

              if (malformedPatterns.length() > 0) {
                Messages.showErrorDialog(CompilerBundle.message("error.bad.resource.patterns", malformedPatterns.toString()),
                                         CompilerBundle.message("bad.resource.patterns.dialog.title"));
                removeWildcardPatterns();
                return false;
              }
              return true;
            }
          });
        if (wildcardPatterns == null) { // cancel pressed
          loadDefaultWildcardPatterns();
        }
      }
    }
    finally {
      myWildcardPatternsInitialized = true;
    }
  }

  private void loadDefaultWildcardPatterns() {
    if (!myWildcardPatterns.isEmpty()) {
      removeWildcardPatterns();
    }
    try {
      addWildcardResourcePattern("!?*.java");
      addWildcardResourcePattern("!?*.form");
      addWildcardResourcePattern("!?*.class");
      addWildcardResourcePattern("!?*.groovy");
      addWildcardResourcePattern("!?*.scala");
      addWildcardResourcePattern("!?*.flex");
      addWildcardResourcePattern("!?*.kt");
      addWildcardResourcePattern("!?*.clj");
    }
    catch (MalformedPatternException e) {
      LOG.error(e);
    }
  }


  private void removeWildcardPatterns() {
    myWildcardPatterns.clear();
    myCompiledPatterns.clear();
    myNegatedCompiledPatterns.clear();
  }

  private void addWildcardResourcePattern(@NonNls final String wildcardPattern) throws MalformedPatternException {
    final CompiledPattern pattern = convertToRegexp(wildcardPattern);
    if (pattern != null) {
      myWildcardPatterns.add(wildcardPattern);
      if (isPatternNegated(wildcardPattern)) {
        myNegatedCompiledPatterns.add(pattern);
      }
      else {
        myCompiledPatterns.add(pattern);
      }
    }
  }

  private boolean needPatternConversion() {
    return !myWildcardPatternsInitialized && !myRegexpResourcePatterns.isEmpty();
  }

  private String[] getRegexpPatterns() {
    String[] patterns = ArrayUtil.newStringArray(myRegexpResourcePatterns.size());
    int index = 0;
    for (final Pattern myRegexpResourcePattern : myRegexpResourcePatterns) {
      patterns[index++] = myRegexpResourcePattern.getPattern();
    }
    return patterns;
  }

  private boolean doConvertPatterns() throws MalformedPatternException {
    final String[] regexpPatterns = getRegexpPatterns();
    final List<String> converted = new ArrayList<String>();
    final Pattern multipleExtensionsPatternPattern = compilePattern("\\.\\+\\\\\\.\\((\\w+(?:\\|\\w+)*)\\)");
    final Pattern singleExtensionPatternPattern = compilePattern("\\.\\+\\\\\\.(\\w+)");
    final Perl5Matcher matcher = new Perl5Matcher();
    for (final String regexpPattern : regexpPatterns) {
      //final Matcher multipleExtensionsMatcher = multipleExtensionsPatternPattern.matcher(regexpPattern);
      if (matcher.matches(regexpPattern, multipleExtensionsPatternPattern)) {
        final MatchResult match = matcher.getMatch();
        final StringTokenizer tokenizer = new StringTokenizer(match.group(1), "|", false);
        while (tokenizer.hasMoreTokens()) {
          converted.add("?*." + tokenizer.nextToken());
        }
      }
      else {
        //final Matcher singleExtensionMatcher = singleExtensionPatternPattern.matcher(regexpPattern);
        if (matcher.matches(regexpPattern, singleExtensionPatternPattern)) {
          final MatchResult match = matcher.getMatch();
          converted.add("?*." + match.group(1));
        }
        else {
          return false;
        }
      }
    }
    for (final String aConverted : converted) {
      addWildcardResourcePattern(aConverted);
    }
    return true;
  }

  private static Pattern compilePattern(@NonNls String s) throws MalformedPatternException {
    try {
      final PatternCompiler compiler = new Perl5Compiler();
      return SystemInfo.isFileSystemCaseSensitive ? compiler.compile(s) : compiler.compile(s, Perl5Compiler.CASE_INSENSITIVE_MASK);
    }
    catch (org.apache.oro.text.regex.MalformedPatternException ex) {
      throw new MalformedPatternException(ex);
    }
  }

  public static boolean isPatternNegated(String wildcardPattern) {
    return wildcardPattern.length() > 1 && wildcardPattern.charAt(0) == '!';
  }

  private static CompiledPattern convertToRegexp(String wildcardPattern) {
    if (isPatternNegated(wildcardPattern)) {
      wildcardPattern = wildcardPattern.substring(1);
    }

    wildcardPattern = FileUtil.toSystemIndependentName(wildcardPattern);

    String srcRoot = null;
    int colon = wildcardPattern.indexOf(":");
    if (colon > 0) {
      srcRoot = wildcardPattern.substring(0, colon);
      wildcardPattern = wildcardPattern.substring(colon + 1);
    }

    String dirPattern = null;
    int slash = wildcardPattern.lastIndexOf('/');
    if (slash >= 0) {
      dirPattern = wildcardPattern.substring(0, slash + 1);
      wildcardPattern = wildcardPattern.substring(slash + 1);
      if (!dirPattern.startsWith("/")) {
        dirPattern = "/" + dirPattern;
      }
      //now dirPattern starts and ends with '/'

      dirPattern = normalizeWildcards(dirPattern);

      dirPattern = StringUtil.replace(dirPattern, "/.*.*/", "(/.*)?/");
      dirPattern = StringUtil.trimEnd(dirPattern, "/");

      dirPattern = optimize(dirPattern);
    }

    wildcardPattern = normalizeWildcards(wildcardPattern);
    wildcardPattern = optimize(wildcardPattern);

    final Pattern dirCompiled = dirPattern == null ? null : compilePattern(dirPattern);
    final Pattern srcCompiled = srcRoot == null ? null : compilePattern(optimize(normalizeWildcards(srcRoot)));
    return new CompiledPattern(compilePattern(wildcardPattern), dirCompiled, srcCompiled);
  }

  private static String optimize(String wildcardPattern) {
    return wildcardPattern.replaceAll("(?:\\.\\*)+", ".*");
  }

  private static String normalizeWildcards(String wildcardPattern) {
    wildcardPattern = StringUtil.replace(wildcardPattern, "\\!", "!");
    wildcardPattern = StringUtil.replace(wildcardPattern, ".", "\\.");
    wildcardPattern = StringUtil.replace(wildcardPattern, "*?", ".+");
    wildcardPattern = StringUtil.replace(wildcardPattern, "?*", ".+");
    wildcardPattern = StringUtil.replace(wildcardPattern, "*", ".*");
    wildcardPattern = StringUtil.replace(wildcardPattern, "?", ".");
    return wildcardPattern;
  }

  private static String patternsToString(final String[] patterns) {
    final StringBuilder extensionsString = new StringBuilder();
    for (int idx = 0; idx < patterns.length; idx++) {
      if (idx > 0) {
        extensionsString.append(";");
      }
      extensionsString.append(patterns[idx]);
    }
    return extensionsString.toString();
  }

  @Nullable
  @Override
  public Element getState() {
    Element parentNode = new Element("state");
    final Element newChild = addChild(parentNode, JpsJavaCompilerConfigurationSerializer.RESOURCE_EXTENSIONS);
    for (final String pattern : getRegexpPatterns()) {
      addChild(newChild, JpsJavaCompilerConfigurationSerializer.ENTRY).setAttribute(JpsJavaCompilerConfigurationSerializer.NAME, pattern);
    }

    if (myWildcardPatternsInitialized || !myWildcardPatterns.isEmpty()) {
      final Element wildcardPatterns = addChild(parentNode, JpsJavaCompilerConfigurationSerializer.WILDCARD_RESOURCE_PATTERNS);
      for (final String wildcardPattern : myWildcardPatterns) {
        addChild(wildcardPatterns, JpsJavaCompilerConfigurationSerializer.ENTRY).setAttribute(JpsJavaCompilerConfigurationSerializer.NAME, wildcardPattern);
      }
    }
    return parentNode;
  }

  @Override
  public void loadState(Element state) {

  }

  private static Element addChild(Element parent, final String childName) {
    final Element child = new Element(childName);
    parent.addContent(child);
    return child;
  }

  @Override
  public Configurable createConfigurable() {
    return new TextConfigurable<CompilerSettings>(this, "Resource", "Test", "Test", AllIcons.Nodes.Module);
  }
}
