/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.container.internal.plugin;

import consulo.container.internal.ContainerLogger;
import consulo.container.plugin.*;
import consulo.util.nodep.LoggerRt;
import consulo.util.nodep.text.StringUtilRt;
import consulo.util.nodep.xml.SimpleXmlParsingException;
import consulo.util.nodep.xml.SimpleXmlReader;
import consulo.util.nodep.xml.node.SimpleXmlElement;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author mike
 */
public class PluginDescriptorImpl extends PluginDescriptorStub {
    private static final String META_INF_VERSION = "META-INF/versions/";

    public static final PluginDescriptorImpl[] EMPTY_ARRAY = new PluginDescriptorImpl[0];

    private @Nullable String myName;
    private @Nullable PluginId myId;
    private @Nullable String myResourceBundleBaseName;
    private @Nullable String myLocalization;
    private @Nullable String myChangeNotes;
    private @Nullable String myVersion;
    private @Nullable String myPlatformVersion;
    private @Nullable String myVendor;
    private @Nullable String myVendorEmail;
    private @Nullable String myVendorUrl;
    private @Nullable String url;
    private final File myPath;
    private final byte[] myIconBytes;
    private final byte[] myDarkIconBytes;
    private final boolean myIsPreInstalled;
    private PluginId[] myDependencies = PluginId.EMPTY_ARRAY;
    private PluginId[] myOptionalDependencies = PluginId.EMPTY_ARRAY;
    private PluginId[] myIncompatibleWithPlugins = PluginId.EMPTY_ARRAY;

    private List<SimpleXmlElement> myActionsElements = Collections.emptyList();

    private Map<PluginPermissionType, PluginPermissionDescriptor> myPermissionDescriptors = Collections.emptyMap();

    private boolean myDeleted = false;
    private @Nullable ClassLoader myLoader;
    private @Nullable ModuleLayer myModuleLayer;

    private Set<String> myTags = Collections.emptySet();

    private @Nullable String myDescriptionChildText;
    private PluginDescriptorStatus myStatus = PluginDescriptorStatus.OK;
    private boolean myExperimental;

    private @Nullable List<ClassPathItem> myClassPathItems;
    private boolean myEnabledIndex;

    public PluginDescriptorImpl(File pluginPath, byte[] iconBytes, byte[] darkIconBytes, boolean isPreInstalled) {
        myPath = pluginPath;
        myIconBytes = iconBytes;
        myDarkIconBytes = darkIconBytes;
        myIsPreInstalled = isPreInstalled;
    }

    @Override
    public File getPath() {
        return myPath;
    }

    @Override
    public Path getNioPath() {
        return myPath.toPath();
    }

    public void readExternal(InputStream stream, ContainerLogger log) throws SimpleXmlParsingException {
        SimpleXmlElement element = SimpleXmlReader.parse(stream);
        readExternal(element, log);
    }

    private void readExternal(SimpleXmlElement element, ContainerLogger log) throws SimpleXmlParsingException {
        PluginBean pluginBean = PluginBeanParser.parseBean(element, null);
        assert pluginBean != null;
        url = pluginBean.url;
        myName = pluginBean.name;
        String idString = pluginBean.id;
        if (StringUtilRt.isEmpty(idString)) {
            idString = myName;
        }
        myId = idString == null ? null : PluginId.getId(idString);
        myResourceBundleBaseName = StringUtilRt.isEmpty(pluginBean.resourceBundle) ? null : pluginBean.resourceBundle;
        myLocalization = StringUtilRt.nullize(pluginBean.localize);

        myDescriptionChildText = pluginBean.description;
        myChangeNotes = pluginBean.changeNotes;
        myVersion = StringUtilRt.nullize(pluginBean.pluginVersion);
        myPlatformVersion = pluginBean.platformVersion;
        myExperimental = pluginBean.experimental;

        if (pluginBean.vendor != null) {
            myVendor = pluginBean.vendor.name;
            myVendorEmail = pluginBean.vendor.email;
            myVendorUrl = pluginBean.vendor.url;
        }

        // preserve items order as specified in xml (filterBadPlugins will not fail if module comes first)
        Set<PluginId> dependentPlugins = new LinkedHashSet<>();
        // we always depend to core plugin, but prevent recursion
        if (!PluginIds.CONSULO_BASE.equals(myId)) {
            dependentPlugins.add(PluginIds.CONSULO_BASE);
        }
        Set<PluginId> optionalDependentPlugins = new LinkedHashSet<>();
        if (pluginBean.dependencies != null) {
            for (PluginDependency dependency : pluginBean.dependencies) {
                String text = dependency.pluginId;
                if (!StringUtilRt.isEmpty(text)) {
                    PluginId id = PluginId.getId(Objects.requireNonNull(text));
                    dependentPlugins.add(id);
                    if (dependency.optional) {
                        optionalDependentPlugins.add(id);
                    }
                }
            }
        }

        Set<PluginId> incompatibleWithPlugins = new LinkedHashSet<>();
        for (String pluginId : pluginBean.incompatibleWith) {
            if (!StringUtilRt.isEmpty(pluginId)) {
                incompatibleWithPlugins.add(PluginId.getId(pluginId));
            }
        }
        myIncompatibleWithPlugins = incompatibleWithPlugins.isEmpty() ? PluginId.EMPTY_ARRAY : incompatibleWithPlugins.toArray(new PluginId[incompatibleWithPlugins.size()]);

        myDependencies = dependentPlugins.isEmpty() ? PluginId.EMPTY_ARRAY : dependentPlugins.toArray(new PluginId[dependentPlugins.size()]);
        myOptionalDependencies = optionalDependentPlugins.isEmpty() ? PluginId.EMPTY_ARRAY : optionalDependentPlugins.toArray(new PluginId[optionalDependentPlugins.size()]);

        if (!pluginBean.tags.isEmpty()) {
            myTags = Collections.unmodifiableSet(pluginBean.tags);
        }

        if (pluginBean.experimental) {
            Set<String> oldTags = new TreeSet<>(myTags);
            oldTags.add(EXPERIMENTAL_TAG);

            myTags = Collections.unmodifiableSet(oldTags);
        }

        for (Map.Entry<String, Set<String>> permissionEntry : pluginBean.permissions.entrySet()) {
            try {
                PluginPermissionType permissionType = PluginPermissionType.valueOf(permissionEntry.getKey());
                if (myPermissionDescriptors.isEmpty()) {
                    myPermissionDescriptors = new HashMap<>();
                }

                myPermissionDescriptors.put(permissionType, new PluginPermissionDescriptor(permissionType, permissionEntry.getValue()));
            }
            catch (IllegalArgumentException e) {
                log.warn("Unknown permissionType " + permissionEntry.getKey() + ", plugin: " + myId);
            }
        }

        myActionsElements = pluginBean.actions;
    }

    private static <T> List<T> mergeElements(List<T> original, List<T> additional) {
        if (additional.isEmpty()) {
            return original;
        }

        List<T> result = original;
        if (original.isEmpty()) {
            result = new ArrayList<>(additional);
        }
        else {
            result.addAll(additional);
        }

        return result;
    }

    @Override
    public Set<String> getTags() {
        return myTags;
    }

    @Override
    public @Nullable String getDescription() {
        return myDescriptionChildText;
    }

    @Override
    public @Nullable String getChangeNotes() {
        return myChangeNotes;
    }

    @Override
    public byte[] getIconBytes(boolean isDarkTheme) {
        if (isDarkTheme && myDarkIconBytes.length > 0) {
            return myDarkIconBytes;
        }
        return myIconBytes;
    }

    @Override
    public @Nullable String getName() {
        return myName;
    }

    @Override
    public PluginId[] getDependentPluginIds() {
        return myDependencies;
    }

    @Override
    public PluginId[] getOptionalDependentPluginIds() {
        return myOptionalDependencies;
    }

    @Override
    public PluginId[] getIncompatibleWithPluginIds() {
        return myIncompatibleWithPlugins;
    }

    @Override
    public @Nullable String getVendor() {
        return myVendor;
    }

    @Override
    public @Nullable String getVersion() {
        return myVersion;
    }

    @Override
    public @Nullable String getPlatformVersion() {
        return myPlatformVersion;
    }

    @Override
    public @Nullable String getResourceBundleBaseName() {
        return myResourceBundleBaseName;
    }

    @Override
    public @Nullable String getLocalization() {
        return myLocalization;
    }

    public List<ClassPathItem> getClassPathItems(Set<PluginId> enabledPluginIds) {
        if (myClassPathItems == null) {
            myClassPathItems = analyzeClassPath();
        }
        List<ClassPathItem> items = new ArrayList<>(myClassPathItems.size());
        for (ClassPathItem classPathItem : myClassPathItems) {
            if (classPathItem.accept(enabledPluginIds)) {
                items.add(classPathItem);
            }
        }
        return items;
    }

    public List<File> getClassPathFiles(Set<PluginId> enabledPluginIds) {
        List<ClassPathItem> items = getClassPathItems(enabledPluginIds);
        List<File> files = new ArrayList<>(items.size());
        for (ClassPathItem item : items) {
            files.add(item.getPath());
        }
        return files;
    }

    public boolean isEnabledIndex() {
        return myEnabledIndex;
    }

    private List<ClassPathItem> analyzeClassPath() {
        if (!myPath.isDirectory()) {
            return Collections.emptyList();
        }

        List<ClassPathItem> result = new ArrayList<ClassPathItem>();
        File libDir = new File(myPath, "lib");

        Map<String, Set<String>> data = readIndex(libDir);
        if (!data.isEmpty()) {
            myEnabledIndex = true;
        }
        fillLibs(libDir, result, data);
        return result;
    }

    private Map<String, Set<String>> readIndex(File libDir) {
        File jarIndex = new File(libDir, "index.txt");
        if (!jarIndex.exists()) {
            return Collections.emptyMap();
        }

        try {
            List<String> lines = Files.readAllLines(jarIndex.toPath());

            Map<String, Set<String>> data = new HashMap<>();

            String currentJarFile = null;
            for (String line : lines) {
                if (line.isEmpty()) {
                    continue;
                }

                if (line.charAt(0) == '#') {
                    currentJarFile = line.substring(1, line.length());
                }
                else {
                    if (currentJarFile == null) {
                        throw new IllegalArgumentException("Broken jar index: " + jarIndex);
                    }

                    Set<String> paths = data.computeIfAbsent(currentJarFile, s -> new HashSet<>());
                    if (line.startsWith(META_INF_VERSION)) {
                        int metaInfVersionIndex = line.indexOf('/', META_INF_VERSION.length() + 1);
                        paths.add(line.substring(metaInfVersionIndex + 1, line.length()));
                    } else {
                        paths.add(line);
                    }
                }
            }

            return data;
        }
        catch (Exception e) {
            LoggerRt.getInstance(PluginDescriptorImpl.class).error("Fail to read: " + jarIndex, e);
        }

        return Collections.emptyMap();
    }

    private static void fillLibs(File libDirectory, List<ClassPathItem> result, Map<String, Set<String>> data) {
        File[] files = libDirectory.listFiles();
        if (files != null && files.length > 0) {
            for (File f : files) {
                String fileName = f.getName();

                if (StringUtilRt.endsWithIgnoreCase(fileName, ".jar")) {
                    File requiresFile = new File(libDirectory, fileName + ".requires");

                    Set<String> index = data.get(fileName);

                    if (requiresFile.exists()) {
                        List<ClassPathPluginSet> pluginSets = readRequires(requiresFile);
                        result.add(new ClassPathItem(f, pluginSets, index));
                    }
                    else {
                        result.add(new ClassPathItem(f, Collections.emptyList(), index));
                    }
                }
            }
        }
    }

    private static List<ClassPathPluginSet> readRequires(File xmlRequires) {
        try {
            List<ClassPathPluginSet> sets = new ArrayList<>();
            SimpleXmlElement rootElement = SimpleXmlReader.parse(xmlRequires);
            for (SimpleXmlElement plugins : rootElement.getChildren("plugins")) {
                ClassPathPluginSet set = new ClassPathPluginSet();
                sets.add(set);
                for (SimpleXmlElement plugin : plugins.getChildren("plugin")) {
                    set.add(PluginId.getId(plugin.getText()));
                }
            }
            return sets;
        }
        catch (SimpleXmlParsingException ignored) {
        }
        return Collections.emptyList();
    }

    @Override
    public List<SimpleXmlElement> getActionsDescriptionElements() {
        return myActionsElements;
    }

    @Override
    public @Nullable String getVendorEmail() {
        return myVendorEmail;
    }

    @Override
    public @Nullable String getVendorUrl() {
        return myVendorUrl;
    }

    @Override
    public @Nullable String getUrl() {
        return url;
    }

    @Override
    public @Nullable PluginPermissionDescriptor getPermissionDescriptor(PluginPermissionType permissionType) {
        return myPermissionDescriptors.get(permissionType);
    }

    @Override
    public String toString() {
        return "PluginDescriptor[name='" + myName + "', classpath='" + myPath + "']";
    }

    @Override
    public boolean isDeleted() {
        return myDeleted;
    }

    public void setDeleted(boolean deleted) {
        myDeleted = deleted;
    }

    public void setLoader(ClassLoader loader) {
        myLoader = loader;
    }

    public void setModuleLayer(ModuleLayer moduleLayer) {
        myModuleLayer = moduleLayer;
    }

    @Override
    public @Nullable ModuleLayer getModuleLayer() {
        return myModuleLayer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PluginDescriptorImpl)) {
            return false;
        }

        PluginDescriptorImpl pluginDescriptor = (PluginDescriptorImpl) o;

        return myName == null ? pluginDescriptor.myName == null : myName.equals(pluginDescriptor.myName);
    }

    @Override
    public int hashCode() {
        return myName != null ? myName.hashCode() : 0;
    }

    @Override
    public PluginId getPluginId() {
        return Objects.requireNonNull(myId);
    }

    @Override
    public ClassLoader getPluginClassLoader() {
        if (myLoader == null) {
            throw new IllegalArgumentException("Do not call #getPluginClassLoader() when plugin is not loaded");
        }
        return myLoader;
    }

    @Override
    public PluginDescriptorStatus getStatus() {
        return myStatus;
    }

    public void setStatus(PluginDescriptorStatus status) {
        myStatus = status;
    }

    @Override
    public boolean isExperimental() {
        return myExperimental;
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public boolean isBundled() {
        return myIsPreInstalled;
    }
}
