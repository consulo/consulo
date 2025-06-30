/*
 * Copyright 2000_2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License();
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http:/Www.apache.orgLicensesLICENSE_2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.application;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

/**
 * Better use PlatformIconGroup
 */
@Deprecated
public class AllIcons {

    public static class Actions {
        public static final Image AddMulticaret = PlatformIconGroup.actionsAddmulticaret(); // 16x16
        public static final Image AllLeft = PlatformIconGroup.actionsAllleft(); // 16x16
        public static final Image AllRight = PlatformIconGroup.actionsAllright(); // 16x16
        public static final Image Annotate = PlatformIconGroup.actionsAnnotate(); // 16x16
        public static final Image Back = PlatformIconGroup.actionsBack(); // 16x16
        public static final Image Browser_externalJavaDoc = PlatformIconGroup.actionsBrowser_externaljavadoc(); // 16x16
        public static final Image Cancel = PlatformIconGroup.actionsCancel(); // 16x16
        public static final Image Checked = PlatformIconGroup.actionsChecked(); // 12x12
        public static final Image Checked_selected = PlatformIconGroup.actionsChecked_selected();
        public static final Image CheckMulticaret = PlatformIconGroup.actionsCheckmulticaret(); // 16x16
        public static final Image CheckOut = PlatformIconGroup.actionsCheckout(); // 16x16
        public static final Image Close = PlatformIconGroup.actionsClose(); // 16x16
        public static final Image Collapseall = PlatformIconGroup.actionsCollapseall(); // 16x16
        public static final Image Commit = PlatformIconGroup.actionsCommit(); // 16x16
        public static final Image Compile = PlatformIconGroup.actionsCompile(); // 16x16
        public static final Image Copy = PlatformIconGroup.actionsCopy(); // 16x16
        public static final Image Diff = PlatformIconGroup.actionsDiff(); // 16x16
        public static final Image DiffWithCurrent = PlatformIconGroup.actionsDiffwithcurrent(); // 16x16
        public static final Image Download = PlatformIconGroup.actionsDownload(); // 16x16
        public static final Image Dump = PlatformIconGroup.actionsDump(); // 16x16
        public static final Image Edit = PlatformIconGroup.actionsEdit(); // 14x14
        public static final Image EditSource = PlatformIconGroup.actionsEditsource(); // 16x16
        public static final Image Exclude = General.Remove; // 14x14
        public static final Image Execute = PlatformIconGroup.actionsExecute(); // 16x16
        public static final Image Exit = PlatformIconGroup.actionsExit(); // 16x16
        public static final Image Expandall = PlatformIconGroup.actionsExpandall(); // 16x16
        public static final Image Export = PlatformIconGroup.actionsExport(); // 16x16
        public static final Image FindAndShowNextMatchesSmall = PlatformIconGroup.actionsFindandshownextmatchessmall();
        public static final Image FindAndShowPrevMatchesSmall = PlatformIconGroup.actionsFindandshowprevmatchessmall();
        public static final Image Find = PlatformIconGroup.actionsFind(); // 16x16
        public static final Image FindPlain = Find; // 16x16
        public static final Image Forward = PlatformIconGroup.actionsForward(); // 16x16
        public static final Image GC = PlatformIconGroup.actionsGc(); // 16x16
        public static final Image GroupByClass = PlatformIconGroup.actionsGroupbyclass(); // 16x16
        public static final Image GroupByFile = PlatformIconGroup.actionsGroupbyfile(); // 16x16
        public static final Image GroupByMethod = PlatformIconGroup.actionsGroupbymethod(); // 16x16
        public static final Image GroupByModule = PlatformIconGroup.actionsGroupbymodule(); // 16x16
        public static final Image GroupByModuleGroup = PlatformIconGroup.actionsGroupbymodulegroup(); // 16x16
        public static final Image GroupBy = PlatformIconGroup.actionsGroupby(); // 16x16
        public static final Image GroupByPackage = PlatformIconGroup.actionsGroupbypackage(); // 16x16
        public static final Image GroupByPrefix = PlatformIconGroup.actionsGroupbyprefix(); // 16x16
        public static final Image GroupByTestProduction = PlatformIconGroup.actionsGroupbytestproduction(); // 16x16
        public static final Image Help = PlatformIconGroup.actionsHelp(); // 16x16
        public static final Image Install = PlatformIconGroup.actionsInstall(); // 16x16
        public static final Image IntentionBulb = PlatformIconGroup.actionsIntentionbulb(); // 16x16
        public static final Image LoginAvator = PlatformIconGroup.actionsLoginavatar(); // 16x16
        public static final Image Lightning = PlatformIconGroup.actionsLightning(); // 16x16
        public static final Image Menu_cut = PlatformIconGroup.actionsMenu_cut(); // 16x16
        public static final Image Menu_help = PlatformIconGroup.actionsMenu_help(); // 16x16
        public static final Image Menu_open = PlatformIconGroup.actionsMenu_open(); // 16x16
        public static final Image Menu_paste = PlatformIconGroup.actionsMenu_paste(); // 16x16
        public static final Image Menu_saveall = PlatformIconGroup.actionsMenu_saveall(); // 16x16
        public static final Image More = PlatformIconGroup.actionsMorevertical();
        public static final Image MoveDown = PlatformIconGroup.actionsMovedown(); // 14x14
        public static final Image MoveToAnotherChangelist = PlatformIconGroup.actionsMovetoanotherchangelist(); // 16x16
        public static final Image MoveUp = PlatformIconGroup.actionsMoveup(); // 14x14
        public static final Image New = PlatformIconGroup.actionsNew(); // 16x16
        public static final Image NewFolder = PlatformIconGroup.actionsNewfolder(); // 16x16
        public static final Image NextOccurence = PlatformIconGroup.actionsNextoccurence(); // 16x16
        public static final Image Pause = PlatformIconGroup.actionsPause(); // 16x16
        public static final Image Preview = PlatformIconGroup.actionsPreview(); // 16x16
        public static final Image PreviewDetails = PlatformIconGroup.actionsPreviewdetails(); // 16x16
        public static final Image PreviousOccurence = PlatformIconGroup.actionsPreviousoccurence(); // 14x14
        public static final Image ProfileCPU = PlatformIconGroup.actionsProfilecpu(); // 16x16
        public static final Image ProfileMemory = PlatformIconGroup.actionsProfilememory(); // 16x16
        public static final Image QuickfixBulb = PlatformIconGroup.actionsQuickfixbulb(); // 16x16
        public static final Image QuickfixOffBulb = PlatformIconGroup.actionsQuickfixoffbulb(); // 16x16
        public static final Image QuickList = PlatformIconGroup.actionsQuicklist(); // 16x16
        public static final Image Redo = PlatformIconGroup.actionsRedo(); // 16x16
        public static final Image RefactoringBulb = PlatformIconGroup.actionsRefactoringbulb(); // 16x16
        public static final Image Refresh = PlatformIconGroup.actionsRefresh(); // 16x16
        public static final Image RemoveMulticaret = PlatformIconGroup.actionsRemovemulticaret(); // 16x16
        public static final Image Replace = PlatformIconGroup.actionsReplace(); // 16x16
        public static final Image Rerun = PlatformIconGroup.actionsRerun(); // 16x16
        public static final Image Restart = PlatformIconGroup.actionsRestart(); // 16x16
        public static final Image Resume = PlatformIconGroup.actionsResume(); // 16x16
        public static final Image Rollback = PlatformIconGroup.actionsRollback(); // 16x16
        public static final Image Run_anything = PlatformIconGroup.actionsRun_anything();
        public static final Image Scratch = PlatformIconGroup.actionsScratch(); // 16x16
        public static final Image Search = PlatformIconGroup.actionsSearch(); // 16x16
        public static final Image SearchWithHistory = PlatformIconGroup.actionsSearchwithhistory(); // 16x16
        public static final Image SearchNewLine = PlatformIconGroup.actionsSearchnewline(); // 16x16
        public static final Image SearchNewLineHover = PlatformIconGroup.actionsSearchnewlinehover(); // 16x16
        public static final Image Selectall = PlatformIconGroup.actionsSelectall(); // 16x16
        public static final Image ShortcutFilter = PlatformIconGroup.actionsShortcutfilter(); // 16x16
        public static final Image ShowAsTree = PlatformIconGroup.actionsShowastree(); // 16x16
        public static final Image ShowImportStatements = PlatformIconGroup.actionsShowimportstatements(); // 16x16
        public static final Image ShowReadAccess = PlatformIconGroup.actionsShowreadaccess(); // 16x16
        @Deprecated
        public static final Image ShowViewer = PlatformIconGroup.actionsShow();
        public static final Image ShowWriteAccess = PlatformIconGroup.actionsShowwriteaccess(); // 16x16
        public static final Image SplitHorizontally = PlatformIconGroup.actionsSplithorizontally(); // 16x16
        public static final Image SplitVertically = PlatformIconGroup.actionsSplitvertically(); // 16x16
        public static final Image StepOut = PlatformIconGroup.actionsStepout(); // 16x16
        public static final Image Suspend = PlatformIconGroup.actionsSuspend(); // 16x16
        public static final Image SwapPanels = PlatformIconGroup.actionsSwappanels(); // 16x16
        public static final Image SynchronizeScrolling = PlatformIconGroup.actionsSynchronizescrolling(); // 16x16
        public static final Image SyncPanels = PlatformIconGroup.actionsSyncpanels(); // 16x16
        public static final Image ToggleSoftWrap = PlatformIconGroup.actionsTogglesoftwrap(); // 16x16
        public static final Image TraceInto = PlatformIconGroup.actionsTraceinto(); // 16x16
        public static final Image TraceOver = PlatformIconGroup.actionsTraceover(); // 16x16
        public static final Image Undo = PlatformIconGroup.actionsUndo(); // 16x16
        public static final Image Uninstall = PlatformIconGroup.actionsUninstall(); // 16x16
        public static final Image Unselectall = PlatformIconGroup.actionsUnselectall(); // 16x16

        /**
         * 16x16
         */
        public static final Image Words = PlatformIconGroup.actionsWords();
        /**
         * 16x16
         */
        public static final Image WordsHovered = PlatformIconGroup.actionsWordshovered();
        /**
         * 16x16
         */
        public static final Image WordsSelected = PlatformIconGroup.actionsWordsselected();

        /**
         * 16x16
         */
        public static final Image Regex = PlatformIconGroup.actionsRegex();
        /**
         * 16x16
         */
        public static final Image RegexHovered = PlatformIconGroup.actionsRegexhovered();
        /**
         * 16x16
         */
        public static final Image RegexSelected = PlatformIconGroup.actionsRegexselected();

        /**
         * 16x16
         */
        public static final Image MatchCase = PlatformIconGroup.actionsMatchcase();
        /**
         * 16x16
         */
        public static final Image MatchCaseHovered = PlatformIconGroup.actionsMatchcasehovered();
        /**
         * 16x16
         */
        public static final Image MatchCaseSelected = PlatformIconGroup.actionsMatchcaseselected();

        /**
         * 16x16
         */
        public static final Image PreserveCase = PlatformIconGroup.actionsPreservecase();
        /**
         * 16x16
         */
        public static final Image PreserveCaseHover = PlatformIconGroup.actionsPreservecasehover();
        /**
         * 16x16
         */
        public static final Image PreserveCaseSelected = PlatformIconGroup.actionsPreservecaseselected();
    }

    public static class Diff {
        public static final Image ApplyNotConflicts = PlatformIconGroup.diffApplynotconflicts(); // 16x16
        public static final Image ApplyNotConflictsLeft = PlatformIconGroup.diffApplynotconflictsleft(); // 16x16
        public static final Image ApplyNotConflictsRight = PlatformIconGroup.diffApplynotconflictsright(); // 16x16
        public static final Image Arrow = PlatformIconGroup.diffArrow(); // 11x11
        public static final Image ArrowLeftDown = PlatformIconGroup.diffArrowleftdown(); // 11x11
        public static final Image ArrowRight = PlatformIconGroup.diffArrowright(); // 11x11
        public static final Image ArrowRightDown = PlatformIconGroup.diffArrowright(); // 11x11
        public static final Image BranchDiff = PlatformIconGroup.diffBranchdiff(); // 16x16
        public static final Image LeftDiff = PlatformIconGroup.diffLeftdiff(); // 16x16
        public static final Image MagicResolve = PlatformIconGroup.diffMagicresolve(); // 12x12
        public static final Image Remove = PlatformIconGroup.diffRemove(); // 11x11
        public static final Image RightDiff = PlatformIconGroup.diffRightdiff(); // 16x16

    }

    public static class Duplicates {
        public static final Image SendToTheLeft = PlatformIconGroup.duplicatesSendtotheleft(); // 16x16
        public static final Image SendToTheLeftGrayed = PlatformIconGroup.duplicatesSendtotheleftgrayed(); // 16x16
        public static final Image SendToTheRight = PlatformIconGroup.duplicatesSendtotheright(); // 16x16
        public static final Image SendToTheRightGrayed = PlatformIconGroup.duplicatesSendtotherightgrayed(); // 16x16

    }

    public static class FileTypes {
        public static final Image Any_type = PlatformIconGroup.filetypesAny_type(); // 16x16
        public static final Image Archive = PlatformIconGroup.filetypesArchive(); // 16x16
        public static final Image Aspectj = PlatformIconGroup.filetypesAspectj(); // 16x16
        public static final Image Config = PlatformIconGroup.filetypesConfig(); // 16x16
        public static final Image Custom = PlatformIconGroup.filetypesCustom(); // 16x16
        public static final Image Dtd = PlatformIconGroup.filetypesDtd(); // 16x16
        public static final Image Htaccess = PlatformIconGroup.filetypesHtaccess(); // 16x16
        public static final Image Html = PlatformIconGroup.filetypesHtml(); // 16x16
        public static final Image Idl = PlatformIconGroup.filetypesIdl(); // 16x16
        public static final Image Properties = PlatformIconGroup.filetypesProperties(); // 16x16
        public static final Image Text = PlatformIconGroup.filetypesText(); // 16x16
        public static final Image UiForm = PlatformIconGroup.filetypesUiform(); // 16x16
        public static final Image Unknown = PlatformIconGroup.filetypesUnknown(); // 16x16
        public static final Image WsdlFile = PlatformIconGroup.filetypesWsdlfile(); // 16x16
        public static final Image Xhtml = PlatformIconGroup.filetypesXhtml(); // 16x16
        public static final Image Xml = PlatformIconGroup.filetypesXml(); // 16x16
        public static final Image XsdFile = PlatformIconGroup.filetypesXsdfile(); // 16x16

    }

    public static class General {
        public static final Image LayoutEditorOnly = PlatformIconGroup.generalLayouteditoronly(); // 16x16
        public static final Image LayoutEditorPreview = PlatformIconGroup.generalLayouteditorpreview(); // 16x16
        public static final Image LayoutPreviewOnly = PlatformIconGroup.generalLayoutpreviewonly(); // 16x16
        public static final Image Add = PlatformIconGroup.generalAdd(); // 16x16
        public static final Image ArrowDown = PlatformIconGroup.generalArrowdown(); // 7x6
        public static final Image AutoscrollFromSource = PlatformIconGroup.generalAutoscrollfromsource(); // 16x16
        public static final Image AutoscrollToSource = PlatformIconGroup.generalAutoscrolltosource(); // 16x16
        public static final Image Balloon = PlatformIconGroup.generalBalloon(); // 16x16
        public static final Image BalloonError = PlatformIconGroup.generalBalloonerror(); // 16x16
        public static final Image BalloonInformation = PlatformIconGroup.generalBallooninformation(); // 16x16
        public static final Image BalloonWarning = PlatformIconGroup.generalBalloonwarning(); // 16x16
        public static final Image Bullet = PlatformIconGroup.generalBullet(); // 16x16
        public static final Image CollapseComponent = PlatformIconGroup.generalCollapsecomponent();
        public static final Image CollapseComponentHover = PlatformIconGroup.generalCollapsecomponenthover();
        public static final Image CollapseAll = PlatformIconGroup.actionsCollapseall(); // 11x16
        public static final Image ContextHelp = PlatformIconGroup.generalContexthelp();
        public static final Image Dropdown = PlatformIconGroup.generalDropdown(); // 16x16
        public static final Image Ellipsis = PlatformIconGroup.generalEllipsis(); // 9x9
        public static final Image Error = PlatformIconGroup.generalError(); // 16x16
        public static final Image ErrorDialog = PlatformIconGroup.generalErrordialog(); // 32x32
        public static final Image ExclMark = PlatformIconGroup.generalExclmark(); // 16x16
        public static final Image ExpandComponent = PlatformIconGroup.generalExpandcomponent();
        public static final Image ExpandComponentHover = PlatformIconGroup.generalExpandcomponenthover();
        public static final Image ExpandAll = PlatformIconGroup.actionsExpandall(); // 11x16
        public static final Image Filter = PlatformIconGroup.generalFilter(); // 16x16
        public static final Image GearPlain = PlatformIconGroup.generalGearplain(); // 16x16
        public static final Image HideToolWindow = PlatformIconGroup.generalHidetoolwindow(); // 16x16
        public static final Image ImplementingMethod = PlatformIconGroup.gutterImplementingmethod(); // 10x14
        public static final Image Information = PlatformIconGroup.generalInformation(); // 16x16
        public static final Image InformationDialog = PlatformIconGroup.generalInformationdialog(); // 32x32
        @Deprecated
        public static final Image InheritedMethod = PlatformIconGroup.hierarchySubtypes(); // 11x14
        public static final Image InspectionsError = PlatformIconGroup.generalInspectionserror(); // 14x14
        public static final Image InspectionsEye = PlatformIconGroup.generalInspectionseye(); // 14x14
        public static final Image InspectionsOK = PlatformIconGroup.generalInspectionsok(); // 14x14
        public static final Image InspectionsPause = PlatformIconGroup.generalInspectionspause(); // 14x14
        public static final Image InspectionsTrafficOff = PlatformIconGroup.generalInspectionstrafficoff(); // 14x14
        public static final Image InspectionsPowerSaveMode = PlatformIconGroup.generalInspectionspowersavemode(); // 14x14
        public static final Image InspectionsTypos = PlatformIconGroup.generalInspectionstypos(); // 14x14
        public static final Image Jdk = PlatformIconGroup.generalJdk(); // 16x16
        public static final Image Youtube = PlatformIconGroup.generalYoutube(); // 32x32
        public static final Image Locate = PlatformIconGroup.generalLocate(); // 14x16
        public static final Image Modified = PlatformIconGroup.generalModified(); // 24x16
        public static final Image MoreTabs = PlatformIconGroup.generalMoretabs(); // 16x16
        public static final Image Mouse = PlatformIconGroup.generalMouse(); // 32x32
        public static final Image NotificationError = PlatformIconGroup.generalNotificationerror(); // 24x24
        public static final Image NotificationInfo = PlatformIconGroup.generalNotificationinfo(); // 24x24
        public static final Image NotificationWarning = PlatformIconGroup.generalNotificationwarning(); // 24x24
        @Deprecated
        public static final Image OverridenMethod = PlatformIconGroup.gutterOverridenmethod(); // 10x14
        @Deprecated
        public static final Image OverridingMethod = PlatformIconGroup.gutterOverridingmethod(); // 10x14
        public static final Image Pin_tab = PlatformIconGroup.generalPin_tab(); // 16x16
        public static final Image ProjectConfigurable = PlatformIconGroup.generalProjectconfigurable(); // 9x9
        public static final Image ProjectStructure = PlatformIconGroup.generalProjectstructure(); // 16x16
        public static final Image ProjectTab = PlatformIconGroup.generalProjecttab(); // 16x16
        public static final Image QuestionDialog = PlatformIconGroup.generalQuestiondialog(); // 32x32
        public static final Image Remove = PlatformIconGroup.generalRemove(); // 16x16
        public static final Image Reset = PlatformIconGroup.generalReset(); // 16x16
        public static final Image SeparatorH = PlatformIconGroup.generalSeparatorh(); // 17x11
        public static final Image Settings = PlatformIconGroup.generalSettings(); // 16x16
        public static final Image Show_to_implement = PlatformIconGroup.generalShowtoimplement(); // 16x16
        public static final Image TbHidden = PlatformIconGroup.generalTbhidden(); // 16x16
        public static final Image TbShown = PlatformIconGroup.generalTbshown(); // 16x16
        public static final Image TodoDefault = PlatformIconGroup.generalTododefault(); // 12x12
        public static final Image TodoImportant = PlatformIconGroup.generalTodoimportant(); // 12x12
        public static final Image TodoQuestion = PlatformIconGroup.generalTodoquestion(); // 12x12
        public static final Image Warning = PlatformIconGroup.generalWarning(); // 16x16
        public static final Image WarningDecorator = PlatformIconGroup.generalWarningdecorator(); // 16x16
        public static final Image WarningDialog = PlatformIconGroup.generalWarningdialog(); // 32x32

    }

    public static class Graph {
        public static final Image ActualZoom = PlatformIconGroup.graphActualzoom(); // 16x16
        public static final Image FitContent = PlatformIconGroup.graphFitcontent(); // 16x16
        public static final Image Grid = PlatformIconGroup.graphGrid(); // 16x16
        public static final Image Layout = PlatformIconGroup.graphLayout(); // 16x16
        public static final Image NodeSelectionMode = PlatformIconGroup.generalPrint(); // 16x16
        public static final Image PrintPreview = PlatformIconGroup.graphPrintpreview(); // 16x16
        public static final Image SnapToGrid = PlatformIconGroup.graphSnaptogrid(); // 16x16
        public static final Image ZoomIn = PlatformIconGroup.graphZoomin(); // 16x16
        public static final Image ZoomOut = PlatformIconGroup.graphZoomout(); // 16x16

    }

    public static class Gutter {
        public static final Image Colors = PlatformIconGroup.gutterColors(); // 12x12
        public static final Image ImplementedMethod = PlatformIconGroup.gutterImplementedmethod(); // 12x12
        public static final Image ImplementingFunctional = PlatformIconGroup.gutterImplementingfunctionalinterface(); // 12x12
        public static final Image ImplementingMethod = PlatformIconGroup.gutterImplementingmethod(); // 12x12
        public static final Image OverridenMethod = PlatformIconGroup.gutterOverridenmethod(); // 12x12
        public static final Image OverridingMethod = PlatformIconGroup.gutterOverridingmethod(); // 12x12
        public static final Image RecursiveMethod = PlatformIconGroup.gutterRecursivemethod(); // 12x12
        public static final Image SiblingInheritedMethod = PlatformIconGroup.gutterSiblinginheritedmethod(); // 12x12
        public static final Image Unique = PlatformIconGroup.gutterUnique(); // 8x8
        public static final Image ReadAccess = PlatformIconGroup.gutterReadaccess(); // 12x12
        public static final Image WriteAccess = PlatformIconGroup.gutterWriteaccess(); // 12x12
    }

    public static class Hierarchy {
        public static final Image Base = PlatformIconGroup.hierarchyBase(); // 16x16
        @Deprecated
        public static final Image Callee = PlatformIconGroup.hierarchySubtypes(); // 16x16
        @Deprecated
        public static final Image Caller = PlatformIconGroup.hierarchySupertypes(); // 16x16
        public static final Image Class = PlatformIconGroup.hierarchyClasshierarchy(); // 16x16
        public static final Image MethodDefined = PlatformIconGroup.hierarchyMethoddefined(); // 9x9
        public static final Image MethodNotDefined = PlatformIconGroup.hierarchyMethodnotdefined(); // 8x8
        public static final Image ShouldDefineMethod = PlatformIconGroup.hierarchyShoulddefinemethod(); // 9x9
        public static final Image Subtypes = PlatformIconGroup.hierarchySubtypes(); // 16x16
        public static final Image Supertypes = PlatformIconGroup.hierarchySupertypes(); // 16x16

    }

    public static final Image Icon16 = PlatformIconGroup.icon16(); // 16x16
    public static final Image Icon16_Sandbox = PlatformIconGroup.icon16_sandbox(); // 16x16
    public static final Image Icon32 = PlatformIconGroup.icon32(); // 32x32

    public static class Ide {

        public static class Dnd {
            public static final Image Bottom = PlatformIconGroup.ideDndBottom(); // 16x16
            public static final Image Left = PlatformIconGroup.ideDndLeft(); // 16x16
            public static final Image Right = PlatformIconGroup.ideDndRight(); // 16x16
            public static final Image Top = PlatformIconGroup.ideDndTop(); // 16x16
        }

        public static final Image ErrorPoint = PlatformIconGroup.ideErrorpoint(); // 6x6
        public static final Image FatalError_read = PlatformIconGroup.ideFatalerror_read(); // 16x16
        public static final Image FatalError = PlatformIconGroup.ideFatalerror(); // 16x16
        public static final Image HectorOff = PlatformIconGroup.ideHectoroff(); // 16x16
        public static final Image HectorOn = PlatformIconGroup.ideHectoron(); // 16x16
        public static final Image HectorSyntax = PlatformIconGroup.ideHectorsyntax(); // 16x16
        public static final Image IncomingChangesOn = PlatformIconGroup.ideIncomingchangeson(); // 16x16
        public static final Image Link = PlatformIconGroup.ideLink(); // 12x12
        public static final Image LocalScope = PlatformIconGroup.ideLocalscope(); // 16x16

        public static class Macro {
            public static final Image Recording_1 = PlatformIconGroup.ideMacroRecording_1(); // 16x16
            public static final Image Recording_2 = PlatformIconGroup.ideMacroRecording_2(); // 16x16
            public static final Image Recording_3 = PlatformIconGroup.ideMacroRecording_3(); // 16x16
            public static final Image Recording_4 = PlatformIconGroup.ideMacroRecording_4(); // 16x16

        }

        public static class Notification {
            public static final Image Close = PlatformIconGroup.ideNotificationClose(); // 16x16
            public static final Image CloseHover = PlatformIconGroup.ideNotificationClosehover(); // 16x16
            public static final Image Collapse = PlatformIconGroup.ideNotificationCollapse(); // 16x16
            public static final Image CollapseHover = PlatformIconGroup.ideNotificationCollapsehover(); // 16x16
            public static final Image DropTriangle = PlatformIconGroup.ideNotificationDroptriangle(); // 11x8
            public static final Image Expand = PlatformIconGroup.ideNotificationExpand(); // 16x16
            public static final Image ExpandHover = PlatformIconGroup.ideNotificationExpandhover(); // 16x16
            public static final Image Gear = PlatformIconGroup.ideNotificationGear(); // 16x16
            public static final Image GearHover = PlatformIconGroup.ideNotificationGearhover(); // 16x16
        }

        public static final Image OutgoingChangesOn = PlatformIconGroup.ideOutgoingchangeson(); // 16x16
        public static final Image Pipette = PlatformIconGroup.idePipette(); // 16x16
        public static final Image Pipette_rollover = PlatformIconGroup.idePipette_rollover(); // 16x16
        public static final Image Readonly = PlatformIconGroup.ideReadonly(); // 16x16
        public static final Image Readwrite = PlatformIconGroup.ideReadwrite(); // 16x16

        public static class Shadow {
            public static final Image Bottom_left = PlatformIconGroup.ideShadowLeft(); // 18x22
            public static final Image Bottom_right = PlatformIconGroup.ideShadowBottomright(); // 18x22
            public static final Image Bottom = PlatformIconGroup.ideShadowBottom(); // 4x14
            public static final Image Left = PlatformIconGroup.ideShadowLeft(); // 10x4
            public static final Image Right = PlatformIconGroup.ideShadowRight(); // 10x4
            public static final Image Top_left = PlatformIconGroup.ideShadowTopleft(); // 18x14
            public static final Image Top_right = PlatformIconGroup.ideShadowTopright(); // 18x14
            public static final Image Top = PlatformIconGroup.ideShadowTop(); // 4x6
        }

        public static final Image SharedScope = PlatformIconGroup.ideSharedscope(); // 16x16
        public static final Image UpDown = PlatformIconGroup.ideUpdown(); // 16x16

    }

    public static class Mac {
        public static final Image AppIconOk512 = PlatformIconGroup.macAppiconok512(); // 55x55
    }

    public static class Modules {
        public static final Image AddExcludedRoot = PlatformIconGroup.modulesAddexcludedroot(); // 16x16
        public static final Image Annotation = PlatformIconGroup.modulesAnnotation(); // 16x16
        public static final Image ExcludeRoot = PlatformIconGroup.modulesExcluderoot(); // 16x16
        public static final Image SourceRoot = PlatformIconGroup.modulesSourceroot(); // 16x16
        public static final Image ResourcesRoot = PlatformIconGroup.modulesResourcesroot(); // 16x16
        public static final Image TestResourcesRoot = PlatformIconGroup.modulesTestresourcesroot(); // 16x16
        public static final Image TestRoot = PlatformIconGroup.modulesTestroot(); // 16x16
        public static final Image WebRoot = PlatformIconGroup.modulesWebroot(); // 16x16

    }

    public static class Nodes {
        public static final Image Attribute = PlatformIconGroup.nodesAttribute(); // 16x16
        public static final Image AbstractAttribute = PlatformIconGroup.nodesAbstractattribute(); // 16x16
        public static final Image AbstractClass = PlatformIconGroup.nodesAbstractclass(); // 16x16
        public static final Image AbstractException = PlatformIconGroup.nodesAbstractexception(); // 16x16
        public static final Image AbstractMethod = PlatformIconGroup.nodesAbstractmethod(); // 16x16
        public static final Image AbstractStruct = PlatformIconGroup.nodesAbstractmethod(); // 16x16
        public static final Image Annotationtype = PlatformIconGroup.nodesAnnotationtype(); // 16x16
        public static final Image AnonymousClass = PlatformIconGroup.nodesAnonymousclass(); // 16x16
        public static final Image Artifact = PlatformIconGroup.nodesArtifact(); // 16x16
        public static final Image C_plocal = PlatformIconGroup.nodesC_plocal(); // 16x16
        public static final Image C_private = PlatformIconGroup.nodesC_private(); // 16x16
        public static final Image C_protected = PlatformIconGroup.nodesC_protected(); // 16x16
        public static final Image C_public = PlatformIconGroup.nodesC_public(); // 16x16
        public static final Image Class = PlatformIconGroup.nodesClass(); // 16x16
        public static final Image ClassInitializer = PlatformIconGroup.nodesClassinitializer(); // 16x16
        public static final Image ConfigFolder = PlatformIconGroup.nodesConfigfolder(); // 16x16
        public static final Image CopyOfFolder = PlatformIconGroup.nodesCopyoffolder(); // 16x16
        public static final Image Deploy = PlatformIconGroup.nodesDeploy(); // 16x16
        public static final Image Desktop = PlatformIconGroup.nodesDesktop(); // 16x16
        public static final Image EntryPoints = PlatformIconGroup.nodesEntrypoints(); // 16x16
        public static final Image Enum = PlatformIconGroup.nodesEnum();
        public static final Image ErrorMark = PlatformIconGroup.nodesErrormark(); // 16x16
        public static final Image ExceptionClass = PlatformIconGroup.nodesExceptionclass(); // 16x16
        public static final Image ExcludedFromCompile = PlatformIconGroup.nodesExcludedfromcompile(); // 16x16
        public static final Image ExtractedFolder = PlatformIconGroup.nodesExtractedfolder(); // 16x16
        public static final Image Event = PlatformIconGroup.nodesEvent(); // 16x16
        public static final Image Field = PlatformIconGroup.nodesField(); // 16x16
        public static final Image FinalMark = PlatformIconGroup.nodesFinalmark(); // 16x16
        public static final Image Folder = PlatformIconGroup.nodesFolder(); // 16x16
        public static final Image Function = PlatformIconGroup.nodesFunction(); // 16x16
        public static final Image Lambda = PlatformIconGroup.nodesLambda(); // 16x16
        public static final Image HomeFolder = PlatformIconGroup.nodesHomefolder(); // 16x16
        public static final Image InspectionResults = PlatformIconGroup.nodesInspectionresults(); // 16x16
        public static final Image Interface = PlatformIconGroup.nodesInterface();
        public static final Image Trait = PlatformIconGroup.nodesTrait(); // 16x16
        public static final Image TypeAlias = PlatformIconGroup.nodesTypealias(); // 16x16
        public static final Image J2eeParameter = PlatformIconGroup.nodesJ2eeparameter(); // 16x16
        public static final Image JarDirectory = PlatformIconGroup.nodesJardirectory(); // 16x16
        public static final Image JavaDocFolder = PlatformIconGroup.nodesJavadocfolder(); // 16x16
        public static final Image JunitTestMark = PlatformIconGroup.nodesJunittestmark(); // 16x16
        public static final Image KeymapEditor = PlatformIconGroup.nodesKeymapeditor(); // 16x16
        public static final Image KeymapMainMenu = PlatformIconGroup.nodesKeymapmainmenu(); // 16x16
        public static final Image KeymapOther = PlatformIconGroup.nodesKeymapother(); // 16x16
        public static final Image KeymapTools = PlatformIconGroup.nodesKeymaptools(); // 16x16
        public static final Image Locked = PlatformIconGroup.nodesLocked(); // 16x16
        public static final Image Method = PlatformIconGroup.nodesMethod(); // 16x16
        public static final Image MethodReference = PlatformIconGroup.nodesMethodreference(); // 16x16
        public static final Image Module = PlatformIconGroup.nodesModule(); // 16x16
        public static final Image ModuleGroup = PlatformIconGroup.nodesModulegroup(); // 16x16
        public static final Image NodePlaceholder = PlatformIconGroup.nodesNodeplaceholder(); // 16x16
        public static final Image Package = PlatformIconGroup.nodesPackage(); // 16x16
        public static final Image TestPackage = Package; // 16x16
        public static final Image Parameter = PlatformIconGroup.nodesParameter(); // 16x16
        public static final Image Plugin = PlatformIconGroup.nodesPlugin(); // 16x16
        public static final Image PpFile = PlatformIconGroup.nodesFolder(); // 16x16
        public static final Image PpInvalid = PlatformIconGroup.nodesPpinvalid(); // 16x16
        @Deprecated
        public static final Image PpJar = PlatformIconGroup.filetypesArchive();
        public static final Image PpLib = PlatformIconGroup.nodesPplib(); // 16x16
        public static final Image PpLibFolder = PlatformIconGroup.nodesPplibfolder(); // 16x16
        public static final Image PpWeb = PlatformIconGroup.nodesPpweb(); // 16x16
        public static final Image Project = PlatformIconGroup.nodesProject(); // 16x16
        public static final Image Property = PlatformIconGroup.nodesProperty(); // 16x16
        public static final Image PropertyRead = PlatformIconGroup.nodesPropertyread(); // 16x16
        public static final Image PropertyReadStatic = PlatformIconGroup.nodesPropertyreadstatic(); // 16x16
        public static final Image PropertyReadWrite = PlatformIconGroup.nodesPropertyreadwrite(); // 16x16
        public static final Image PropertyReadWriteStatic = PlatformIconGroup.nodesPropertyreadwritestatic(); // 16x16
        public static final Image PropertyWrite = PlatformIconGroup.nodesPropertywrite(); // 16x16
        public static final Image PropertyWriteStatic = PlatformIconGroup.nodesPropertywritestatic(); // 16x16
        public static final Image Read_access = PlatformIconGroup.nodesRead_access(); // 13x9
        public static final Image ResourceBundle = PlatformIconGroup.nodesResourcebundle(); // 16x16
        public static final Image RunnableMark = PlatformIconGroup.nodesRunnablemark(); // 16x16
        public static final Image Rw_access = PlatformIconGroup.nodesRw_access(); // 13x9
        public static final Image SortBySeverity = PlatformIconGroup.nodesSortbyseverity(); // 16x16
        public static final Image Static = PlatformIconGroup.nodesStatic(); // 16x16
        public static final Image StaticMark = PlatformIconGroup.nodesStaticmark(); // 16x16
        public static final Image Struct = PlatformIconGroup.nodesStruct(); // 16x16
        public static final Image Symlink = PlatformIconGroup.nodesSymlink(); // 16x16
        public static final Image TabAlert = PlatformIconGroup.nodesTabalert(); // 16x16
        public static final Image TabPin = PlatformIconGroup.nodesTabpin(); // 16x16
        public static final Image Tag = PlatformIconGroup.nodesTag(); // 16x16
        public static final Image TreeClosed = PlatformIconGroup.nodesTreeclosed(); // 16x16
        public static final Image TreeOpen = PlatformIconGroup.nodesTreeopen(); // 16x16
        public static final Image Undeploy = PlatformIconGroup.nodesUndeploy(); // 16x16
        public static final Image UnknownJdk = PlatformIconGroup.nodesUnknownjdk(); // 16x16
        public static final Image UpFolder = PlatformIconGroup.nodesUpfolder(); // 16x16
        public static final Image UpLevel = PlatformIconGroup.nodesUplevel(); // 16x16
        public static final Image Variable = PlatformIconGroup.nodesVariable(); // 16x16
        public static final Image Value = PlatformIconGroup.nodesValue(); // 16x16
        public static final Image WebFolder = PlatformIconGroup.nodesWebfolder(); // 16x16
        public static final Image Write_access = PlatformIconGroup.nodesWrite_access(); // 13x9
        public static final Image CustomRegion = PlatformIconGroup.nodesCustomregion();

    }

    public static class ObjectBrowser {
        public static final Image AbbreviatePackageNames = PlatformIconGroup.objectbrowserAbbreviatepackagenames(); // 16x16
        public static final Image CompactEmptyPackages = PlatformIconGroup.objectbrowserCompactemptypackages(); // 16x16
        public static final Image FlattenPackages = PlatformIconGroup.objectbrowserFlattenpackages(); // 16x16
        public static final Image ShowLibraryContents = PlatformIconGroup.objectbrowserShowlibrarycontents(); // 16x16
        public static final Image ShowMembers = PlatformIconGroup.objectbrowserShowmembers(); // 16x16
        public static final Image SortByType = PlatformIconGroup.objectbrowserSortbytype(); // 16x16
        public static final Image Sorted = PlatformIconGroup.objectbrowserSorted(); // 16x16
        public static final Image SortedByUsage = PlatformIconGroup.objectbrowserSortedbyusage(); // 16x16
        public static final Image VisibilitySort = PlatformIconGroup.objectbrowserVisibilitysort(); // 16x16

    }

    public static class Process {

        public static class Big {
            public static final Image Step_1 = PlatformIconGroup.processBigStep_1(); // 32x32
            public static final Image Step_2 = PlatformIconGroup.processBigStep_2(); // 32x32
            public static final Image Step_3 = PlatformIconGroup.processBigStep_3(); // 32x32
            public static final Image Step_4 = PlatformIconGroup.processBigStep_4(); // 32x32
            public static final Image Step_5 = PlatformIconGroup.processBigStep_5(); // 32x32
            public static final Image Step_6 = PlatformIconGroup.processBigStep_6(); // 32x32
            public static final Image Step_7 = PlatformIconGroup.processBigStep_7(); // 32x32
            public static final Image Step_8 = PlatformIconGroup.processBigStep_8(); // 32x32
            public static final Image Step_passive = PlatformIconGroup.processBigStep_passive(); // 32x32

        }

        public static final Image Step_1 = PlatformIconGroup.processStep_1(); // 16x16
        public static final Image Step_2 = PlatformIconGroup.processStep_2(); // 16x16
        public static final Image Step_3 = PlatformIconGroup.processStep_3(); // 16x16
        public static final Image Step_4 = PlatformIconGroup.processStep_4(); // 16x16
        public static final Image Step_5 = PlatformIconGroup.processStep_5(); // 16x16
        public static final Image Step_6 = PlatformIconGroup.processStep_6(); // 16x16
        public static final Image Step_7 = PlatformIconGroup.processStep_7(); // 16x16
        public static final Image Step_8 = PlatformIconGroup.processStep_8(); // 16x16
        public static final Image Step_mask = PlatformIconGroup.processStep_mask(); // 16x16
        public static final Image Step_passive = PlatformIconGroup.processStep_passive(); // 16x16
        public static final Image Stop = PlatformIconGroup.processStop(); // 16x16
        public static final Image StopSmall = PlatformIconGroup.processStopsmall(); // 16x16
        public static final Image StopHovered = PlatformIconGroup.processStophovered(); // 16x16
        public static final Image StopSmallHovered = PlatformIconGroup.processStopsmallhovered(); // 16x16

    }

    public static class RunConfigurations {
        public static final Image Application = PlatformIconGroup.runconfigurationsApplication(); // 16x16
        public static final Image HidePassed = PlatformIconGroup.runconfigurationsHidepassed(); // 16x16
        public static final Image IgnoredTest = PlatformIconGroup.runconfigurationsIgnoredtest(); // 16x16
        public static final Image InvalidConfigurationLayer = PlatformIconGroup.runconfigurationsInvalidconfigurationlayer(); // 16x16
        public static final Image Junit = PlatformIconGroup.runconfigurationsJunit(); // 16x16
        public static final Image Remote = PlatformIconGroup.runconfigurationsRemote(); // 16x16
        public static final Image RerunFailedTests = PlatformIconGroup.runconfigurationsRerunfailedtests(); // 16x16
        public static final Image Scroll_down = PlatformIconGroup.runconfigurationsScroll_down(); // 16x16
        public static final Image SortbyDuration = PlatformIconGroup.runconfigurationsSortbyduration(); // 16x16
        public static final Image TestError = PlatformIconGroup.runconfigurationsTesterror(); // 16x16
        public static final Image TestFailed = PlatformIconGroup.runconfigurationsTestfailed(); // 16x16
        public static final Image TestIgnored = PlatformIconGroup.runconfigurationsTestignored(); // 16x16
        public static final Image TestMark = PlatformIconGroup.runconfigurationsTestmark(); // 16x16
        public static final Image TestNotRan = PlatformIconGroup.runconfigurationsTestnotran(); // 16x16
        public static final Image TestPassed = PlatformIconGroup.runconfigurationsTestpassed(); // 16x16
        public static final Image TestPaused = PlatformIconGroup.runconfigurationsTestpaused(); // 16x16
        public static final Image TestSkipped = PlatformIconGroup.runconfigurationsTestskipped(); // 16x16
        public static final Image TestTerminated = PlatformIconGroup.runconfigurationsTestterminated(); // 16x16
        public static final Image Web_app = PlatformIconGroup.runconfigurationsWeb_app(); // 16x16

    }

    public static class Toolbar {
        public static final Image Filterdups = PlatformIconGroup.toolbarFilterdups(); // 16x16
        public static final Image Unknown = PlatformIconGroup.toolbarUnknown(); // 16x16

    }

    public static class Toolwindows {
        public static final Image Documentation = PlatformIconGroup.toolwindowsDocumentation(); // 13x13
        public static final Image ToolWindowChanges = PlatformIconGroup.toolwindowsToolwindowchanges(); // 13x13
        public static final Image ToolWindowFind = PlatformIconGroup.toolwindowsToolwindowfind(); // 13x13
        public static final Image ToolWindowHierarchy = PlatformIconGroup.toolwindowsToolwindowhierarchy(); // 13x13
        public static final Image ToolWindowMessages = PlatformIconGroup.toolwindowsToolwindowmessages(); // 13x13
        public static final Image ToolWindowModuleDependencies = PlatformIconGroup.toolwindowsToolwindowmoduledependencies(); // 13x13
        public static final Image ToolWindowPalette = PlatformIconGroup.toolwindowsToolwindowpalette(); // 13x13
        public static final Image ToolWindowProject = PlatformIconGroup.toolwindowsToolwindowproject(); // 13x13
        public static final Image ToolWindowRun = PlatformIconGroup.toolwindowsToolwindowrun(); // 13x13
        public static final Image ToolWindowStructure = PlatformIconGroup.toolwindowsToolwindowstructure(); // 13x13
        public static final Image ToolWindowTodo = PlatformIconGroup.toolwindowsToolwindowtodo(); // 13x13
        public static final Image WebToolWindow = PlatformIconGroup.toolwindowsWebtoolwindow(); // 13x13
    }

    public static class Vcs {
        public static final Image Arrow_left = PlatformIconGroup.vcsArrow_left(); // 16x16
        public static final Image Arrow_right = PlatformIconGroup.vcsArrow_right(); // 16x16
        public static final Image Equal = PlatformIconGroup.vcsEqual(); // 16x16
        public static final Image History = PlatformIconGroup.vcsHistory(); // 16x16
        public static final Image MapBase = PlatformIconGroup.vcsMapbase(); // 16x16
        public static final Image Merge = PlatformIconGroup.vcsMerge(); // 12x12
        public static final Image MergeSourcesTree = PlatformIconGroup.vcsMergesourcestree(); // 16x16
        public static final Image Not_equal = PlatformIconGroup.vcsNot_equal(); // 16x16
        public static final Image ResetStrip = PlatformIconGroup.vcsResetstrip(); // 16x16
        public static final Image StripDown = PlatformIconGroup.vcsStripdown(); // 16x16
        public static final Image StripNull = PlatformIconGroup.vcsStripnull(); // 16x16
        public static final Image StripUp = PlatformIconGroup.vcsStripup(); // 16x16

    }

    public static class Welcome {
        public static final Image CreateNewProject = PlatformIconGroup.welcomeCreatenewproject(); // 16x16
        public static final Image FromVCS = PlatformIconGroup.welcomeFromvcs(); // 16x16
        public static final Image OpenProject = PlatformIconGroup.welcomeOpenproject(); // 16x16
    }

    public static class Webreferences {
        public static final Image Server = PlatformIconGroup.webreferencesServer(); // 16x16

    }

    public static final class Scope {
        /**
         * 16x16
         */
        public static final Image ChangedFiles = PlatformIconGroup.scopeChangedfiles();
        /**
         * 16x16
         */
        public static final Image ChangedFilesAll = PlatformIconGroup.scopeChangedfilesall();
    }
}
