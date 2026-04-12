// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.ide.util.gotoByName;

import com.google.common.annotations.VisibleForTesting;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.ui.UISettings;
import consulo.application.util.NotNullLazyValue;
import consulo.application.util.Semaphore;
import consulo.application.util.VolatileNotNullLazyValue;
import consulo.application.util.matcher.MatcherTextRange;
import consulo.application.util.matcher.WordPrefixMatcher;
import consulo.codeEditor.Editor;
import consulo.component.ProcessCanceledException;
import consulo.component.util.localize.BundleBase;
import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.dataContext.DataContext;
import consulo.ide.impl.base.BaseShowSettingsUtil;
import consulo.ide.impl.idea.ide.actions.ApplyIntentionAction;
import consulo.ui.ex.action.BooleanOptionDescription;
import consulo.ui.ex.action.OptionDescription;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ToggleSwitch;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearchUtil;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.style.StyleManager;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static consulo.ide.impl.idea.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts;
import static consulo.ui.ex.SimpleTextAttributes.STYLE_PLAIN;
import static consulo.ui.ex.SimpleTextAttributes.STYLE_SEARCH_MATCH;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class GotoActionModel implements ChooseByNameModel, Comparator<Object>, DumbAware {
    private static final Logger LOG = Logger.getInstance(GotoActionModel.class);
    private static final Pattern INNER_GROUP_WITH_IDS = Pattern.compile("(.*) \\(\\d+\\)");

    private final @Nullable Project myProject;
    private final Component myContextComponent;
    private final @Nullable Editor myEditor;

    protected final ActionManager myActionManager = ActionManager.getInstance();

    private final Map<AnAction, GroupMapping> myActionGroups = new HashMap<>();

    private final NotNullLazyValue<Map<String, String>> myConfigurablesNames = VolatileNotNullLazyValue.createValue(() -> {
        if (SwingUtilities.isEventDispatchThread()) {
            LOG.error("Configurable names must not be loaded on EDT");
        }

        Map<String, String> map = new HashMap<>();
        for (Configurable configurable : BaseShowSettingsUtil.buildConfigurables(getProject())) {
            if (configurable instanceof SearchableConfigurable) {
                map.put(configurable.getId(), configurable.getDisplayName().get());
            }
        }
        return map;
    });

    private final IdeaModalityState myModality;

    public GotoActionModel(@Nullable Project project, Component component, @Nullable Editor editor) {
        this(project, component, editor, IdeaModalityState.defaultModalityState());
    }

    public GotoActionModel(
        @Nullable Project project,
        Component component,
        @Nullable Editor editor,
        @Nullable IdeaModalityState modalityState
    ) {
        myProject = project;
        myContextComponent = component;
        myEditor = editor;
        myModality = modalityState;
        ActionGroup mainMenu = (ActionGroup)myActionManager.getActionOrStub(IdeActions.GROUP_MAIN_MENU);
        ActionGroup keymapOthers = (ActionGroup)myActionManager.getActionOrStub("Other.KeymapGroup");
        assert mainMenu != null && keymapOthers != null;
        collectActions(myActionGroups, mainMenu, emptyList(), false);

        Map<AnAction, GroupMapping> keymapActionGroups = new HashMap<>();
        collectActions(keymapActionGroups, keymapOthers, emptyList(), true);
        // Let menu groups have priority over keymap (and do not introduce ambiguity)
        keymapActionGroups.forEach(myActionGroups::putIfAbsent);
    }

    Map<String, ApplyIntentionAction> getAvailableIntentions() {
        Map<String, ApplyIntentionAction> map = new TreeMap<>();
        if (myProject != null && !myProject.isDisposed() && !DumbService.isDumb(myProject) && myEditor != null && !myEditor.isDisposed()) {
            PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
            ApplyIntentionAction[] children = file == null ? null : ApplyIntentionAction.getAvailableIntentions(myEditor, file);
            if (children != null) {
                for (ApplyIntentionAction action : children) {
                    map.put(action.getName().get(), action);
                }
            }
        }
        return map;
    }

    @Override
    public String getPromptText() {
        return IdeLocalize.promptGotoactionEnterAction().get();
    }

    @Override
    public @Nullable LocalizeValue getCheckBoxName() {
        return IdeLocalize.checkboxDisabledIncluded();
    }

    @Override
    public String getNotInMessage() {
        return IdeLocalize.labelNoEnabledActionsFound().get();
    }

    @Override
    public String getNotFoundMessage() {
        return IdeLocalize.labelNoActionsFound().get();
    }

    @Override
    public boolean loadInitialCheckBoxState() {
        return false;
    }

    @Override
    public void saveInitialCheckBoxState(boolean state) {
    }

    public static class MatchedValue {
        public final Object value;
        
        final String pattern;

        public MatchedValue(Object value, String pattern) {
            assert value instanceof OptionDescription || value instanceof ActionWrapper : "unknown value: " + value.getClass();
            this.value = value;
            this.pattern = pattern;
        }

        @VisibleForTesting
        public @Nullable String getValueText() {
            if (value instanceof OptionDescription optionDescription) {
                return optionDescription.getHit();
            }
            if (value instanceof ActionWrapper actionWrapper) {
                return actionWrapper.getAction().getTemplatePresentation().getText();
            }
            return null;
        }

        @Override
        public @Nullable String toString() {
            return getMatchingDegree() + " " + getValueText();
        }

        private int getMatchingDegree() {
            String text = getValueText();
            if (text != null) {
                int degree = getRank(text);
                return value instanceof ActionWrapper actionWrapper && !actionWrapper.isGroupAction() ? degree + 1 : degree;
            }
            return 0;
        }

        private int getRank(String text) {
            if (StringUtil.equalsIgnoreCase(StringUtil.trimEnd(text, "..."), pattern)) {
                return 3;
            }
            if (StringUtil.startsWithIgnoreCase(text, pattern)) {
                return 2;
            }
            if (StringUtil.containsIgnoreCase(text, pattern)) {
                return 1;
            }
            return 0;
        }

        public int compareWeights(MatchedValue o) {
            if (o == this) {
                return 0;
            }
            int diff = o.getMatchingDegree() - getMatchingDegree();
            if (diff != 0) {
                return diff;
            }

            diff = getTypeWeight(o.value) - getTypeWeight(value);
            if (diff != 0) {
                return diff;
            }

            if (value instanceof ActionWrapper value1 && o.value instanceof ActionWrapper value2) {
                int compared = value1.compareWeights(value2);
                if (compared != 0) {
                    return compared;
                }
            }

            diff = StringUtil.notNullize(getValueText()).length() - StringUtil.notNullize(o.getValueText()).length();
            if (diff != 0) {
                return diff;
            }

            if (value instanceof OptionDescription value1 && o.value instanceof OptionDescription value2) {
                diff = value1.compareTo(value2);
                if (diff != 0) {
                    return diff;
                }
            }

            return o.hashCode() - hashCode();
        }

        private static int getTypeWeight(Object value) {
            if (value instanceof ActionWrapper actionWrapper) {
                if ((UIAccess.isUIThread() || actionWrapper.hasPresentation()) && actionWrapper.isAvailable()) {
                    return 0;
                }
                return 2;
            }
            if (value instanceof OptionDescription) {
                return value instanceof BooleanOptionDescription ? 1 : 3;
            }
            throw new IllegalArgumentException(value.getClass() + " - " + value.toString());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MatchedValue value1 = (MatchedValue)o;
            return Objects.equals(value, value1.value) && Objects.equals(pattern, value1.pattern);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, pattern);
        }
    }

    @Override
    public ListCellRenderer getListCellRenderer() {
        return new GotoActionListCellRenderer(this::getGroupName);
    }

    protected String getActionId(AnAction anAction) {
        return myActionManager.getId(anAction);
    }

    private static JLabel createIconLabel(@Nullable Image icon, boolean disabled) {
        if (icon == null) {
            return new JBLabel(Image.empty(Image.DEFAULT_ICON_SIZE));
        }

        int width = icon.getWidth();
        int height = icon.getHeight();
        if (width != Image.DEFAULT_ICON_SIZE || height != Image.DEFAULT_ICON_SIZE) {
            return new JBLabel(ImageEffects.resize(icon, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE));
        }

        return new JBLabel(icon);
    }

    @Override
    public int compare(Object o1, Object o2) {
        if (ChooseByNameBase.EXTRA_ELEM.equals(o1)) {
            return 1;
        }
        if (ChooseByNameBase.EXTRA_ELEM.equals(o2)) {
            return -1;
        }
        return ((MatchedValue)o1).compareWeights((MatchedValue)o2);
    }

    public static AnActionEvent updateActionBeforeShow(AnAction anAction, DataContext dataContext) {
        Presentation presentation = new Presentation();
        presentation.copyFrom(anAction.getTemplatePresentation());
        AnActionEvent event = AnActionEvent.createFromDataContext(ActionPlaces.ACTION_SEARCH, presentation, dataContext);
        ActionImplUtil.performDumbAwareUpdate(anAction, event, false);
        return event;
    }

    /**
     * @deprecated Please use {@link GotoActionModel#defaultActionForeground(boolean, boolean, Presentation)} instead.
     * This method may be removed in future versions
     */
    @Deprecated
    public static Color defaultActionForeground(boolean isSelected, @Nullable Presentation presentation) {
        return defaultActionForeground(isSelected, true, presentation);
    }

    public static Color defaultActionForeground(boolean isSelected, boolean hasFocus, @Nullable Presentation presentation) {
        if (isSelected) {
            return UIUtil.getListSelectionForeground(hasFocus);
        }
        if (presentation != null && !presentation.isEnabledAndVisible()) {
            return UIUtil.getInactiveTextColor();
        }
        return UIUtil.getListForeground();
    }

    @Override
    public String[] getNames(boolean checkBoxState) {
        return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    @Override
    public Object[] getElementsByName(String id, boolean checkBoxState, String pattern) {
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    public String getGroupName(OptionDescription description) {
        //if (description instanceof RegistryTextOptionDescriptor) return "Registry";
        String groupName = description.getGroupName();
        String settings = Platform.current().os().isMac() ? "Preferences" : "Settings";
        if (groupName == null || groupName.equals(description.getHit())) {
            return settings;
        }
        return settings + " > " + groupName;
    }

    Map<String, String> getConfigurablesNames() {
        return myConfigurablesNames.getValue();
    }

    private void collectActions(
        Map<AnAction, GroupMapping> actionGroups,
        ActionGroup group,
        List<ActionGroup> path,
        boolean showNonPopupGroups
    ) {
        AnAction[] actions = group.getChildren(null);

        boolean hasMeaningfulChildren = ContainerUtil.exists(actions, action -> myActionManager.getId(action) != null);
        if (!hasMeaningfulChildren) {
            GroupMapping mapping = actionGroups.computeIfAbsent(group, (key) -> new GroupMapping(showNonPopupGroups));
            mapping.addPath(path);
        }

        List<ActionGroup> newPath = consulo.ide.impl.idea.util.containers.ContainerUtil.append(path, group);
        for (AnAction action : actions) {
            if (action == null || action instanceof AnSeparator) {
                continue;
            }
            if (action instanceof ActionGroup actionGroup) {
                collectActions(actionGroups, actionGroup, newPath, showNonPopupGroups);
            }
            else {
                GroupMapping mapping = actionGroups.computeIfAbsent(action, (key) -> new GroupMapping(showNonPopupGroups));
                mapping.addPath(newPath);
            }
        }
    }

    @Nullable GroupMapping getGroupMapping(AnAction action) {
        return myActionGroups.get(action);
    }

    @Override
    public @Nullable String getFullName(Object element) {
        return getElementName(element);
    }

    @Override
    public String getHelpId() {
        return "procedures.navigating.goto.action";
    }

    @Override
    
    public String[] getSeparators() {
        return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    @Override
    public @Nullable String getElementName(Object mv) {
        return ((MatchedValue)mv).getValueText();
    }

    protected MatchMode actionMatches(
        String pattern,
        consulo.application.util.matcher.Matcher matcher,
        AnAction anAction
    ) {
        Presentation presentation = anAction.getTemplatePresentation();
        String text = presentation.getText();
        String description = presentation.getDescription().getNullIfEmpty();
        if (text != null && matcher.matches(text)) {
            return MatchMode.NAME;
        }
        else if (description != null && !description.equals(text) && new WordPrefixMatcher(pattern).matches(description)) {
            return MatchMode.DESCRIPTION;
        }
        if (text == null) {
            return MatchMode.NONE;
        }

        GroupMapping groupMapping = myActionGroups.get(anAction);
        if (groupMapping != null) {
            for (String groupName : groupMapping.getAllGroupNames()) {
                if (matcher.matches(groupName + " " + text)) {
                    return anAction instanceof ToggleAction ? MatchMode.NAME : MatchMode.GROUP;
                }
                if (matcher.matches(text + " " + groupName)) {
                    return MatchMode.GROUP;
                }
            }
        }

        //for (GotoActionAliasMatcher m : GotoActionAliasMatcher.EP_NAME.getExtensions()) {
        //    if (m.match(anAction, pattern)) {
        //        return MatchMode.NAME;
        //    }
        //}
        return MatchMode.NONE;
    }

    protected @Nullable Project getProject() {
        return myProject;
    }

    protected Component getContextComponent() {
        return myContextComponent;
    }

    public SortedSet<Object> sortItems(Set<Object> elements) {
        TreeSet<Object> objects = new TreeSet<>(this);
        objects.addAll(elements);
        return objects;
    }

    private void updateOnEdt(Runnable update) {
        Semaphore semaphore = new Semaphore(1);
        ProgressIndicator indicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
        Application.get().invokeLater(
            () -> {
                try {
                    update.run();
                }
                finally {
                    semaphore.up();
                }
            },
            myModality,
            () -> indicator != null && indicator.isCanceled()
        );

        while (!semaphore.waitFor(10)) {
            if (indicator != null && indicator.isCanceled()) {
                // don't use `checkCanceled` because some smart devs might suppress PCE and end up with a deadlock like IDEA-177788
                throw new ProcessCanceledException();
            }
        }
    }

    public enum MatchMode {
        NONE,
        INTENTION,
        NAME,
        DESCRIPTION,
        GROUP,
        NON_MENU
    }

    @Override
    public boolean willOpenEditor() {
        return false;
    }

    @Override
    public boolean useMiddleMatching() {
        return true;
    }

    public static class GroupMapping implements Comparable<GroupMapping> {
        private final boolean myShowNonPopupGroups;
        private final List<List<ActionGroup>> myPaths = new ArrayList<>();

        private @Nullable String myBestGroupName;
        private boolean myBestNameComputed;

        public GroupMapping() {
            this(false);
        }

        public GroupMapping(boolean showNonPopupGroups) {
            myShowNonPopupGroups = showNonPopupGroups;
        }

        
        public static GroupMapping createFromText(String text) {
            GroupMapping mapping = new GroupMapping();
            mapping.addPath(singletonList(new DefaultActionGroup(text, false)));
            return mapping;
        }

        private void addPath(List<ActionGroup> path) {
            myPaths.add(path);
        }

        @Override
        public int compareTo(GroupMapping o) {
            return Comparing.compare(getFirstGroupName(), o.getFirstGroupName());
        }

        public @Nullable String getBestGroupName() {
            if (myBestNameComputed) {
                return myBestGroupName;
            }
            return getFirstGroupName();
        }

        private @Nullable String getFirstGroupName() {
            List<ActionGroup> path = ContainerUtil.getFirstItem(myPaths);
            return path != null ? getPathName(path) : null;
        }

        private void updateBeforeShow(DataContext context) {
            if (myBestNameComputed) {
                return;
            }
            myBestNameComputed = true;

            for (List<ActionGroup> path : myPaths) {
                String name = getActualPathName(path, context);
                if (name != null) {
                    myBestGroupName = name;
                    return;
                }
            }
        }

        
        public List<String> getAllGroupNames() {
            return ContainerUtil.map(myPaths, this::getPathName);
        }

        private @Nullable String getPathName(List<? extends ActionGroup> path) {
            String name = "";
            for (ActionGroup group : path) {
                name = appendGroupName(name, group, group.getTemplatePresentation());
            }
            return StringUtil.nullize(name);
        }

        private @Nullable String getActualPathName(List<? extends ActionGroup> path, DataContext context) {
            String name = "";
            for (ActionGroup group : path) {
                Presentation presentation = updateActionBeforeShow(group, context).getPresentation();
                if (!presentation.isVisible()) {
                    return null;
                }
                name = appendGroupName(name, group, presentation);
            }
            return StringUtil.nullize(name);
        }

        
        private String appendGroupName(String prefix, ActionGroup group, Presentation presentation) {
            if (group.isPopup() || myShowNonPopupGroups) {
                String groupName = getActionGroupName(presentation);
                if (!StringUtil.isEmptyOrSpaces(groupName)) {
                    return prefix.isEmpty() ? groupName : prefix + " | " + groupName;
                }
            }
            return prefix;
        }

        private static @Nullable String getActionGroupName(Presentation presentation) {
            String text = presentation.getText();
            if (text == null) {
                return null;
            }

            Matcher matcher = INNER_GROUP_WITH_IDS.matcher(text);
            if (matcher.matches()) {
                return matcher.group(1);
            }

            return text;
        }
    }

    public static class ActionWrapper {
        
        private final AnAction myAction;
        
        private final MatchMode myMode;
        private final @Nullable GroupMapping myGroupMapping;
        private final DataContext myDataContext;
        private final GotoActionModel myModel;
        private volatile Presentation myPresentation;

        public ActionWrapper(
            AnAction action,
            @Nullable GroupMapping groupMapping,
            MatchMode mode,
            DataContext dataContext,
            GotoActionModel model
        ) {
            myAction = action;
            myMode = mode;
            myGroupMapping = groupMapping;
            myDataContext = dataContext;
            myModel = model;
        }

        
        public AnAction getAction() {
            return myAction;
        }

        
        public MatchMode getMode() {
            return myMode;
        }

        public int compareWeights(ActionWrapper o) {
            int compared = myMode.compareTo(o.getMode());
            if (compared != 0) {
                return compared;
            }
            Presentation myPresentation = myAction.getTemplatePresentation();
            Presentation oPresentation = o.getAction().getTemplatePresentation();
            String myText = StringUtil.notNullize(myPresentation.getText());
            String oText = StringUtil.notNullize(oPresentation.getText());
            int byText = StringUtil.compare(StringUtil.trimEnd(myText, "..."), StringUtil.trimEnd(oText, "..."), true);
            if (byText != 0) {
                return byText;
            }
            int byTextLength = StringUtil.notNullize(myText).length() - StringUtil.notNullize(oText).length();
            if (byTextLength != 0) {
                return byTextLength;
            }
            int byGroup = Comparing.compare(myGroupMapping, o.myGroupMapping);
            if (byGroup != 0) {
                return byGroup;
            }
            int byDesc = myPresentation.getDescription().compareTo(oPresentation.getDescription());
            if (byDesc != 0) {
                return byDesc;
            }
            int byClassHashCode = Comparing.compare(myAction.getClass().hashCode(), o.myAction.getClass().hashCode());
            if (byClassHashCode != 0) {
                return byClassHashCode;
            }
            int byInstanceHashCode = Comparing.compare(myAction.hashCode(), o.myAction.hashCode());
            if (byInstanceHashCode != 0) {
                return byInstanceHashCode;
            }
            return 0;
        }

        public boolean isAvailable() {
            return getPresentation().isEnabledAndVisible();
        }

        
        public Presentation getPresentation() {
            if (myPresentation != null) {
                return myPresentation;
            }
            Runnable r = () -> {
                myPresentation = updateActionBeforeShow(myAction, myDataContext).getPresentation();
                if (myGroupMapping != null) {
                    myGroupMapping.updateBeforeShow(myDataContext);
                }
            };
            if (UIAccess.isUIThread()) {
                r.run();
            }
            else {
                myModel.updateOnEdt(r);
            }

            return ObjectUtil.notNull(myPresentation, myAction.getTemplatePresentation());
        }

        private boolean hasPresentation() {
            return myPresentation != null;
        }

        public @Nullable String getGroupName() {
            if (myGroupMapping == null) {
                return null;
            }
            String groupName = myGroupMapping.getBestGroupName();
            if (myAction instanceof ActionGroup && Objects.equals(myAction.getTemplatePresentation().getText(), groupName)) {
                return null;
            }
            return groupName;
        }

        public boolean isGroupAction() {
            return myAction instanceof ActionGroup;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this
                || obj instanceof ActionWrapper that
                && myAction.equals(that.myAction);
        }

        @Override
        public int hashCode() {
            String text = myAction.getTemplatePresentation().getText();
            return text != null ? text.hashCode() : 0;
        }

        @Override
        public String toString() {
            return myAction.toString();
        }
    }

    public static class GotoActionListCellRenderer extends DefaultListCellRenderer {
        private final Function<? super OptionDescription, String> myGroupNamer;
        private final boolean myUseListFont;

        public GotoActionListCellRenderer(Function<? super OptionDescription, String> groupNamer) {
            this(groupNamer, false);
        }

        public GotoActionListCellRenderer(Function<? super OptionDescription, String> groupNamer, boolean useListFont) {
            myGroupNamer = groupNamer;
            myUseListFont = useListFont;
        }

        
        @Override
        @RequiredUIAccess
        public Component getListCellRendererComponent(
            JList list,
            Object matchedValue,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            boolean showIcon = UISettings.getInstance().getShowIconsInMenus();
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(JBUI.Borders.empty(2));
            panel.setOpaque(true);
            Color bg = UIUtil.getListBackground(isSelected, cellHasFocus);
            panel.setBackground(bg);

            SimpleColoredComponent nameComponent = new SimpleColoredComponent();
            if (myUseListFont) {
                nameComponent.setFont(list.getFont());
            }
            nameComponent.setBackground(bg);
            panel.add(nameComponent, BorderLayout.CENTER);

            if (matchedValue instanceof String name) { //...
                if (showIcon) {
                    panel.add(new JBLabel(Image.empty(Image.DEFAULT_ICON_SIZE)), BorderLayout.WEST);
                }
                String str = cutName(name, null, list, panel, nameComponent);
                nameComponent.append(str, new SimpleTextAttributes(STYLE_PLAIN, defaultActionForeground(isSelected, cellHasFocus, null)));
                return panel;
            }

            Color groupFg = isSelected ? UIUtil.getListSelectionForeground(true) : UIUtil.getInactiveTextColor();

            Object value = ((MatchedValue)matchedValue).value;
            String pattern = ((MatchedValue)matchedValue).pattern;

            Border eastBorder = JBUI.Borders.emptyRight(2);
            if (value instanceof ActionWrapper actionWithParentGroup) {
                AnAction anAction = actionWithParentGroup.getAction();
                boolean toggle = anAction instanceof ToggleAction;
                String groupName =
                    actionWithParentGroup.getAction() instanceof ApplyIntentionAction ? null : actionWithParentGroup.getGroupName();
                Presentation presentation = actionWithParentGroup.getPresentation();
                Color fg = defaultActionForeground(isSelected, cellHasFocus, presentation);
                boolean disabled = !isSelected && !presentation.isEnabledAndVisible();

                if (disabled) {
                    groupFg = UIUtil.getLabelDisabledForeground();
                }

                if (showIcon) {
                    Image icon = presentation.getIcon();
                    panel.add(createIconLabel(icon, disabled), BorderLayout.WEST);
                }

                if (toggle) {
                    AnActionEvent event =
                        AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, ((ActionWrapper)value).myDataContext);
                    boolean selected = ((ToggleAction)anAction).isSelected(event);
                    addOnOffButton(panel, selected);
                }
                else {
                    if (groupName != null) {
                        JLabel groupLabel = new JLabel(groupName);
                        groupLabel.setBackground(bg);
                        groupLabel.setBorder(eastBorder);
                        groupLabel.setForeground(groupFg);
                        panel.add(groupLabel, BorderLayout.EAST);
                    }
                }

                panel.setToolTipText(presentation.getDescription().getNullIfEmpty());
                Shortcut[] shortcuts = getActiveKeymapShortcuts(ActionManager.getInstance().getId(anAction)).getShortcuts();
                String shortcutText = KeymapUtil.getPreferredShortcutText(shortcuts);
                String name = getName(presentation.getText(), groupName, toggle);
                name = cutName(name, shortcutText, list, panel, nameComponent);

                appendWithColoredMatches(nameComponent, name, pattern, fg, isSelected);
                if (StringUtil.isNotEmpty(shortcutText)) {
                    nameComponent.append(
                        " " + shortcutText,
                        new SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER | SimpleTextAttributes.STYLE_BOLD, groupFg)
                    );
                }
            }
            else if (value instanceof OptionDescription optionDescription) {
                if (!isSelected && !(optionDescription instanceof BooleanOptionDescription)) {
                    Color descriptorBg = StyleManager.get().getCurrentStyle().isDark()
                        ? ColorUtil.brighter(UIUtil.getListBackground(), 1) : LightColors.SLIGHTLY_GRAY;
                    panel.setBackground(descriptorBg);
                    nameComponent.setBackground(descriptorBg);
                }
                String hit = calcHit(optionDescription);
                Color fg = UIUtil.getListForeground(isSelected, cellHasFocus);

                if (showIcon) {
                    panel.add(new JBLabel(Image.empty(Image.DEFAULT_ICON_SIZE)), BorderLayout.WEST);
                }
                if (optionDescription instanceof BooleanOptionDescription booleanOptionDescription) {
                    boolean selected = booleanOptionDescription.isOptionEnabled();
                    addOnOffButton(panel, selected);
                }
                else {
                    JLabel settingsLabel = new JLabel(myGroupNamer.apply(optionDescription));
                    settingsLabel.setForeground(groupFg);
                    settingsLabel.setBackground(bg);
                    settingsLabel.setBorder(eastBorder);
                    panel.add(settingsLabel, BorderLayout.EAST);
                }

                String name = cutName(hit, null, list, panel, nameComponent);
                appendWithColoredMatches(nameComponent, name, pattern, fg, isSelected);
            }
            return panel;
        }

        
        private static String calcHit(OptionDescription value) {
            //if (value instanceof RegistryTextOptionDescriptor) {
            //    return value.getHit() + " = " + value.getValue();
            //}
            String hit = StringUtil.defaultIfEmpty(value.getHit(), value.getOption());
            return StringUtil.unescapeXml(StringUtil.notNullize(hit))
                .replace(BundleBase.MNEMONIC_STRING, "")
                .replace("  ", " "); // avoid extra spaces from mnemonics and xml conversion
        }

        private static String cutName(String name, String shortcutText, JList list, JPanel panel, SimpleColoredComponent nameComponent) {
            if (!list.isShowing() || list.getWidth() <= 0) {
                return StringUtil.first(name, 60, true); //fallback to previous behaviour
            }
            int freeSpace = calcFreeSpace(list, panel, nameComponent, shortcutText);

            if (freeSpace <= 0) {
                return name;
            }

            FontMetrics fm = nameComponent.getFontMetrics(nameComponent.getFont());
            int strWidth = fm.stringWidth(name);
            if (strWidth <= freeSpace) {
                return name;
            }

            int cutSymbolIndex = (int)((((double)freeSpace - fm.stringWidth("...")) / strWidth) * name.length());
            cutSymbolIndex = Integer.max(1, cutSymbolIndex);
            name = name.substring(0, cutSymbolIndex);
            while (fm.stringWidth(name + "...") > freeSpace && name.length() > 1) {
                name = name.substring(0, name.length() - 1);
            }

            return name.trim() + "...";
        }

        private static int calcFreeSpace(JList list, JPanel panel, SimpleColoredComponent nameComponent, String shortcutText) {
            BorderLayout layout = (BorderLayout)panel.getLayout();
            Component eastComponent = layout.getLayoutComponent(BorderLayout.EAST);
            Component westComponent = layout.getLayoutComponent(BorderLayout.WEST);
            int freeSpace = list.getWidth() -
                (list.getInsets().right + list.getInsets().left) -
                (panel.getInsets().right + panel.getInsets().left) -
                (eastComponent == null ? 0 : eastComponent.getPreferredSize().width) -
                (westComponent == null ? 0 : westComponent.getPreferredSize().width) -
                (nameComponent.getInsets().right + nameComponent.getInsets().left) -
                (nameComponent.getIpad().right + nameComponent.getIpad().left) -
                nameComponent.getIconTextGap();

            if (StringUtil.isNotEmpty(shortcutText)) {
                FontMetrics fm = nameComponent.getFontMetrics(nameComponent.getFont().deriveFont(Font.BOLD));
                freeSpace -= fm.stringWidth(" " + shortcutText);
            }

            return freeSpace;
        }

        @RequiredUIAccess
        private static void addOnOffButton(JPanel panel, boolean selected) {
            ToggleSwitch toggleSwitch = ToggleSwitch.create(selected);
            panel.add(TargetAWT.to(toggleSwitch), BorderLayout.EAST);
            panel.setBorder(JBUI.Borders.empty(0, 2));
        }

        
        private static String getName(@Nullable String text, @Nullable String groupName, boolean toggle) {
            return toggle && StringUtil.isNotEmpty(groupName) ? StringUtil.isNotEmpty(text) ? groupName + ": " + text : groupName : StringUtil.notNullize(
                text);
        }

        private static void appendWithColoredMatches(
            SimpleColoredComponent nameComponent,
            String name,
            String pattern,
            Color fg,
            boolean selected
        ) {
            SimpleTextAttributes plain = new SimpleTextAttributes(STYLE_PLAIN, fg);
            SimpleTextAttributes highlighted = new SimpleTextAttributes(null, fg, null, STYLE_SEARCH_MATCH);
            List<MatcherTextRange> fragments = new ArrayList<>();
            if (selected) {
                int matchStart = StringUtil.indexOfIgnoreCase(name, pattern, 0);
                if (matchStart >= 0) {
                    fragments.add(MatcherTextRange.from(matchStart, pattern.length()));
                }
            }
            SpeedSearchUtil.appendColoredFragments(nameComponent, name, fragments, plain, highlighted);
        }
    }
}
