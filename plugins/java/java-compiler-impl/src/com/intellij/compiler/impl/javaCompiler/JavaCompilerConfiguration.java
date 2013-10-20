package com.intellij.compiler.impl.javaCompiler;

import com.intellij.compiler.impl.javaCompiler.annotationProcessing.ProcessorConfigProfile;
import com.intellij.compiler.impl.javaCompiler.annotationProcessing.impl.ProcessorConfigProfileImpl;
import com.intellij.openapi.components.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import org.consulo.lombok.annotations.ProjectService;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * @author VISTALL
 * @since 10:31/27.05.13
 */
@ProjectService
@State(
  name = "JavaCompilerConfiguration",
  storages = {@Storage(file = StoragePathMacros.PROJECT_FILE),
    @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/compiler.xml", scheme = StorageScheme.DIRECTORY_BASED)}
)
public class JavaCompilerConfiguration implements PersistentStateComponent<Element> {
  public static final String ANNOTATION_PROCESSING = "annotationProcessing";
  public static final String BYTECODE_TARGET_LEVEL = "bytecodeTargetLevel";

  public static final String ADD_NOTNULL_ASSERTIONS = "add-not-null-assertions";
  public static final String ENTRY = "entry";
  public static final String NAME = "name";
  public static final String ENABLED = "enabled";
  public static final String MODULE = "module";
  public static final String TARGET_ATTRIBUTE = "target";

  public static final String DEFAULT_COMPILER = "JavacCompiler";

  @NotNull
  private final Project myProject;

  private BackendCompiler myBackendCompilerCache;

  private final ProcessorConfigProfile myDefaultProcessorsProfile = new ProcessorConfigProfileImpl("Default");
  private final List<ProcessorConfigProfile> myModuleProcessorProfiles = new ArrayList<ProcessorConfigProfile>();

  // the map is calculated by module processor profiles list for faster access to module settings
  private Map<Module, ProcessorConfigProfile> myProcessorsProfilesMap = null;
  private boolean myAddNotNullAssertions;

  public JavaCompilerConfiguration(@NotNull Project project) {
    myProject = project;
  }

  @Nullable
  public String getBytecodeTargetLevel(Module module) {
    return null; //TODO [VISTALL]
  }

  public void setBytecodeTargetLevel(Module module, String level) {

  }

  @NotNull
  public BackendCompiler getActiveCompiler() {
    if (myBackendCompilerCache == null) {
      myBackendCompilerCache = findCompiler(DEFAULT_COMPILER);
    }
    return myBackendCompilerCache;
  }

  public void setActiveCompiler(@NotNull BackendCompiler key) {
    myBackendCompilerCache = key;
  }

  @Nullable
  public BackendCompiler findCompiler(@NotNull String className) {
    for (BackendCompilerEP ep : BackendCompiler.EP_NAME.getExtensions(myProject)) {
      if (className.equals(ep.getInstance(myProject).getClass().getSimpleName())) {
        return ep.getInstance(myProject);
      }
    }
    return null;
  }

  @Nullable
  @Override
  public Element getState() {
    Element parentNode = new Element("state");

    final Element annotationProcessingSettings = addChild(parentNode, ANNOTATION_PROCESSING);
    final Element defaultProfileElem = addChild(annotationProcessingSettings, "profile").setAttribute("default", "true");
    AnnotationProcessorProfileSerializer.writeExternal(myDefaultProcessorsProfile, defaultProfileElem);
    for (ProcessorConfigProfile profile : myModuleProcessorProfiles) {
      final Element profileElem = addChild(annotationProcessingSettings, "profile").setAttribute("default", "false");
      AnnotationProcessorProfileSerializer.writeExternal(profile, profileElem);
    }

    parentNode
      .setAttribute("compiler", myBackendCompilerCache == null ? DEFAULT_COMPILER : myBackendCompilerCache.getClass().getSimpleName());
    if(myAddNotNullAssertions) {
      parentNode.setAttribute(ADD_NOTNULL_ASSERTIONS, String.valueOf(Boolean.TRUE));
    }
    return parentNode;
  }

  private static Element addChild(Element parent, final String childName) {
    final Element child = new Element(childName);
    parent.addContent(child);
    return child;
  }

  @Override
  public void loadState(Element parentNode) {
    myBackendCompilerCache = findCompiler(parentNode.getAttributeValue("compiler"));
    myAddNotNullAssertions = Boolean.parseBoolean(parentNode.getAttributeValue(ADD_NOTNULL_ASSERTIONS, String.valueOf(Boolean.FALSE)));

    final Element annotationProcessingSettings = parentNode.getChild(ANNOTATION_PROCESSING);
    if (annotationProcessingSettings != null) {
      final List profiles = annotationProcessingSettings.getChildren("profile");
      if (!profiles.isEmpty()) {
        for (Object elem : profiles) {
          final Element profileElement = (Element)elem;
          final boolean isDefault = "true".equals(profileElement.getAttributeValue("default"));
          if (isDefault) {
            AnnotationProcessorProfileSerializer.readExternal(myDefaultProcessorsProfile, profileElement);
          }
          else {
            final ProcessorConfigProfile profile = new ProcessorConfigProfileImpl("");
            AnnotationProcessorProfileSerializer.readExternal(profile, profileElement);
            myModuleProcessorProfiles.add(profile);
          }
        }
      }
      else {
        // assuming older format
        loadProfilesFromOldFormat(annotationProcessingSettings);
      }
    }
  }

  private void loadProfilesFromOldFormat(Element processing) {
    // collect data
    final boolean isEnabled = Boolean.parseBoolean(processing.getAttributeValue(ENABLED, "false"));
    final boolean isUseClasspath = Boolean.parseBoolean(processing.getAttributeValue("useClasspath", "true"));
    final StringBuilder processorPath = new StringBuilder();
    final Set<String> optionPairs = new HashSet<String>();
    final Set<String> processors = new HashSet<String>();
    final List<Pair<String, String>> modulesToProcess = new ArrayList<Pair<String, String>>();

    for (Object child : processing.getChildren("processorPath")) {
      final Element pathElement = (Element)child;
      final String path = pathElement.getAttributeValue("value", (String)null);
      if (path != null) {
        if (processorPath.length() > 0) {
          processorPath.append(File.pathSeparator);
        }
        processorPath.append(path);
      }
    }

    for (Object child : processing.getChildren("processor")) {
      final Element processorElement = (Element)child;
      final String proc = processorElement.getAttributeValue(NAME, (String)null);
      if (proc != null) {
        processors.add(proc);
      }
      final StringTokenizer tokenizer = new StringTokenizer(processorElement.getAttributeValue("options", ""), " ", false);
      while (tokenizer.hasMoreTokens()) {
        final String pair = tokenizer.nextToken();
        optionPairs.add(pair);
      }
    }

    for (Object child : processing.getChildren("processModule")) {
      final Element moduleElement = (Element)child;
      final String name = moduleElement.getAttributeValue(NAME, (String)null);
      if (name == null) {
        continue;
      }
      final String dir = moduleElement.getAttributeValue("generatedDirName", (String)null);
      modulesToProcess.add(Pair.create(name, dir));
    }

    myDefaultProcessorsProfile.setEnabled(false);
    myDefaultProcessorsProfile.setObtainProcessorsFromClasspath(isUseClasspath);
    if (processorPath.length() > 0) {
      myDefaultProcessorsProfile.setProcessorPath(processorPath.toString());
    }
    if (!optionPairs.isEmpty()) {
      for (String pair : optionPairs) {
        final int index = pair.indexOf("=");
        if (index > 0) {
          myDefaultProcessorsProfile.setOption(pair.substring(0, index), pair.substring(index + 1));
        }
      }
    }
    for (String processor : processors) {
      myDefaultProcessorsProfile.addProcessor(processor);
    }

    final Map<String, Set<String>> dirNameToModulesMap = new HashMap<String, Set<String>>();
    for (Pair<String, String> moduleDirPair : modulesToProcess) {
      final String dir = moduleDirPair.getSecond();
      Set<String> set = dirNameToModulesMap.get(dir);
      if (set == null) {
        set = new HashSet<String>();
        dirNameToModulesMap.put(dir, set);
      }
      set.add(moduleDirPair.getFirst());
    }

    int profileIndex = 0;
    for (Map.Entry<String, Set<String>> entry : dirNameToModulesMap.entrySet()) {
      final String dirName = entry.getKey();
      final ProcessorConfigProfile profile = new ProcessorConfigProfileImpl(myDefaultProcessorsProfile);
      profile.setName("Profile" + (++profileIndex));
      profile.setEnabled(isEnabled);
      profile.setGeneratedSourcesDirectoryName(dirName, false);
      for (String moduleName : entry.getValue()) {
        profile.addModuleName(moduleName);
      }
      myModuleProcessorProfiles.add(profile);
    }
  }

  @NotNull
  public ProcessorConfigProfile getAnnotationProcessingConfiguration(Module module) {
    Map<Module, ProcessorConfigProfile> map = myProcessorsProfilesMap;
    if (map == null) {
      map = new HashMap<Module, ProcessorConfigProfile>();
      final Map<String, Module> namesMap = new HashMap<String, Module>();
      for (Module m : ModuleManager.getInstance(module.getProject()).getModules()) {
        namesMap.put(m.getName(), m);
      }
      if (!namesMap.isEmpty()) {
        for (ProcessorConfigProfile profile : myModuleProcessorProfiles) {
          for (String name : profile.getModuleNames()) {
            final Module mod = namesMap.get(name);
            if (mod != null) {
              map.put(mod, profile);
            }
          }
        }
      }
      myProcessorsProfilesMap = map;
    }
    final ProcessorConfigProfile profile = map.get(module);
    return profile != null ? profile : myDefaultProcessorsProfile;
  }

  @NotNull
  public ProcessorConfigProfile getDefaultProcessorProfile() {
    return myDefaultProcessorsProfile;
  }

  @NotNull
  public List<ProcessorConfigProfile> getModuleProcessorProfiles() {
    return myModuleProcessorProfiles;
  }

  public void setDefaultProcessorProfile(ProcessorConfigProfile profile) {
    myDefaultProcessorsProfile.initFrom(profile);
  }

  public void setModuleProcessorProfiles(Collection<ProcessorConfigProfile> moduleProfiles) {
    myModuleProcessorProfiles.clear();
    myModuleProcessorProfiles.addAll(moduleProfiles);
    myProcessorsProfilesMap = null;
  }

  public boolean isAnnotationProcessorsEnabled() {
    if (myDefaultProcessorsProfile.isEnabled()) {
      return true;
    }
    for (ProcessorConfigProfile profile : myModuleProcessorProfiles) {
      if (profile.isEnabled()) {
        return true;
      }
    }
    return false;
  }

  public void removeModuleProcessorProfile(ProcessorConfigProfile profile) {
    myModuleProcessorProfiles.remove(profile);
    myProcessorsProfilesMap = null; // clear cache
  }

  public void addModuleProcessorProfile(@NotNull ProcessorConfigProfile profile) {
    myModuleProcessorProfiles.add(profile);
    myProcessorsProfilesMap = null; // clear cache
  }

  @Nullable
  public ProcessorConfigProfile findModuleProcessorProfile(@NotNull String name) {
    for (ProcessorConfigProfile profile : myModuleProcessorProfiles) {
      if (name.equals(profile.getName())) {
        return profile;
      }
    }

    return null;
  }

  public void setAddNotNullAssertions(boolean addNotNullAssertions) {
    myAddNotNullAssertions = addNotNullAssertions;
  }

  public boolean isAddNotNullAssertions() {
    return myAddNotNullAssertions;
  }
}
