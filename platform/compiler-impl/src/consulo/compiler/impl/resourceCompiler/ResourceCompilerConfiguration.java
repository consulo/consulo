package consulo.compiler.impl.resourceCompiler;

import com.intellij.CommonBundle;
import com.intellij.compiler.MalformedPatternException;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import org.apache.oro.text.regex.*;
import consulo.lombok.annotations.Logger;
import consulo.lombok.annotations.ProjectService;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author VISTALL
 * @since 20:16/24.05.13
 */
@Logger
@ProjectService
@State(
  name = "ResourceCompilerConfiguration",
  storages = {@Storage(file = StoragePathMacros.PROJECT_FILE),
    @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/compiler.xml", scheme = StorageScheme.DIRECTORY_BASED)})
public class ResourceCompilerConfiguration implements PersistentStateComponent<Element> {
  public static final String RESOURCE_EXTENSIONS = "resourceExtensions";
  public static final String WILDCARD_RESOURCE_PATTERNS = "wildcardResourcePatterns";
  public static final String ENTRY = "entry";
  public static final String NAME = "name";

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

  private Project myProject;
  // extensions of the files considered as resource files
  private final List<Pattern> myRegexpResourcePatterns = new ArrayList<Pattern>();
  // extensions of the files considered as resource files. If present, overrides patterns in old regexp format stored in myRegexpResourcePatterns
  private final List<String> myWildcardPatterns = new ArrayList<String>();
  private final List<CompiledPattern> myCompiledPatterns = new ArrayList<CompiledPattern>();
  private final List<CompiledPattern> myNegatedCompiledPatterns = new ArrayList<CompiledPattern>();
  private boolean myWildcardPatternsInitialized = false;
  private final Perl5Matcher myPatternMatcher = new Perl5Matcher();

  public ResourceCompilerConfiguration(Project project) {
    myProject = project;
  }

  public boolean isResourceFile(VirtualFile virtualFile) {
    return isResourceFile(virtualFile.getName(), virtualFile.getParent());
  }

  public List<String> getResourceFilePatterns() {
    return new ArrayList<String>(myWildcardPatterns);
  }

  public void removeResourceFilePatterns() {
    removeWildcardPatterns();
  }

  private boolean isResourceFile(String name, @Nullable VirtualFile parent) {
    final Ref<String> parentRef = Ref.create(null);
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myCompiledPatterns.size(); i++) {
      if (matches(name, parent, parentRef, myCompiledPatterns.get(i))) {
        return true;
      }
    }

    if (myNegatedCompiledPatterns.isEmpty()) {
      return false;
    }

    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myNegatedCompiledPatterns.size(); i++) {
      if (matches(name, parent, parentRef, myNegatedCompiledPatterns.get(i))) {
        return false;
      }
    }
    return true;
  }

  private boolean matches(String name, VirtualFile parent, Ref<String> parentRef, CompiledPattern pair) {
    if (!matches(name, pair.fileName)) {
      return false;
    }

    if (parent != null && (pair.dir != null || pair.srcRoot != null)) {
      VirtualFile srcRoot = ProjectRootManager.getInstance(myProject).getFileIndex().getSourceRootForFile(parent);
      if (pair.dir != null) {
        String parentPath = parentRef.get();
        if (parentPath == null) {
          parentRef.set(parentPath = srcRoot == null ? parent.getPath() : VfsUtilCore.getRelativePath(parent, srcRoot, '/'));
        }
        if (parentPath == null || !matches("/" + parentPath, pair.dir)) {
          return false;
        }
      }

      if (pair.srcRoot != null) {
        String srcRootName = srcRoot == null ? null : srcRoot.getName();
        if (srcRootName == null || !matches(srcRootName, pair.srcRoot)) {
          return false;
        }
      }
    }

    return true;
  }

  private boolean matches(String s, Pattern p) {
    synchronized (myPatternMatcher) {
      try {
        return myPatternMatcher.matches(s, p);
      }
      catch (Exception e) {
        ResourceCompilerConfiguration.LOGGER.error("Exception matching file name \"" + s + "\" against the pattern \"" + p + "\"", e);
        return false;
      }
    }
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

      }
    }
    finally {
      myWildcardPatternsInitialized = true;
    }
  }



  private void removeWildcardPatterns() {
    myWildcardPatterns.clear();
    myCompiledPatterns.clear();
    myNegatedCompiledPatterns.clear();
  }

  public void addResourceFilePattern(String namePattern) throws MalformedPatternException {
    addWildcardResourcePattern(namePattern);
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

  public static CompiledPattern convertToRegexp(String wildcardPattern) throws MalformedPatternException{
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
    final Element newChild = addChild(parentNode, RESOURCE_EXTENSIONS);
    for (final String pattern : getRegexpPatterns()) {
      addChild(newChild, ENTRY).setAttribute(NAME, pattern);
    }

    if (myWildcardPatternsInitialized || !myWildcardPatterns.isEmpty()) {
      final Element wildcardPatterns = addChild(parentNode, WILDCARD_RESOURCE_PATTERNS);
      for (final String wildcardPattern : myWildcardPatterns) {
        addChild(wildcardPatterns, ENTRY)
          .setAttribute(NAME, wildcardPattern);
      }
    }
    return parentNode;
  }

  @Override
  public void loadState(Element state) {
    try {
      readExternal(state);
    }
    catch (InvalidDataException e) {
      ResourceCompilerConfiguration.LOGGER.error(e);
    }
  }

  private void removeRegexpPatterns() {
    myRegexpResourcePatterns.clear();
  }

  private void addRegexpPattern(String namePattern) throws MalformedPatternException {
    Pattern pattern = compilePattern(namePattern);
    if (pattern != null) {
      myRegexpResourcePatterns.add(pattern);
    }
  }

  public void readExternal(Element parentNode) throws InvalidDataException {
    DefaultJDOMExternalizer.readExternal(this, parentNode);

    try {
      removeRegexpPatterns();
      Element node = parentNode.getChild(RESOURCE_EXTENSIONS);
      if (node != null) {
        for (final Object o : node.getChildren(ENTRY)) {
          Element element = (Element)o;
          String pattern = element.getAttributeValue(NAME);
          if (!StringUtil.isEmpty(pattern)) {
            addRegexpPattern(pattern);
          }
        }
      }

      removeWildcardPatterns();
      node = parentNode.getChild(WILDCARD_RESOURCE_PATTERNS);
      if (node != null) {
        myWildcardPatternsInitialized = true;
        for (final Object o : node.getChildren(ENTRY)) {
          final Element element = (Element)o;
          String pattern = element.getAttributeValue(NAME);
          if (!StringUtil.isEmpty(pattern)) {
            addWildcardResourcePattern(pattern);
          }
        }
      }
    }
    catch (MalformedPatternException e) {
      throw new InvalidDataException(e);
    }
  }


  private static Element addChild(Element parent, final String childName) {
    final Element child = new Element(childName);
    parent.addContent(child);
    return child;
  }

}
