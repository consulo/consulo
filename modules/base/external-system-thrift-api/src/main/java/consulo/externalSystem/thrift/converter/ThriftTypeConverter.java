/*
 * Copyright 2013-2026 consulo.io
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
package consulo.externalSystem.thrift.converter;

import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.Key;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.project.*;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.*;
import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.externalSystem.rt.model.ExternalSystemSourceType;
import consulo.externalSystem.rt.model.LocationAwareExternalSystemException;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.thrift.*;
import consulo.module.content.layer.orderEntry.DependencyScope;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts between Thrift structs and Consulo domain objects.
 *
 * @author VISTALL
 * @since 2026-03-20
 */
public final class ThriftTypeConverter {

    private ThriftTypeConverter() {
    }

    // ============ TaskId ============

    public static ThriftTaskId toThrift(ExternalSystemTaskId id) {
        ThriftTaskId result = new ThriftTaskId();
        result.setType(toThrift(id.getType()));
        result.setProjectId(id.getIdeProjectId());
        result.setProjectSystemId(toThrift(id.getProjectSystemId()));
        result.setId(id.hashCode());
        return result;
    }

    public static ExternalSystemTaskId fromThrift(ThriftTaskId thrift) {
        return ExternalSystemTaskId.create(
            fromThrift(thrift.getProjectSystemId()),
            fromThrift(thrift.getType()),
            thrift.getProjectId()
        );
    }

    // ============ TaskType ============

    public static ThriftTaskType toThrift(ExternalSystemTaskType type) {
        switch (type) {
            case RESOLVE_PROJECT:
                return ThriftTaskType.RESOLVE_PROJECT;
            case REFRESH_TASKS_LIST:
                return ThriftTaskType.REFRESH_TASKS_LIST;
            case EXECUTE_TASK:
                return ThriftTaskType.EXECUTE_TASK;
            default:
                throw new IllegalArgumentException("Unknown task type: " + type);
        }
    }

    public static ExternalSystemTaskType fromThrift(ThriftTaskType type) {
        switch (type) {
            case RESOLVE_PROJECT:
                return ExternalSystemTaskType.RESOLVE_PROJECT;
            case REFRESH_TASKS_LIST:
                return ExternalSystemTaskType.REFRESH_TASKS_LIST;
            case EXECUTE_TASK:
                return ExternalSystemTaskType.EXECUTE_TASK;
            default:
                throw new IllegalArgumentException("Unknown thrift task type: " + type);
        }
    }

    // ============ ProjectSystemId ============

    public static ThriftProjectSystemId toThrift(ProjectSystemId id) {
        ThriftProjectSystemId result = new ThriftProjectSystemId();
        result.setId(id.getId());
        return result;
    }

    public static ProjectSystemId fromThrift(ThriftProjectSystemId thrift) {
        return new ProjectSystemId(thrift.getId());
    }

    // ============ TaskNotificationEvent ============

    public static ThriftTaskNotificationEvent toThrift(ExternalSystemTaskNotificationEvent event) {
        ThriftTaskNotificationEvent result = new ThriftTaskNotificationEvent();
        result.setId(toThrift(event.getId()));
        result.setDescription(event.getDescription());
        return result;
    }

    public static ExternalSystemTaskNotificationEvent fromThrift(ThriftTaskNotificationEvent thrift) {
        return new ExternalSystemTaskNotificationEvent(fromThrift(thrift.getId()), thrift.getDescription());
    }

    // ============ ExternalSystemException ============

    public static ThriftExternalSystemException toThrift(ExternalSystemException ex) {
        ThriftExternalSystemException result = new ThriftExternalSystemException();
        result.setMessage(ex.getMessage() != null ? ex.getMessage() : "");
        result.setOriginalReason(ex.getOriginalReason() != null ? ex.getOriginalReason() : "");
        result.setQuickFixes(ex.getQuickFixes() != null ? Arrays.asList(ex.getQuickFixes()) : Collections.emptyList());
        if (ex instanceof LocationAwareExternalSystemException) {
            LocationAwareExternalSystemException locEx = (LocationAwareExternalSystemException) ex;
            if (locEx.getFilePath() != null) {
                result.setFilePath(locEx.getFilePath());
            }
            result.setLine(locEx.getLine());
            result.setColumn(locEx.getColumn());
        }
        return result;
    }

    public static ExternalSystemException fromThrift(ThriftExternalSystemException thrift) {
        String[] quickFixes = thrift.getQuickFixes() != null
            ? thrift.getQuickFixes().toArray(new String[0])
            : new String[0];

        if (thrift.isSetFilePath()) {
            return new LocationAwareExternalSystemException(
                thrift.getMessage(),
                thrift.getFilePath(),
                thrift.isSetLine() ? thrift.getLine() : -1,
                thrift.isSetColumn() ? thrift.getColumn() : -1,
                quickFixes
            );
        }

        return new ExternalSystemException(thrift.getMessage(), quickFixes);
    }

    // ============ SourceType ============

    public static ThriftSourceType toThrift(ExternalSystemSourceType type) {
        switch (type) {
            case SOURCE:
                return ThriftSourceType.SOURCE;
            case TEST:
                return ThriftSourceType.TEST;
            case EXCLUDED:
                return ThriftSourceType.EXCLUDED;
            case SOURCE_GENERATED:
                return ThriftSourceType.SOURCE_GENERATED;
            case TEST_GENERATED:
                return ThriftSourceType.TEST_GENERATED;
            case RESOURCE:
                return ThriftSourceType.RESOURCE;
            case TEST_RESOURCE:
                return ThriftSourceType.TEST_RESOURCE;
            default:
                throw new IllegalArgumentException("Unknown source type: " + type);
        }
    }

    public static ExternalSystemSourceType fromThrift(ThriftSourceType type) {
        switch (type) {
            case SOURCE:
                return ExternalSystemSourceType.SOURCE;
            case TEST:
                return ExternalSystemSourceType.TEST;
            case EXCLUDED:
                return ExternalSystemSourceType.EXCLUDED;
            case SOURCE_GENERATED:
                return ExternalSystemSourceType.SOURCE_GENERATED;
            case TEST_GENERATED:
                return ExternalSystemSourceType.TEST_GENERATED;
            case RESOURCE:
                return ExternalSystemSourceType.RESOURCE;
            case TEST_RESOURCE:
                return ExternalSystemSourceType.TEST_RESOURCE;
            default:
                throw new IllegalArgumentException("Unknown thrift source type: " + type);
        }
    }

    // ============ LibraryPathType ============

    public static ThriftLibraryPathType toThrift(LibraryPathType type) {
        switch (type) {
            case BINARY:
                return ThriftLibraryPathType.BINARY;
            case SOURCE:
                return ThriftLibraryPathType.SOURCE;
            case DOC:
                return ThriftLibraryPathType.DOC;
            default:
                throw new IllegalArgumentException("Unknown library path type: " + type);
        }
    }

    public static LibraryPathType fromThrift(ThriftLibraryPathType type) {
        switch (type) {
            case BINARY:
                return LibraryPathType.BINARY;
            case SOURCE:
                return LibraryPathType.SOURCE;
            case DOC:
                return LibraryPathType.DOC;
            default:
                throw new IllegalArgumentException("Unknown thrift library path type: " + type);
        }
    }

    // ============ DependencyScope ============

    public static ThriftDependencyScope toThrift(DependencyScope scope) {
        switch (scope) {
            case COMPILE:
                return ThriftDependencyScope.COMPILE;
            case TEST:
                return ThriftDependencyScope.TEST;
            case RUNTIME:
                return ThriftDependencyScope.RUNTIME;
            case PROVIDED:
                return ThriftDependencyScope.PROVIDED;
            default:
                throw new IllegalArgumentException("Unknown dependency scope: " + scope);
        }
    }

    public static DependencyScope fromThrift(ThriftDependencyScope scope) {
        switch (scope) {
            case COMPILE:
                return DependencyScope.COMPILE;
            case TEST:
                return DependencyScope.TEST;
            case RUNTIME:
                return DependencyScope.RUNTIME;
            case PROVIDED:
                return DependencyScope.PROVIDED;
            default:
                throw new IllegalArgumentException("Unknown thrift dependency scope: " + scope);
        }
    }

    // ============ LibraryLevel ============

    public static ThriftLibraryLevel toThrift(LibraryLevel level) {
        switch (level) {
            case PROJECT:
                return ThriftLibraryLevel.PROJECT;
            case MODULE:
                return ThriftLibraryLevel.MODULE;
            default:
                throw new IllegalArgumentException("Unknown library level: " + level);
        }
    }

    public static LibraryLevel fromThrift(ThriftLibraryLevel level) {
        switch (level) {
            case PROJECT:
                return LibraryLevel.PROJECT;
            case MODULE:
                return LibraryLevel.MODULE;
            default:
                throw new IllegalArgumentException("Unknown thrift library level: " + level);
        }
    }

    // ============ ProjectData ============

    public static ThriftProjectData toThrift(ProjectData data) {
        ThriftProjectData result = new ThriftProjectData();
        result.setExternalName(data.getExternalName());
        result.setInternalName(data.getInternalName());
        result.setOwner(toThrift(data.getOwner()));
        result.setLinkedExternalProjectPath(data.getLinkedExternalProjectPath());
        if (data.getIdeProjectFileDirectoryPath() != null) {
            result.setIdeProjectFileDirectoryPath(data.getIdeProjectFileDirectoryPath());
        }
        return result;
    }

    public static ProjectData fromThrift(ThriftProjectData thrift) {
        ProjectData result = new ProjectData(
            fromThrift(thrift.getOwner()),
            thrift.getExternalName(),
            thrift.isSetIdeProjectFileDirectoryPath() ? thrift.getIdeProjectFileDirectoryPath() : "",
            thrift.getLinkedExternalProjectPath()
        );
        result.setInternalName(thrift.getInternalName());
        return result;
    }

    // ============ ModuleData ============

    public static ThriftModuleData toThrift(ModuleData data) {
        ThriftModuleData result = new ThriftModuleData();
        result.setId(data.getId());
        result.setExternalName(data.getExternalName());
        result.setInternalName(data.getInternalName());
        result.setOwner(toThrift(data.getOwner()));
        result.setExternalConfigPath(data.getLinkedExternalProjectPath());
        if (data.getModuleDirPath() != null) {
            result.setModuleDirPath(data.getModuleDirPath());
        }
        if (data.getGroup() != null) {
            result.setGroup(data.getGroup());
        }
        if (data.getVersion() != null) {
            result.setVersion(data.getVersion());
        }
        result.setArtifacts(data.getArtifacts().stream()
            .map(f -> f.getPath())
            .collect(Collectors.toList()));
        result.setInheritProjectCompileOutputPath(data.isInheritProjectCompileOutputPath());

        Map<ThriftSourceType, String> outputPaths = new HashMap<>();
        for (ExternalSystemSourceType sourceType : ExternalSystemSourceType.values()) {
            String path = data.getCompileOutputPath(sourceType);
            if (path != null) {
                outputPaths.put(toThrift(sourceType), path);
            }
        }
        result.setCompileOutputPaths(outputPaths);
        return result;
    }

    public static ModuleData fromThrift(ThriftModuleData thrift) {
        ModuleData result = new ModuleData(
            thrift.getId(),
            fromThrift(thrift.getOwner()),
            thrift.getExternalName(),
            thrift.isSetModuleDirPath() ? thrift.getModuleDirPath() : "",
            thrift.getExternalConfigPath()
        );
        result.setInternalName(thrift.getInternalName());
        if (thrift.isSetGroup()) {
            result.setGroup(thrift.getGroup());
        }
        if (thrift.isSetVersion()) {
            result.setVersion(thrift.getVersion());
        }
        result.setInheritProjectCompileOutputPath(thrift.isInheritProjectCompileOutputPath());
        if (thrift.getCompileOutputPaths() != null) {
            for (Map.Entry<ThriftSourceType, String> entry : thrift.getCompileOutputPaths().entrySet()) {
                result.setCompileOutputPath(fromThrift(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    // ============ LibraryData ============

    public static ThriftLibraryData toThrift(LibraryData data) {
        ThriftLibraryData result = new ThriftLibraryData();
        result.setExternalName(data.getExternalName());
        result.setInternalName(data.getInternalName());
        result.setOwner(toThrift(data.getOwner()));
        result.setUnresolved(data.isUnresolved());

        Map<ThriftLibraryPathType, List<String>> paths = new HashMap<>();
        for (LibraryPathType pathType : LibraryPathType.values()) {
            Set<String> typePaths = data.getPaths(pathType);
            if (!typePaths.isEmpty()) {
                paths.put(toThrift(pathType), new ArrayList<>(typePaths));
            }
        }
        result.setPaths(paths);
        return result;
    }

    public static LibraryData fromThrift(ThriftLibraryData thrift) {
        LibraryData result = new LibraryData(
            fromThrift(thrift.getOwner()),
            thrift.getExternalName(),
            thrift.isUnresolved()
        );
        result.setInternalName(thrift.getInternalName());
        if (thrift.getPaths() != null) {
            for (Map.Entry<ThriftLibraryPathType, List<String>> entry : thrift.getPaths().entrySet()) {
                LibraryPathType pathType = fromThrift(entry.getKey());
                for (String path : entry.getValue()) {
                    result.addPath(pathType, path);
                }
            }
        }
        return result;
    }

    // ============ ContentRootData ============

    public static ThriftContentRootData toThrift(ContentRootData data) {
        ThriftContentRootData result = new ThriftContentRootData();
        result.setRootPath(data.getRootPath());

        Map<ThriftSourceType, List<ThriftSourceRoot>> sourceRoots = new HashMap<>();
        for (ExternalSystemSourceType sourceType : ExternalSystemSourceType.values()) {
            Collection<ContentRootData.SourceRoot> roots = data.getPaths(sourceType);
            if (!roots.isEmpty()) {
                List<ThriftSourceRoot> thriftRoots = new ArrayList<>();
                for (ContentRootData.SourceRoot root : roots) {
                    ThriftSourceRoot thriftRoot = new ThriftSourceRoot();
                    thriftRoot.setPath(root.getPath());
                    if (root.getPackagePrefix() != null) {
                        thriftRoot.setPackagePrefix(root.getPackagePrefix());
                    }
                    thriftRoots.add(thriftRoot);
                }
                sourceRoots.put(toThrift(sourceType), thriftRoots);
            }
        }
        result.setData(sourceRoots);
        return result;
    }

    public static ContentRootData fromThrift(ThriftContentRootData thrift, ProjectSystemId owner) {
        ContentRootData result = new ContentRootData(owner, thrift.getRootPath());
        if (thrift.getData() != null) {
            for (Map.Entry<ThriftSourceType, List<ThriftSourceRoot>> entry : thrift.getData().entrySet()) {
                ExternalSystemSourceType sourceType = fromThrift(entry.getKey());
                for (ThriftSourceRoot root : entry.getValue()) {
                    result.storePath(sourceType, root.getPath(), root.isSetPackagePrefix() ? root.getPackagePrefix() : null);
                }
            }
        }
        return result;
    }

    // ============ TaskData ============

    public static ThriftTaskData toThrift(TaskData data) {
        ThriftTaskData result = new ThriftTaskData();
        result.setName(data.getName());
        result.setOwner(toThrift(data.getOwner()));
        result.setLinkedExternalProjectPath(data.getLinkedExternalProjectPath());
        if (data.getDescription() != null) {
            result.setDescription(data.getDescription());
        }
        return result;
    }

    public static TaskData fromThrift(ThriftTaskData thrift) {
        return new TaskData(
            fromThrift(thrift.getOwner()),
            thrift.getName(),
            thrift.getLinkedExternalProjectPath(),
            thrift.isSetDescription() ? thrift.getDescription() : null
        );
    }

    // ============ ModuleDependencyData ============

    public static ThriftModuleDependencyData toThriftModuleDep(ModuleDependencyData data) {
        ThriftModuleDependencyData result = new ThriftModuleDependencyData();
        result.setOwner(toThrift(data.getOwner()));
        result.setOwnerModuleId(data.getOwnerModule().getId());
        result.setTargetModuleId(data.getTarget().getId());
        result.setScope(toThrift(data.getScope()));
        result.setExported(data.isExported());
        return result;
    }

    public static ModuleDependencyData fromThriftModuleDep(ThriftModuleDependencyData thrift) {
        ProjectSystemId owner = fromThrift(thrift.getOwner());
        ModuleData ownerModule = new ModuleData(thrift.getOwnerModuleId(), owner, thrift.getOwnerModuleId(), "", "");
        ModuleData targetModule = new ModuleData(thrift.getTargetModuleId(), owner, thrift.getTargetModuleId(), "", "");
        ModuleDependencyData result = new ModuleDependencyData(ownerModule, targetModule);
        result.setScope(fromThrift(thrift.getScope()));
        result.setExported(thrift.isExported());
        return result;
    }

    // ============ LibraryDependencyData ============

    public static ThriftLibraryDependencyData toThriftLibraryDep(LibraryDependencyData data) {
        ThriftLibraryDependencyData result = new ThriftLibraryDependencyData();
        result.setOwner(toThrift(data.getOwner()));
        result.setOwnerModuleId(data.getOwnerModule().getId());
        result.setTargetLibraryName(data.getTarget().getExternalName());
        result.setScope(toThrift(data.getScope()));
        result.setExported(data.isExported());
        result.setLevel(toThrift(data.getLevel()));
        result.setTarget(toThrift(data.getTarget()));
        return result;
    }

    public static LibraryDependencyData fromThriftLibraryDep(ThriftLibraryDependencyData thrift) {
        ProjectSystemId owner = fromThrift(thrift.getOwner());
        ModuleData ownerModule = new ModuleData(thrift.getOwnerModuleId(), owner, thrift.getOwnerModuleId(), "", "");
        LibraryData target = fromThrift(thrift.getTarget());
        LibraryDependencyData result = new LibraryDependencyData(ownerModule, target, fromThrift(thrift.getLevel()));
        result.setScope(fromThrift(thrift.getScope()));
        result.setExported(thrift.isExported());
        return result;
    }

    // ============ DataNode tree ============

    @SuppressWarnings("unchecked")
    public static ThriftDataNode toThrift(DataNode<?> node) {
        ThriftDataNode result = new ThriftDataNode();
        Key<?> key = node.getKey();
        result.setKey(key.toString());

        Object data = node.getData();
        if (data instanceof ProjectData) {
            result.setProjectData(toThrift((ProjectData) data));
        }
        else if (data instanceof ModuleData) {
            result.setModuleData(toThrift((ModuleData) data));
        }
        else if (data instanceof LibraryData) {
            result.setLibraryData(toThrift((LibraryData) data));
        }
        else if (data instanceof ContentRootData) {
            result.setContentRootData(toThrift((ContentRootData) data));
        }
        else if (data instanceof ModuleDependencyData) {
            result.setModuleDependencyData(toThriftModuleDep((ModuleDependencyData) data));
        }
        else if (data instanceof LibraryDependencyData) {
            result.setLibraryDependencyData(toThriftLibraryDep((LibraryDependencyData) data));
        }
        else if (data instanceof TaskData) {
            result.setTaskData(toThrift((TaskData) data));
        }
        else {
            // Plugin extension data: serialize as bytes
            result.setExtensionData(serializeToBytes(data));
        }

        List<ThriftDataNode> children = new ArrayList<>();
        for (DataNode<?> child : node.getChildren()) {
            children.add(toThrift(child));
        }
        result.setChildren(children);
        return result;
    }

    public static DataNode<ProjectData> fromThrift(ThriftDataNode thrift) {
        return fromThriftNode(thrift, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> DataNode<T> fromThriftNode(ThriftDataNode thrift, DataNode<?> parent) {
        Key key;
        Object data;

        if (thrift.isSetProjectData()) {
            key = ProjectKeys.PROJECT;
            data = fromThrift(thrift.getProjectData());
        }
        else if (thrift.isSetModuleData()) {
            key = ProjectKeys.MODULE;
            data = fromThrift(thrift.getModuleData());
        }
        else if (thrift.isSetLibraryData()) {
            key = ProjectKeys.LIBRARY;
            data = fromThrift(thrift.getLibraryData());
        }
        else if (thrift.isSetContentRootData()) {
            ProjectSystemId owner = deriveOwner(parent);
            key = ProjectKeys.CONTENT_ROOT;
            data = fromThrift(thrift.getContentRootData(), owner);
        }
        else if (thrift.isSetModuleDependencyData()) {
            key = ProjectKeys.MODULE_DEPENDENCY;
            data = fromThriftModuleDep(thrift.getModuleDependencyData());
        }
        else if (thrift.isSetLibraryDependencyData()) {
            key = ProjectKeys.LIBRARY_DEPENDENCY;
            data = fromThriftLibraryDep(thrift.getLibraryDependencyData());
        }
        else if (thrift.isSetTaskData()) {
            key = ProjectKeys.TASK;
            data = fromThrift(thrift.getTaskData());
        }
        else if (thrift.isSetExtensionData()) {
            key = new Key<>(thrift.getKey(), 300);
            data = deserializeFromBytes(thrift.getExtensionData());
        }
        else {
            throw new IllegalStateException("ThriftDataNode has no data set for key: " + thrift.getKey());
        }

        DataNode node = new DataNode(key, data, parent);
        if (thrift.getChildren() != null) {
            for (ThriftDataNode child : thrift.getChildren()) {
                DataNode childNode = fromThriftNode(child, node);
                node.addChild(childNode);
            }
        }
        return node;
    }

    private static ProjectSystemId deriveOwner(DataNode<?> parent) {
        if (parent != null) {
            Object parentData = parent.getData();
            if (parentData instanceof ModuleData) {
                return ((ModuleData) parentData).getOwner();
            }
            if (parentData instanceof ProjectData) {
                return ((ProjectData) parentData).getOwner();
            }
        }
        return ProjectSystemId.IDE;
    }

    // ============ ExecutionSettings ============

    public static ThriftExecutionSettings toThrift(ExternalSystemExecutionSettings settings, Map<String, String> extraProperties) {
        ThriftExecutionSettings result = new ThriftExecutionSettings();
        result.setRemoteProcessIdleTtlMs(settings.getRemoteProcessIdleTtlInMs());
        result.setVerboseProcessing(settings.isVerboseProcessing());
        result.setProperties(extraProperties != null ? extraProperties : Collections.emptyMap());
        return result;
    }

    // ============ Serialization helpers for extension data ============

    private static byte[] serializeToBytes(Object data) {
        try {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            ObjectOutputStream oOut = new ObjectOutputStream(bOut);
            oOut.writeObject(data);
            oOut.close();
            return bOut.toByteArray();
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to serialize extension data: " + data.getClass().getName(), e);
        }
    }

    private static Object deserializeFromBytes(byte[] data) {
        try {
            ObjectInputStream oIn = new ObjectInputStream(new ByteArrayInputStream(data));
            return oIn.readObject();
        }
        catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialize extension data", e);
        }
    }
}
