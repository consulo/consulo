/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util;

import consulo.annotation.DeprecationInfo;
import consulo.platform.base.icon.PlatformIconGroup;

import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
@Deprecated
@DeprecationInfo(value = "Use PlatformIconGroup")
public interface PlatformIcons {
    Icon PUBLIC_ICON = (Icon) PlatformIconGroup.nodesC_public();
    Icon LOCKED_ICON = (Icon) PlatformIconGroup.nodesLocked();
    Icon SYMLINK_ICON = (Icon) PlatformIconGroup.nodesSymlink();
    Icon PRIVATE_ICON = (Icon) PlatformIconGroup.nodesC_private();
    Icon PROTECTED_ICON = (Icon) PlatformIconGroup.nodesC_protected();
    Icon PACKAGE_LOCAL_ICON = (Icon) PlatformIconGroup.nodesC_plocal();
    Icon PACKAGE_ICON = (Icon) PlatformIconGroup.nodesPackage();

    Icon DIRECTORY_CLOSED_ICON = (Icon) PlatformIconGroup.nodesTreeclosed();
    @Deprecated
    Icon DIRECTORY_OPEN_ICON = (Icon) PlatformIconGroup.nodesTreeclosed();

    Icon CLASS_ICON = (Icon) PlatformIconGroup.nodesClass();
    Icon EXCEPTION_CLASS_ICON = (Icon) PlatformIconGroup.nodesExceptionclass();
    Icon ANONYMOUS_CLASS_ICON = (Icon) PlatformIconGroup.nodesAnonymousclass();
    Icon ABSTRACT_CLASS_ICON = (Icon) PlatformIconGroup.nodesAbstractclass();
    Icon ANNOTATION_TYPE_ICON = (Icon) PlatformIconGroup.nodesAnnotationtype();
    Icon ENUM_ICON = (Icon) PlatformIconGroup.nodesEnum();
    Icon INTERFACE_ICON = (Icon) PlatformIconGroup.nodesInterface();
    Icon METHOD_ICON = (Icon) PlatformIconGroup.nodesMethod();
    Icon FUNCTION_ICON = (Icon) PlatformIconGroup.nodesFunction();
    Icon ABSTRACT_METHOD_ICON = (Icon) PlatformIconGroup.nodesAbstractmethod();
    Icon FIELD_ICON = (Icon) PlatformIconGroup.nodesField();
    Icon PARAMETER_ICON = (Icon) PlatformIconGroup.nodesParameter();
    Icon VARIABLE_ICON = (Icon) PlatformIconGroup.nodesVariable();
    Icon XML_TAG_ICON = (Icon) PlatformIconGroup.nodesTag();
    Icon LIBRARY_ICON = (Icon) PlatformIconGroup.nodesPplib();
    Icon WEB_ICON = (Icon) PlatformIconGroup.nodesPpweb();
    Icon JAR_ICON = (Icon) PlatformIconGroup.filetypesArchive();
    Icon FILE_ICON = (Icon) PlatformIconGroup.nodesFolder();

    Icon VARIABLE_READ_ACCESS = (Icon) PlatformIconGroup.nodesRead_access();
    Icon VARIABLE_WRITE_ACCESS = (Icon) PlatformIconGroup.nodesWrite_access();
    Icon VARIABLE_RW_ACCESS = (Icon) PlatformIconGroup.nodesRw_access();
    Icon CUSTOM_FILE_ICON = (Icon) PlatformIconGroup.filetypesCustom();
    Icon PROPERTY_ICON = (Icon) PlatformIconGroup.nodesProperty();
    Icon ERROR_INTRODUCTION_ICON = (Icon) PlatformIconGroup.generalError();
    Icon WARNING_INTRODUCTION_ICON = (Icon) PlatformIconGroup.generalWarning();
    Icon EXCLUDED_FROM_COMPILE_ICON = (Icon) PlatformIconGroup.nodesExcludedfromcompile();
    Icon PROJECT_ICON = (Icon) PlatformIconGroup.icon16();
    Icon UI_FORM_ICON = (Icon) PlatformIconGroup.filetypesUiform();

    Icon GROUP_BY_PACKAGES = (Icon) PlatformIconGroup.actionsGroupbypackage();
    Icon ADD_ICON = (Icon) PlatformIconGroup.generalAdd();
    Icon DELETE_ICON = (Icon) PlatformIconGroup.generalRemove();
    Icon COPY_ICON = (Icon) PlatformIconGroup.actionsCopy();
    Icon EDIT = (Icon) PlatformIconGroup.actionsEdit();
    Icon SELECT_ALL_ICON = (Icon) PlatformIconGroup.actionsSelectall();
    Icon UNSELECT_ALL_ICON = (Icon) PlatformIconGroup.actionsUnselectall();
    Icon SYNCHRONIZE_ICON = (Icon) PlatformIconGroup.actionsRefresh();
    Icon SHOW_SETTINGS_ICON = (Icon) PlatformIconGroup.generalSettings();

    Icon CHECK_ICON = (Icon) PlatformIconGroup.actionsChecked();
    Icon CHECK_ICON_SELECTED = (Icon) PlatformIconGroup.actionsChecked_selected();
    Icon CHECK_ICON_SMALL = (Icon) PlatformIconGroup.actionsChecked();
    Icon CHECK_ICON_SMALL_SELECTED = (Icon) PlatformIconGroup.actionsChecked_selected();

    Icon OPEN_EDIT_DIALOG_ICON = (Icon) PlatformIconGroup.actionsShow();
    Icon FLATTEN_PACKAGES_ICON = (Icon) PlatformIconGroup.objectbrowserFlattenpackages();

    Icon CLASS_INITIALIZER = (Icon) PlatformIconGroup.nodesClassinitializer();

    @Deprecated
    Icon CLOSED_MODULE_GROUP_ICON = (Icon) PlatformIconGroup.nodesModulegroup();
    @Deprecated
    Icon OPENED_MODULE_GROUP_ICON = CLOSED_MODULE_GROUP_ICON;

    Icon FOLDER_ICON = (Icon) PlatformIconGroup.nodesFolder();

    Icon INVALID_ENTRY_ICON = (Icon) PlatformIconGroup.nodesPpinvalid();

    Icon MODULES_SOURCE_FOLDERS_ICON = (Icon) PlatformIconGroup.modulesSourceroot();
    Icon MODULES_TEST_SOURCE_FOLDER = (Icon) PlatformIconGroup.modulesTestroot();

    Icon CONTENT_ROOT_ICON_CLOSED = (Icon) PlatformIconGroup.nodesModule();
    @Deprecated
    Icon CONTENT_ROOT_ICON_OPEN = CONTENT_ROOT_ICON_CLOSED;

    Icon UP_DOWN_ARROWS = (Icon) PlatformIconGroup.ideUpdown();
}
