/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInspection.ui;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.component.macro.PathMacroManager;
import consulo.ide.impl.idea.codeInspection.ex.*;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.inspection.GlobalInspectionContextBase;
import consulo.language.editor.impl.internal.inspection.InspectionProjectProfileManager;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.reference.*;
import consulo.language.editor.inspection.scheme.*;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.QuickFixWrapper;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.language.editor.internal.intention.ActionClassHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.SeverityRegistrar;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.macro.ProjectPathMacroManager;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;
import org.jdom.IllegalDataException;

import javax.swing.tree.DefaultTreeModel;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultInspectionToolPresentation implements ProblemDescriptionsProcessor, InspectionToolPresentation {
    @Nonnull
    private final InspectionToolWrapper myToolWrapper;

    @Nonnull
    private final GlobalInspectionContextImpl myContext;
    protected static String ourOutputPath;
    protected InspectionNode myToolNode;

    private static final Object lock = new Object();
    private Map<RefEntity, CommonProblemDescriptor[]> myProblemElements;
    private Map<String, Set<RefEntity>> myContents = null;
    private Set<RefModule> myModulesProblems = null;
    private Map<CommonProblemDescriptor, RefEntity> myProblemToElements;
    private DescriptorComposer myComposer;
    private Map<RefEntity, Set<QuickFix>> myQuickFixActions;
    private Map<RefEntity, CommonProblemDescriptor[]> myIgnoredElements;

    private Map<RefEntity, CommonProblemDescriptor[]> myOldProblemElements = null;
    protected static final Logger LOG = Logger.getInstance(DefaultInspectionToolPresentation.class);
    private boolean isDisposed;

    public DefaultInspectionToolPresentation(@Nonnull InspectionToolWrapper toolWrapper, @Nonnull GlobalInspectionContextImpl context) {
        myToolWrapper = toolWrapper;
        myContext = context;
    }

    @Nonnull
    protected static FileStatus calcStatus(boolean old, boolean current) {
        if (old) {
            if (!current) {
                return FileStatus.DELETED;
            }
        }
        else if (current) {
            return FileStatus.ADDED;
        }
        return FileStatus.NOT_CHANGED;
    }

    public static String stripUIRefsFromInspectionDescription(String description) {
        int descriptionEnd = description.indexOf("<!-- tooltip end -->");
        if (descriptionEnd < 0) {
            Pattern pattern = Pattern.compile(".*Use.*(the (panel|checkbox|checkboxes|field|button|controls).*below).*", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(description);
            int startFindIdx = 0;
            while (matcher.find(startFindIdx)) {
                int end = matcher.end(1);
                startFindIdx = end;
                description = description.substring(0, matcher.start(1)) + " inspection settings " + description.substring(end);
            }
        }
        else {
            description = description.substring(0, descriptionEnd);
        }
        return description;
    }

    @RequiredReadAction
    protected HighlightSeverity getSeverity(@Nonnull RefElement element) {
        PsiElement psiElement = element.getPointer().getContainingFile();
        if (psiElement != null) {
            GlobalInspectionContextImpl context = (GlobalInspectionContextImpl) getContext();
            String shortName = getSeverityDelegateName();
            Tools tools = context.getTools().get(shortName);
            if (tools != null) {
                for (ScopeToolState state : tools.getTools()) {
                    InspectionToolWrapper toolWrapper = state.getTool();
                    if (toolWrapper == getToolWrapper()) {
                        return context.getCurrentProfile().getErrorLevel(HighlightDisplayKey.find(shortName), psiElement).getSeverity();
                    }
                }
            }

            InspectionProfile profile = InspectionProjectProfileManager.getInstance(context.getProject()).getInspectionProfile();
            HighlightDisplayLevel level = profile.getErrorLevel(HighlightDisplayKey.find(shortName), psiElement);
            return level.getSeverity();
        }
        return null;
    }

    protected String getSeverityDelegateName() {
        return getToolWrapper().getShortName();
    }

    protected static String getTextAttributeKey(
        @Nonnull Project project,
        @Nonnull HighlightSeverity severity,
        @Nonnull ProblemHighlightType highlightType
    ) {
        HighlightInfoType type = ProblemHighlightTypeInspectionRuler.REGISTRY.get(highlightType);
        if (type != null) {
            return type.getAttributesKey().getExternalName();
        }

        if (highlightType == ProblemHighlightType.LIKE_UNKNOWN_SYMBOL && severity == HighlightSeverity.ERROR) {
            return HighlightInfoType.WRONG_REF.getAttributesKey().getExternalName();
        }

        SeverityRegistrar registrar = SeverityRegistrar.getSeverityRegistrar(project);
        return registrar.getHighlightInfoTypeBySeverity(severity).getAttributesKey().getExternalName();
    }

    @Nonnull
    public InspectionToolWrapper getToolWrapper() {
        return myToolWrapper;
    }

    @Nonnull
    public RefManager getRefManager() {
        return getContext().getRefManager();
    }

    @Nonnull
    @Override
    public GlobalInspectionContextBase getContext() {
        return myContext;
    }

    @Override
    public void exportResults(@Nonnull final Element parentNode) {
        getRefManager().iterate(new RefVisitor() {
            @Override
            @RequiredReadAction
            public void visitElement(@Nonnull RefEntity elem) {
                exportResults(parentNode, elem);
            }
        });
    }

    @Override
    public boolean isOldProblemsIncluded() {
        GlobalInspectionContextImpl context = (GlobalInspectionContextImpl) getContext();
        return context.getUIOptions().SHOW_DIFF_WITH_PREVIOUS_RUN && getOldContent() != null;
    }

    @Override
    @RequiredReadAction
    public void addProblemElement(RefEntity refElement, @Nonnull CommonProblemDescriptor... descriptions) {
        addProblemElement(refElement, true, descriptions);
    }

    @Override
    @RequiredReadAction
    public void addProblemElement(RefEntity refElement, boolean filterSuppressed, @Nonnull CommonProblemDescriptor... descriptors) {
        if (refElement == null) {
            return;
        }
        if (descriptors.length == 0) {
            return;
        }
        if (filterSuppressed) {
            if (ourOutputPath == null || !(myToolWrapper instanceof LocalInspectionToolWrapper)) {
                synchronized (lock) {
                    Map<RefEntity, CommonProblemDescriptor[]> problemElements = getProblemElements();
                    CommonProblemDescriptor[] problems = problemElements.get(refElement);
                    problems = problems == null ? descriptors : ArrayUtil.mergeArrays(
                        problems,
                        descriptors,
                        CommonProblemDescriptor.ARRAY_FACTORY
                    );
                    problemElements.put(refElement, problems);
                }
                for (CommonProblemDescriptor description : descriptors) {
                    getProblemToElements().put(description, refElement);
                    collectQuickFixes(description.getFixes(), refElement);
                }
            }
            else {
                writeOutput(descriptors, refElement);
            }
        }
        else { //just need to collect problems
            for (CommonProblemDescriptor descriptor : descriptors) {
                getProblemToElements().put(descriptor, refElement);
            }
        }

        GlobalInspectionContextImpl context = (GlobalInspectionContextImpl) getContext();
        if (myToolWrapper instanceof LocalInspectionToolWrapper) {
            InspectionResultsView view = context.getView();
            if (view == null || !(refElement instanceof RefElement)) {
                return;
            }
            InspectionNode toolNode = myToolNode;
            if (toolNode == null) {
                HighlightSeverity currentSeverity = getSeverity((RefElement) refElement);
                view.addTool(myToolWrapper, HighlightDisplayLevel.find(currentSeverity), context.getUIOptions().GROUP_BY_SEVERITY);
            }
            else if (toolNode.isTooBigForOnlineRefresh()) {
                return;
            }
            Map<RefEntity, CommonProblemDescriptor[]> problems = new HashMap<>();
            problems.put(refElement, descriptors);
            Map<String, Set<RefEntity>> contents = new HashMap<>();
            String groupName = refElement.getRefManager().getGroupName((RefElement) refElement);
            Set<RefEntity> content = contents.get(groupName);
            if (content == null) {
                content = new HashSet<>();
                contents.put(groupName, content);
            }
            content.add(refElement);

            UIUtil.invokeLaterIfNeeded(() -> {
                if (!isDisposed()) {
                    view.getProvider().appendToolNodeContent(
                        context,
                        myToolNode,
                        (InspectionTreeNode) myToolNode.getParent(),
                        context.getUIOptions().SHOW_STRUCTURE,
                        contents,
                        problems,
                        (DefaultTreeModel) view.getTree().getModel()
                    );
                    context.addView(view);
                }
            });

        }
    }

    protected boolean isDisposed() {
        return isDisposed;
    }

    @RequiredReadAction
    private void writeOutput(@Nonnull CommonProblemDescriptor[] descriptions, @Nonnull RefEntity refElement) {
        Element parentNode = new Element(InspectionLocalize.inspectionProblems().get());
        exportResults(descriptions, refElement, parentNode);
        List list = parentNode.getChildren();

        String ext = ".xml";
        String fileName = ourOutputPath + File.separator + myToolWrapper.getShortName() + ext;
        PathMacroManager pathMacroManager = ProjectPathMacroManager.getInstance(getContext().getProject());
        PrintWriter printWriter = null;
        try {
            new File(ourOutputPath).mkdirs();
            File file = new File(fileName);
            CharArrayWriter writer = new CharArrayWriter();
            if (!file.exists()) {
                writer.append("<").append(InspectionLocalize.inspectionProblems().get())
                    .append(" " + GlobalInspectionContextImpl.LOCAL_TOOL_ATTRIBUTE + "=\"")
                    .append(Boolean.toString(myToolWrapper instanceof LocalInspectionToolWrapper)).append("\">\n");
            }
            for (Object o : list) {
                Element element = (Element) o;
                pathMacroManager.collapsePaths(element);
                JDOMUtil.writeElement(element, writer, "\n");
            }
            printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName, true), "UTF-8")));
            printWriter.append("\n");
            printWriter.append(writer.toString());
        }
        catch (IOException e) {
            LOG.error(e);
        }
        finally {
            if (printWriter != null) {
                printWriter.close();
            }
        }
    }

    @Nonnull
    @Override
    public Collection<CommonProblemDescriptor> getProblemDescriptors() {
        return getProblemToElements().keySet();
    }

    private void collectQuickFixes(QuickFix[] fixes, @Nonnull RefEntity refEntity) {
        if (fixes != null && fixes.length != 0) {
            Set<QuickFix> localQuickFixes = getQuickFixActions().get(refEntity);
            if (localQuickFixes == null) {
                localQuickFixes = new HashSet<>();
                getQuickFixActions().put(refEntity, localQuickFixes);
            }
            ContainerUtil.addAll(localQuickFixes, fixes);
        }
    }

    @Override
    public void ignoreElement(@Nonnull RefEntity refEntity) {
        getProblemElements().remove(refEntity);
        getQuickFixActions().remove(refEntity);
    }

    @Override
    public void ignoreCurrentElement(RefEntity refEntity) {
        if (refEntity == null) {
            return;
        }
        getIgnoredElements().put(refEntity, getProblemElements().get(refEntity));
    }

    @Override
    public void amnesty(RefEntity refEntity) {
        getIgnoredElements().remove(refEntity);
    }

    @Override
    public void ignoreProblem(RefEntity refEntity, CommonProblemDescriptor problem, int idx) {
        if (refEntity == null) {
            return;
        }
        Set<QuickFix> localQuickFixes = getQuickFixActions().get(refEntity);
        QuickFix[] fixes = problem.getFixes();
        if (isIgnoreProblem(fixes, localQuickFixes, idx)) {
            getProblemToElements().remove(problem);
            Map<RefEntity, CommonProblemDescriptor[]> problemElements = getProblemElements();
            synchronized (lock) {
                CommonProblemDescriptor[] descriptors = problemElements.get(refEntity);
                if (descriptors != null) {
                    ArrayList<CommonProblemDescriptor> newDescriptors = new ArrayList<>(Arrays.asList(descriptors));
                    newDescriptors.remove(problem);
                    getQuickFixActions().put(refEntity, null);
                    if (!newDescriptors.isEmpty()) {
                        problemElements.put(refEntity, newDescriptors.toArray(new CommonProblemDescriptor[newDescriptors.size()]));
                        for (CommonProblemDescriptor descriptor : newDescriptors) {
                            collectQuickFixes(descriptor.getFixes(), refEntity);
                        }
                    }
                    else {
                        ignoreProblemElement(refEntity);
                    }
                }
            }
        }
    }

    private void ignoreProblemElement(RefEntity refEntity) {
        CommonProblemDescriptor[] problemDescriptors = getProblemElements().remove(refEntity);
        getIgnoredElements().put(refEntity, problemDescriptors);
    }

    @Override
    public void ignoreCurrentElementProblem(RefEntity refEntity, CommonProblemDescriptor descriptor) {
        CommonProblemDescriptor[] descriptors = getIgnoredElements().get(refEntity);
        if (descriptors == null) {
            descriptors = new CommonProblemDescriptor[0];
        }
        getIgnoredElements().put(refEntity, ArrayUtil.append(descriptors, descriptor));
    }

    private static boolean isIgnoreProblem(QuickFix[] problemFixes, Set<QuickFix> fixes, int idx) {
        if (problemFixes == null || fixes == null) {
            return true;
        }
        if (problemFixes.length <= idx) {
            return true;
        }
        for (QuickFix fix : problemFixes) {
            if (fix != problemFixes[idx] && !fixes.contains(fix)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void cleanup() {
        myOldProblemElements = null;

        synchronized (lock) {
            myProblemElements = null;
            myProblemToElements = null;
            myQuickFixActions = null;
            myIgnoredElements = null;
        }

        myContents = null;
        myModulesProblems = null;
        isDisposed = true;
    }

    @Override
    public void finalCleanup() {
        myOldProblemElements = null;
        cleanup();
    }

    @Override
    @Nullable
    public CommonProblemDescriptor[] getDescriptions(@Nonnull RefEntity refEntity) {
        CommonProblemDescriptor[] problems = getProblemElements().get(refEntity);
        if (problems == null) {
            return null;
        }

        if (!refEntity.isValid()) {
            ignoreElement(refEntity);
            return null;
        }

        return problems;
    }

    @Nonnull
    @Override
    public HTMLComposerBase getComposer() {
        if (myComposer == null) {
            myComposer = new DescriptorComposer(this);
        }
        return myComposer;
    }

    @Override
    @RequiredReadAction
    public void exportResults(@Nonnull Element parentNode, @Nonnull RefEntity refEntity) {
        synchronized (lock) {
            if (getProblemElements().containsKey(refEntity)) {
                CommonProblemDescriptor[] descriptions = getDescriptions(refEntity);
                if (descriptions != null) {
                    exportResults(descriptions, refEntity, parentNode);
                }
            }
        }
    }

    @RequiredReadAction
    private void exportResults(@Nonnull CommonProblemDescriptor[] descriptors, @Nonnull RefEntity refEntity, @Nonnull Element parentNode) {
        for (CommonProblemDescriptor descriptor : descriptors) {
            LocalizeValue template = descriptor.getDescriptionTemplate();
            int line = descriptor instanceof ProblemDescriptor problemDescriptor ? problemDescriptor.getLineNumber() : -1;
            PsiElement psiElement = descriptor instanceof ProblemDescriptor problemDescriptor ? problemDescriptor.getPsiElement() : null;
            String problemText = StringUtil.replace(
                StringUtil.replace(
                    template.get(),
                    "#ref",
                    psiElement != null ? ProblemDescriptorUtil.extractHighlightedText(descriptor, psiElement) : ""
                ),
                " #loc ",
                " "
            );

            Element element = refEntity.getRefManager().export(refEntity, parentNode, line);
            if (element == null) {
                return;
            }
            Element problemClassElement = new Element(InspectionLocalize.inspectionExportResultsProblemElementTag().get());
            problemClassElement.addContent(myToolWrapper.getDisplayName().get());
            if (refEntity instanceof RefElement refElement) {
                HighlightSeverity severity = getSeverity(refElement);
                ProblemHighlightType problemHighlightType = descriptor instanceof ProblemDescriptor problemDescriptor
                    ? problemDescriptor.getHighlightType()
                    : ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
                String attributeKey = getTextAttributeKey(refElement.getRefManager().getProject(), severity, problemHighlightType);
                problemClassElement.setAttribute("severity", severity.myName);
                problemClassElement.setAttribute("attribute_key", attributeKey);
            }
            element.addContent(problemClassElement);
            if (myToolWrapper instanceof GlobalInspectionToolWrapper) {
                GlobalInspectionTool globalInspectionTool = ((GlobalInspectionToolWrapper) myToolWrapper).getTool();
                QuickFix[] fixes = descriptor.getFixes();
                if (fixes != null) {
                    Element hintsElement = new Element("hints");
                    for (QuickFix fix : fixes) {
                        String hint = globalInspectionTool.getHint(fix);
                        if (hint != null) {
                            Element hintElement = new Element("hint");
                            hintElement.setAttribute("value", hint);
                            hintsElement.addContent(hintElement);
                        }
                    }
                    element.addContent(hintsElement);
                }
            }
            try {
                Element descriptionElement = new Element(InspectionLocalize.inspectionExportResultsDescriptionTag().get());
                descriptionElement.addContent(problemText);
                element.addContent(descriptionElement);
            }
            catch (IllegalDataException e) {
                //noinspection HardCodedStringLiteral,UseOfSystemOutOrSystemErr
                System.out.println(
                    "Cannot save results for " + refEntity.getName() + ", inspection which caused problem: " + myToolWrapper.getShortName()
                );
            }
        }
    }

    @Override
    public boolean isGraphNeeded() {
        return false;
    }

    @Override
    @RequiredReadAction
    public boolean hasReportedProblems() {
        GlobalInspectionContextImpl context = (GlobalInspectionContextImpl) getContext();
        if (!isDisposed() && context.getUIOptions().SHOW_ONLY_DIFF) {
            for (CommonProblemDescriptor descriptor : getProblemToElements().keySet()) {
                if (getProblemStatus(descriptor) != FileStatus.NOT_CHANGED) {
                    return true;
                }
            }
            if (myOldProblemElements != null) {
                for (RefEntity entity : myOldProblemElements.keySet()) {
                    if (getElementStatus(entity) != FileStatus.NOT_CHANGED) {
                        return true;
                    }
                }
            }
            return false;
        }
        return !getProblemElements().isEmpty()
            || !isDisposed() && context.getUIOptions().SHOW_DIFF_WITH_PREVIOUS_RUN && myOldProblemElements != null && !myOldProblemElements.isEmpty();
    }

    @Override
    public void updateContent() {
        myContents = new HashMap<>();
        myModulesProblems = new HashSet<>();
        Set<RefEntity> elements = getProblemElements().keySet();
        GlobalInspectionContextImpl context = (GlobalInspectionContextImpl) getContext();
        for (RefEntity element : elements) {
            if (context.getUIOptions().FILTER_RESOLVED_ITEMS && getIgnoredElements().containsKey(element)) {
                continue;
            }
            if (element instanceof RefModule) {
                myModulesProblems.add((RefModule) element);
            }
            else {
                String groupName = element instanceof RefElement ? element.getRefManager().getGroupName((RefElement) element) : null;
                Set<RefEntity> content = myContents.get(groupName);
                if (content == null) {
                    content = new HashSet<>();
                    myContents.put(groupName, content);
                }
                content.add(element);
            }
        }
    }

    @Override
    public Map<String, Set<RefEntity>> getContent() {
        return myContents;
    }

    @Override
    public Map<String, Set<RefEntity>> getOldContent() {
        if (myOldProblemElements == null) {
            return null;
        }
        HashMap<String, Set<RefEntity>>
            oldContents = new HashMap<>();
        Set<RefEntity> elements = myOldProblemElements.keySet();
        for (RefEntity element : elements) {
            String groupName =
                element instanceof RefElement ? element.getRefManager().getGroupName((RefElement) element) : element.getName();
            Set<RefEntity> collection = myContents.get(groupName);
            if (collection != null) {
                Set<RefEntity> currentElements = new HashSet<>(collection);
                if (RefUtil.contains(element, currentElements)) {
                    continue;
                }
            }
            Set<RefEntity> oldContent = oldContents.get(groupName);
            if (oldContent == null) {
                oldContent = new HashSet<>();
                oldContents.put(groupName, oldContent);
            }
            oldContent.add(element);
        }
        return oldContents;
    }

    @Override
    public Set<RefModule> getModuleProblems() {
        return myModulesProblems;
    }

    @Override
    @Nullable
    public QuickFixAction[] getQuickFixes(@Nonnull RefEntity[] refElements) {
        return extractActiveFixes(refElements, getQuickFixActions());
    }

    @Override
    @Nullable
    public QuickFixAction[] extractActiveFixes(@Nonnull RefEntity[] refElements, @Nonnull Map<RefEntity, Set<QuickFix>> actions) {
        Map<Class, QuickFixAction> result = new HashMap<>();
        for (RefEntity refElement : refElements) {
            Set<QuickFix> localQuickFixes = actions.get(refElement);
            if (localQuickFixes == null) {
                continue;
            }
            for (QuickFix fix : localQuickFixes) {
                if (fix == null) {
                    continue;
                }
                Class klass = fix instanceof ActionClassHolder actionClassHolder ? actionClassHolder.getActionClass() : fix.getClass();
                QuickFixAction quickFixAction = result.get(klass);
                if (quickFixAction != null) {
                    ((LocalQuickFixWrapper) quickFixAction).setText(InspectionLocalize.inspectionDescriptorProviderApplyFix(""));
                }
                else {
                    LocalQuickFixWrapper quickFixWrapper = new LocalQuickFixWrapper(fix, myToolWrapper);
                    result.put(klass, quickFixWrapper);
                }
            }
        }
        return result.values().isEmpty() ? null : result.values().toArray(new QuickFixAction[result.size()]);
    }

    @Override
    public RefEntity getElement(@Nonnull CommonProblemDescriptor descriptor) {
        return getProblemToElements().get(descriptor);
    }

    @Override
    public void ignoreProblem(@Nonnull CommonProblemDescriptor descriptor, @Nonnull QuickFix fix) {
        RefEntity refElement = getProblemToElements().get(descriptor);
        if (refElement != null) {
            QuickFix[] fixes = descriptor.getFixes();
            for (int i = 0; i < fixes.length; i++) {
                if (fixes[i] == fix) {
                    ignoreProblem(refElement, descriptor, i);
                    return;
                }
            }
        }
    }

    @Override
    public boolean isElementIgnored(RefEntity element) {
        for (RefEntity entity : getIgnoredElements().keySet()) {
            if (Comparing.equal(entity, element)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isProblemResolved(RefEntity refEntity, CommonProblemDescriptor descriptor) {
        if (descriptor == null) {
            return true;
        }
        for (RefEntity entity : getIgnoredElements().keySet()) {
            if (Comparing.equal(entity, refEntity)) {
                CommonProblemDescriptor[] descriptors = getIgnoredElements().get(refEntity);
                return ArrayUtil.contains(descriptor, descriptors);
            }
        }
        return false;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public FileStatus getProblemStatus(@Nonnull CommonProblemDescriptor descriptor) {
        GlobalInspectionContextImpl context = (GlobalInspectionContextImpl) getContext();
        if (!isDisposed() && context.getUIOptions().SHOW_DIFF_WITH_PREVIOUS_RUN) {
            if (myOldProblemElements != null) {
                Set<CommonProblemDescriptor> allAvailable = new HashSet<>();
                for (CommonProblemDescriptor[] descriptors : myOldProblemElements.values()) {
                    if (descriptors != null) {
                        ContainerUtil.addAll(allAvailable, descriptors);
                    }
                }
                boolean old = containsDescriptor(descriptor, allAvailable);
                boolean current = containsDescriptor(descriptor, getProblemToElements().keySet());
                return calcStatus(old, current);
            }
        }
        return FileStatus.NOT_CHANGED;
    }

    @RequiredReadAction
    private static boolean containsDescriptor(
        @Nonnull CommonProblemDescriptor descriptor,
        Collection<CommonProblemDescriptor> descriptors
    ) {
        PsiElement element = null;
        if (descriptor instanceof ProblemDescriptor problemDescriptor) {
            element = problemDescriptor.getPsiElement();
        }
        for (CommonProblemDescriptor commonProblemDescriptor : descriptors) {
            if (commonProblemDescriptor instanceof ProblemDescriptor problemDescriptor) {
                if (!Objects.equals(element, problemDescriptor.getPsiElement())) {
                    continue;
                }
            }
            if (commonProblemDescriptor.getDescriptionTemplate().equals(descriptor.getDescriptionTemplate())) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public FileStatus getElementStatus(RefEntity element) {
        GlobalInspectionContextImpl context = (GlobalInspectionContextImpl) getContext();
        if (!isDisposed() && context.getUIOptions().SHOW_DIFF_WITH_PREVIOUS_RUN) {
            if (myOldProblemElements != null) {
                boolean old = RefUtil.contains(element, myOldProblemElements.keySet());
                boolean current = RefUtil.contains(element, getProblemElements().keySet());
                return calcStatus(old, current);
            }
        }
        return FileStatus.NOT_CHANGED;
    }

    @Nonnull
    @Override
    public Collection<RefEntity> getIgnoredRefElements() {
        return getIgnoredElements().keySet();
    }

    @Override
    @Nonnull
    public Map<RefEntity, CommonProblemDescriptor[]> getProblemElements() {
        synchronized (lock) {
            if (myProblemElements == null) {
                myProblemElements = Collections.synchronizedMap(new HashMap<RefEntity, CommonProblemDescriptor[]>());
            }
            return myProblemElements;
        }
    }

    @Override
    @Nullable
    public Map<RefEntity, CommonProblemDescriptor[]> getOldProblemElements() {
        return myOldProblemElements;
    }

    @Nonnull
    private Map<CommonProblemDescriptor, RefEntity> getProblemToElements() {
        synchronized (lock) {
            if (myProblemToElements == null) {
                myProblemToElements = Collections.synchronizedMap(new HashMap<CommonProblemDescriptor, RefEntity>());
            }
            return myProblemToElements;
        }
    }

    @Nonnull
    private Map<RefEntity, Set<QuickFix>> getQuickFixActions() {
        synchronized (lock) {
            if (myQuickFixActions == null) {
                myQuickFixActions = Collections.synchronizedMap(new HashMap<RefEntity, Set<QuickFix>>());
            }
            return myQuickFixActions;
        }
    }

    @Nonnull
    private Map<RefEntity, CommonProblemDescriptor[]> getIgnoredElements() {
        synchronized (lock) {
            if (myIgnoredElements == null) {
                myIgnoredElements = Collections.synchronizedMap(new HashMap<RefEntity, CommonProblemDescriptor[]>());
            }
            return myIgnoredElements;
        }
    }

    @Nonnull
    @Override
    public InspectionNode createToolNode(
        @Nonnull GlobalInspectionContextImpl globalInspectionContext,
        @Nonnull InspectionNode node,
        @Nonnull InspectionRVContentProvider provider,
        @Nonnull InspectionTreeNode parentNode,
        boolean showStructure
    ) {
        return node;
    }

    @Override
    @Nullable
    @RequiredReadAction
    public IntentionAction findQuickFixes(@Nonnull final CommonProblemDescriptor commonProblemDescriptor, String hint) {
        InspectionTool tool = getToolWrapper().getTool();
        if (!(tool instanceof GlobalInspectionTool globalInspectionTool)) {
            return null;
        }
        final QuickFix fix = globalInspectionTool.getQuickFix(hint);
        if (fix == null) {
            return null;
        }
        if (commonProblemDescriptor instanceof ProblemDescriptor problemDescriptor) {
            ProblemDescriptor descriptor = new ProblemDescriptorImpl(
                problemDescriptor.getStartElement(),
                problemDescriptor.getEndElement(),
                commonProblemDescriptor.getDescriptionTemplate(),
                new LocalQuickFix[]{(LocalQuickFix) fix},
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                false,
                null,
                false
            );
            return QuickFixWrapper.wrap(descriptor, 0);
        }
        return new IntentionAction() {
            @Override
            @Nonnull
            public LocalizeValue getText() {
                return fix.getName();
            }

            @Override
            public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
                fix.applyFix(project, commonProblemDescriptor); //todo check type consistency
            }

            @Override
            public boolean startInWriteAction() {
                return true;
            }
        };
    }

    public static void setOutputPath(String output) {
        ourOutputPath = output;
    }
}
