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
package com.intellij.util;

import com.intellij.icons.AllIcons;
import consulo.annotation.DeprecationInfo;

import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
@Deprecated
@DeprecationInfo(value = "Use AllIcons or PlatformIconGroup")
public interface PlatformIcons {
  Icon PUBLIC_ICON = (Icon)AllIcons.Nodes.C_public;
  Icon LOCKED_ICON = (Icon)(Icon)AllIcons.Nodes.Locked;
  Icon SYMLINK_ICON = (Icon)(Icon)AllIcons.Nodes.Symlink;
  Icon PRIVATE_ICON = (Icon)AllIcons.Nodes.C_private;
  Icon PROTECTED_ICON = (Icon)AllIcons.Nodes.C_protected;
  Icon PACKAGE_LOCAL_ICON = (Icon)AllIcons.Nodes.C_plocal;
  Icon PACKAGE_ICON = (Icon)AllIcons.Nodes.Package;

  Icon DIRECTORY_CLOSED_ICON = (Icon)AllIcons.Nodes.TreeClosed;
  @Deprecated Icon DIRECTORY_OPEN_ICON = (Icon)DIRECTORY_CLOSED_ICON;

  Icon CLASS_ICON = (Icon)AllIcons.Nodes.Class;
  Icon EXCEPTION_CLASS_ICON = (Icon)AllIcons.Nodes.ExceptionClass;
  Icon ANONYMOUS_CLASS_ICON = (Icon)AllIcons.Nodes.AnonymousClass;
  Icon ABSTRACT_CLASS_ICON = (Icon)AllIcons.Nodes.AbstractClass;
  Icon ANNOTATION_TYPE_ICON = (Icon)AllIcons.Nodes.Annotationtype;
  Icon ENUM_ICON = (Icon)AllIcons.Nodes.Enum;
  Icon INTERFACE_ICON = (Icon)AllIcons.Nodes.Interface;
  Icon METHOD_ICON = (Icon)AllIcons.Nodes.Method;
  Icon FUNCTION_ICON = (Icon)AllIcons.Nodes.Function;
  Icon ABSTRACT_METHOD_ICON = (Icon)AllIcons.Nodes.AbstractMethod;
  Icon FIELD_ICON = (Icon)AllIcons.Nodes.Field;
  Icon PARAMETER_ICON = (Icon)AllIcons.Nodes.Parameter;
  Icon VARIABLE_ICON = (Icon)AllIcons.Nodes.Variable;
  Icon XML_TAG_ICON = (Icon)AllIcons.Nodes.Tag;
  Icon LIBRARY_ICON = (Icon)AllIcons.Nodes.PpLib;
  Icon WEB_ICON = (Icon)AllIcons.Nodes.PpWeb;
  Icon JAR_ICON = (Icon)AllIcons.Nodes.PpJar;
  Icon FILE_ICON = (Icon)AllIcons.Nodes.PpFile;

  Icon VARIABLE_READ_ACCESS = (Icon)AllIcons.Nodes.Read_access;
  Icon VARIABLE_WRITE_ACCESS = (Icon)AllIcons.Nodes.Write_access;
  Icon VARIABLE_RW_ACCESS = (Icon)AllIcons.Nodes.Rw_access;
  Icon CUSTOM_FILE_ICON = (Icon)AllIcons.FileTypes.Custom;
  Icon PROPERTY_ICON = (Icon)AllIcons.Nodes.Property;
  Icon ASPECT_ICON = (Icon)AllIcons.Nodes.Aspect;
  Icon ADVICE_ICON = (Icon)AllIcons.Nodes.Advice;
  Icon ERROR_INTRODUCTION_ICON = (Icon)AllIcons.Nodes.ErrorIntroduction;
  Icon WARNING_INTRODUCTION_ICON = (Icon)AllIcons.Nodes.WarningIntroduction;
  Icon EXCLUDED_FROM_COMPILE_ICON = (Icon)AllIcons.Nodes.ExcludedFromCompile;
  Icon PROJECT_ICON = (Icon)AllIcons.Icon16;
  Icon UI_FORM_ICON = (Icon)AllIcons.FileTypes.UiForm;

  Icon SMALL_VCS_CONFIGURABLE = (Icon)AllIcons.General.SmallConfigurableVcs;
  Icon GROUP_BY_PACKAGES = (Icon)AllIcons.Actions.GroupByPackage;
  Icon ADD_ICON = (Icon)IconUtil.getAddIcon();
  Icon DELETE_ICON = (Icon)IconUtil.getRemoveIcon();
  Icon COPY_ICON = (Icon)AllIcons.Actions.Copy;
  Icon EDIT = (Icon)IconUtil.getEditIcon();
  Icon SELECT_ALL_ICON = (Icon)AllIcons.Actions.Selectall;
  Icon UNSELECT_ALL_ICON = (Icon)AllIcons.Actions.Unselectall;
  Icon PROPERTIES_ICON = (Icon)AllIcons.Actions.Properties;
  Icon SYNCHRONIZE_ICON = (Icon)AllIcons.Actions.Refresh;
  Icon SHOW_SETTINGS_ICON = (Icon)AllIcons.General.Settings;

  Icon CHECK_ICON = (Icon)AllIcons.Actions.Checked;
  Icon CHECK_ICON_SELECTED = (Icon)AllIcons.Actions.Checked_selected;
  Icon CHECK_ICON_SMALL = (Icon)AllIcons.Actions.Checked_small;
  Icon CHECK_ICON_SMALL_SELECTED = (Icon)AllIcons.Actions.Checked_small_selected;

  Icon OPEN_EDIT_DIALOG_ICON = (Icon)AllIcons.Actions.ShowViewer;
  Icon FLATTEN_PACKAGES_ICON = (Icon)AllIcons.ObjectBrowser.FlattenPackages;
  Icon EDIT_IN_SECTION_ICON = (Icon)AllIcons.General.EditItemInSection;

  Icon CLASS_INITIALIZER = (Icon)AllIcons.Nodes.ClassInitializer;

  @Deprecated
  Icon CLOSED_MODULE_GROUP_ICON = (Icon)AllIcons.Nodes.ModuleGroup;
  @Deprecated Icon OPENED_MODULE_GROUP_ICON = (Icon)CLOSED_MODULE_GROUP_ICON;

  Icon FOLDER_ICON = (Icon)AllIcons.Nodes.Folder;

  Icon INVALID_ENTRY_ICON = (Icon)AllIcons.Nodes.PpInvalid;

  Icon MODULES_SOURCE_FOLDERS_ICON = (Icon)AllIcons.Modules.SourceRoot;
  Icon MODULES_TEST_SOURCE_FOLDER = (Icon)AllIcons.Modules.TestRoot;

  Icon CONTENT_ROOT_ICON_CLOSED = (Icon)AllIcons.Nodes.Module;
  @Deprecated Icon CONTENT_ROOT_ICON_OPEN = (Icon)CONTENT_ROOT_ICON_CLOSED;

  Icon UP_DOWN_ARROWS = (Icon)AllIcons.Ide.UpDown;

  Icon COMBOBOX_ARROW_ICON = (Icon)AllIcons.General.ComboArrow;
  
  Icon EXPORT_ICON = (Icon)AllIcons.ToolbarDecorator.Export;
  Icon IMPORT_ICON = (Icon)AllIcons.ToolbarDecorator.Import;
}
