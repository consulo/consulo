/*
 * Copyright 2013-2022 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License",
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
package consulo.ide.impl.tipOfDay;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.tipOfDay.TipOfDayProvider;

/**
 * @author VISTALL
 * @since 09-Jul-22
 */
@ExtensionImpl
public class DefaultTipOfDayProvider implements TipOfDayProvider {
  @Override
  public String[] getTipFiles() {
    return new String[] {
      "/tips/Welcome.html",
      "/tips/GoToClass.html",

      "/tips/CodeCompletion.html",

      "/tips/FindUsages.html",
      "/tips/QuickJavaDoc.html",

      "/tips/GoToDeclaration.html",

      "/tips/FileStructurePopup.html",

      "/tips/Rename.html",
      "/tips/OverrideImplementMethods.html",

      "/tips/SmartTypeCompletion.html",

      "/tips/TabInLookups.html",

      "/tips/TabInEditorClose.html",
      "/tips/AltInsertInEditor.html",

      "/tips/SelectIn.html",

      "/tips/SpeedSearch.html",

      "/tips/Escape.html",
      "/tips/SurroundWith.html",

      "/tips/SmartTypeAfterNew.html",

      "/tips/GoToImplementation.html",

      "/tips/CtrlW.html",

      "/tips/IntroduceVariable.html",

      "/tips/CommentCode.html",


      "/tips/ExternalJavaDoc.html",

      "/tips/SmartTypeCasting.html",

      "/tips/CtrlD.html",
      "/tips/LiveTemplates.html",
      "/tips/VariableNameCompletion.html",

      "/tips/ParameterInfo.html",
      "/tips/JumpToLastEdit.html",
      "/tips/HighlightUsagesInFile.html",
      "/tips/LayoutCode.html",
      "/tips/LocalVCS.html",
      "/tips/ContextInfo.html",
      "/tips/RecentFiles.html",

      "/tips/NextPrevError.html",
      "/tips/InsertLiveTemplate.html",
      "/tips/MethodSeparators.html",
      "/tips/CodeCompletionMiddle.html",

      "/tips/MethodUpDown.html",
      "/tips/JoinLines.html",
      "/tips/CopyClass.html",

      "/tips/ClipboardStack.html",
      "/tips/HierarchyBrowser.html",
      "/tips/BreakpointSpeedmenu.html",
      "/tips/EvaluateExpressionInEditor.html",
      "/tips/WordCompletion.html",
      "/tips/QuickJavaDocInLookups.html",

      "/tips/DotEtcInLookups.html",

      "/tips/MenuItemsDescriptions.html",
      "/tips/WildcardsInNavigationPopups.html",
      "/tips/MoveInnerToUpper.html",
      "/tips/IntroduceVariableIncompleteCode.html",

      "/tips/GoToSymbol.html",
      "/tips/RecentChanges.html",

      "/tips/ImageFileCompletion.html",
      "/tips/CreatePropertyTag.html",
      "/tips/QuickSwitchScheme.html",

      "/tips/CompleteStatement.html",
      "/tips/CamelPrefixesInNavigationPopups.html",
      "/tips/CtrlShiftI.html",

      "/tips/CompletionInHTML.html",
      "/tips/CopyPasteReference.html",
      "/tips/MoveUpDown.html",
      "/tips/SelectRunDebugConfiguration.html",
      "/tips/CtrlShiftIForLookup.html",

      "/tips/PropertiesCompletion.html",
      "/tips/ShowAppliedStyles.html",
      "/tips/ImagesLookup.html",
      "/tips/RenameCssSelector.html",
      "/tips/NavBar.html",
      "/tips/ChangesView.html",
      "/tips/Antivirus.html",
      "/tips/MavenQuickOpen.html",
      "/tips/MoveToChangelist.html",
      "/tips/EclipseQuickOpen.html",
      "/tips/ShowUsages.html",
      "/tips/GoToAction.html",
      "/tips/GoToInspection.html",
      "/tips/SearchInSettings.html",
      "/tips/CompleteMethod.html",
      "/tips/HighlightImplements.html",
      "/tips/RecentSearch.html",
      "/tips/CodeCompletionInSearch.html",
      "/tips/HighlightMethodExitPoint.html",
      "/tips/HighlightThrows.html",
      "/tips/QuickFixRightArrow.html",
      "/tips/NavigateToFilePath.html",
      "/tips/IssueNavigation.html",
      "/tips/UmlClassDiagram.html",
      "/tips/ColorEditingInCss.html",
      "/tips/CreateTestIntentionAction.html",
      "/tips/ColumnSelection.html",
      "/tips/ColorFiles.html",
      "/tips/CopyWithNoSelection.html",
      "/tips/moveFileToChangelist.html",
      "/tips/Spellchecker.html",
      "/tips/SpellcheckerDictionaries.html",
      "/tips/VcsQuickList.html",
      "/tips/Switcher.html",
      "/tips/DragToOpen.html",
      "/tips/CloseOthers.html",
      "/tips/EnterDirectoryInGotoFile.html",
      "/tips/GotoLineInFile.html",
      "/tips/AnnotationsAndDiffs.html",
      "/tips/DirDiff.html",
      "/tips/JarDiff.html",
      "/tips/ShowHideSideBars.html",
      "/tips/ExcludeFromProject.html",
      "/tips/CodeCompletionNoShift.html",
      "/tips/CommitCtrlK.html",
      "/tips/FindReplaceToggle.html",
      "/tips/ScopesInTODO.html",
      "/tips/PreviewTODO.html",
      "/tips/FixDocComment.html",
      "/tips/SelectTasks.html",
      "/tips/RunConfigFolders.html",
      "/tips/SpeedSearchinLiveTemplates.html",
      "/tips/EditRegExp.html",

      "/tips/Emmet.html",
      "/tips/EscapeCharactersInResourceBundle.html",
      "/tips/LineEndings.html",
      "/tips/LineEndingsFolder.html",
      "/tips/RefactorThis.html",
      "/tips/FavoritesToolWindow1.html",
      "/tips/FavoritesToolWindow2.html"
    };
  }
}
