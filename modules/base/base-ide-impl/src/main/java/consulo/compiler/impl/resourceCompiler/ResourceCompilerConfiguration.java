/*
 * Copyright 2013-2016 consulo.io
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
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import consulo.logging.Logger;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author VISTALL
 * @since 20:16/24.05.13
 */
@Singleton
@State(
  name = "ResourceCompilerConfiguration",
  storages = {
    @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/compiler.xml")})
public class ResourceCompilerConfiguration implements PersistentStateComponent<Element> {
  @Nonnull
  public static ResourceCompilerConfiguration getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, ResourceCompilerConfiguration.class);
  }

  public static final Logger LOGGER = Logger.getInstance(ResourceCompilerConfiguration.class);

  public static final String RESOURCE_EXTENSIONS = "resourceExtensions";
  public static final String WILDCARD_RESOURCE_PATTERNS = "wildcardResourcePatterns";
  public static final String ENTRY = "entry";
  public static final String NAME = "name";

  private static class CompiledPattern {
    @Nonnull
    final Pattern fileName;
    @Nullable
    final Pattern dir;
    @Nullable
    final Pattern srcRoot;

    private CompiledPattern(Pattern fileName, Pattern dir, Pattern srcRoot) {
      this.fileName = fileName;
      this.dir = dir;
      this.srcRoot = srcRoot;
    }
  }

  private Project myProject;
  // extensions of the files considered as resource files
  private final List<Pattern> myRegexpResourcePatterns = new ArrayList<>();
  // extensions of the files considered as resource files. If present, overrides patterns in old regexp format stored in myRegexpResourcePatterns
  private final List<String> myWildcardPatterns = new ArrayList<>();
  private final List<CompiledPattern> myCompiledPatterns = new ArrayList<>();
  private final List<CompiledPattern> myNegatedCompiledPatterns = new ArrayList<>();
  private boolean myWildcardPatternsInitialized = false;

  @Inject
  public ResourceCompilerConfiguration(Project project) {
    myProject = project;
  }

  public boolean isResourceFile(VirtualFile virtualFile) {
    return isResourceFile(virtualFile.getName(), virtualFile.getParent());
  }

  public List<String> getResourceFilePatterns() {
    return new ArrayList<>(myWildcardPatterns);
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
    try {
      return p.matcher(s).find();
    }
    catch (Exception e) {
      ResourceCompilerConfiguration.LOGGER.error("Exception matching file name \"" + s + "\" against the pattern \"" + p + "\"", e);
      return false;
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
      patterns[index++] = myRegexpResourcePattern.pattern();
    }
    return patterns;
  }

  private boolean doConvertPatterns() throws MalformedPatternException {
    final String[] regexpPatterns = getRegexpPatterns();
    final List<String> converted = new ArrayList<>();
    final Pattern multipleExtensionsPatternPattern = compilePattern("\\.\\+\\\\\\.\\((\\w+(?:\\|\\w+)*)\\)");
    final Pattern singleExtensionPatternPattern = compilePattern("\\.\\+\\\\\\.(\\w+)");
    for (final String regexpPattern : regexpPatterns) {
      Matcher matcher = multipleExtensionsPatternPattern.matcher(regexpPattern);
      if (matcher.find()) {
        final StringTokenizer tokenizer = new StringTokenizer(matcher.group(1), "|", false);
        while (tokenizer.hasMoreTokens()) {
          converted.add("?*." + tokenizer.nextToken());
        }
      }
      else {
        matcher = singleExtensionPatternPattern.matcher(regexpPattern);
        if (matcher.find()) {
          converted.add("?*." + matcher.group(1));
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

  private static Pattern compilePattern(String s) throws MalformedPatternException {
    return SystemInfo.isFileSystemCaseSensitive ? Pattern.compile(s) : Pattern.compile(s, Pattern.CASE_INSENSITIVE);
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
    String[] patterns = getRegexpPatterns();
    if(patterns.length == 0 && myWildcardPatterns.isEmpty()) {
      return null;
    }
    Element state = new Element("state");
    final Element newChild = addChild(state, RESOURCE_EXTENSIONS);
    for (final String pattern : patterns) {
      addChild(newChild, ENTRY).setAttribute(NAME, pattern);
    }

    if (myWildcardPatternsInitialized || !myWildcardPatterns.isEmpty()) {
      final Element wildcardPatterns = addChild(state, WILDCARD_RESOURCE_PATTERNS);
      for (final String wildcardPattern : myWildcardPatterns) {
        addChild(wildcardPatterns, ENTRY)
          .setAttribute(NAME, wildcardPattern);
      }
    }
    return state;
  }

  @Override
  public void loadState(Element state) {
    readExternal(state);
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

  public void readExternal(Element parentNode) {
    try {
      removeRegexpPatterns();
      Element node = parentNode.getChild(RESOURCE_EXTENSIONS);
      if (node != null) {
        for (final Element element : node.getChildren(ENTRY)) {
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
        for (final Element element : node.getChildren(ENTRY)) {
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
