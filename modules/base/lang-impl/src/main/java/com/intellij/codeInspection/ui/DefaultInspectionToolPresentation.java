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
package com.intellij.codeInspection.ui;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.*;
import com.intellij.codeInspection.reference.*;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.profile.codeInspection.InspectionProjectProfileManagerImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import consulo.logging.Logger;
import org.jdom.Element;
import org.jdom.IllegalDataException;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    final int descriptionEnd = description.indexOf("<!-- tooltip end -->");
    if (descriptionEnd < 0) {
      final Pattern pattern = Pattern.compile(".*Use.*(the (panel|checkbox|checkboxes|field|button|controls).*below).*", Pattern.DOTALL);
      final Matcher matcher = pattern.matcher(description);
      int startFindIdx = 0;
      while (matcher.find(startFindIdx)) {
        final int end = matcher.end(1);
        startFindIdx = end;
        description = description.substring(0, matcher.start(1)) + " inspection settings " + description.substring(end);
      }
    } else {
      description = description.substring(0, descriptionEnd);
    }
    return description;
  }

  protected HighlightSeverity getSeverity(@Nonnull RefElement element) {
    final PsiElement psiElement = element.getPointer().getContainingFile();
    if (psiElement != null) {
      final GlobalInspectionContextImpl context = getContext();
      final String shortName = getSeverityDelegateName();
      final Tools tools = context.getTools().get(shortName);
      if (tools != null) {
        for (ScopeToolState state : tools.getTools()) {
          InspectionToolWrapper toolWrapper = state.getTool();
          if (toolWrapper == getToolWrapper()) {
            return context.getCurrentProfile().getErrorLevel(HighlightDisplayKey.find(shortName), psiElement).getSeverity();
          }
        }
      }

      final InspectionProfile profile = InspectionProjectProfileManager.getInstance(context.getProject()).getInspectionProfile();
      final HighlightDisplayLevel level = profile.getErrorLevel(HighlightDisplayKey.find(shortName), psiElement);
      return level.getSeverity();
    }
    return null;
  }

  protected String getSeverityDelegateName() {
    return getToolWrapper().getShortName();
  }

  protected static String getTextAttributeKey(@Nonnull Project project,
                                              @Nonnull HighlightSeverity severity,
                                              @Nonnull ProblemHighlightType highlightType) {
    if (highlightType == ProblemHighlightType.LIKE_DEPRECATED) {
      return HighlightInfoType.DEPRECATED.getAttributesKey().getExternalName();
    }
    if (highlightType == ProblemHighlightType.LIKE_UNKNOWN_SYMBOL && severity == HighlightSeverity.ERROR) {
      return HighlightInfoType.WRONG_REF.getAttributesKey().getExternalName();
    }
    if (highlightType == ProblemHighlightType.LIKE_UNUSED_SYMBOL) {
      return HighlightInfoType.UNUSED_SYMBOL.getAttributesKey().getExternalName();
    }
    SeverityRegistrar registrar = InspectionProjectProfileManagerImpl.getInstanceImpl(project).getSeverityRegistrar();
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
  public GlobalInspectionContextImpl getContext() {
    return myContext;
  }

  @Override
  public void exportResults(@Nonnull final Element parentNode) {
    getRefManager().iterate(new RefVisitor(){
      @Override
      public void visitElement(@Nonnull RefEntity elem) {
        exportResults(parentNode, elem);
      }
    });
  }

  @Override
  public boolean isOldProblemsIncluded() {
    final GlobalInspectionContextImpl context = getContext();
    return context.getUIOptions().SHOW_DIFF_WITH_PREVIOUS_RUN && getOldContent() != null;
  }


  @Override
  public void addProblemElement(RefEntity refElement, @Nonnull CommonProblemDescriptor... descriptions){
    addProblemElement(refElement, true, descriptions);
  }

  @Override
  public void addProblemElement(RefEntity refElement, boolean filterSuppressed, @Nonnull CommonProblemDescriptor... descriptors) {
    if (refElement == null) return;
    if (descriptors.length == 0) return;
    if (filterSuppressed) {
      if (ourOutputPath == null || !(myToolWrapper instanceof LocalInspectionToolWrapper)) {
        synchronized (lock) {
          Map<RefEntity, CommonProblemDescriptor[]> problemElements = getProblemElements();
          CommonProblemDescriptor[] problems = problemElements.get(refElement);
          problems = problems == null ? descriptors : ArrayUtil.mergeArrays(problems, descriptors, CommonProblemDescriptor.ARRAY_FACTORY);
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

    final GlobalInspectionContextImpl context = getContext();
    if (myToolWrapper instanceof LocalInspectionToolWrapper) {
      final InspectionResultsView view = context.getView();
      if (view == null || !(refElement instanceof RefElement)) {
        return;
      }
      InspectionNode toolNode = myToolNode;
      if (toolNode == null) {
        final HighlightSeverity currentSeverity = getSeverity((RefElement)refElement);
        view.addTool(myToolWrapper, HighlightDisplayLevel.find(currentSeverity), context.getUIOptions().GROUP_BY_SEVERITY);
      }
      else if (toolNode.isTooBigForOnlineRefresh()) {
        return;
      }
      final Map<RefEntity, CommonProblemDescriptor[]> problems = new HashMap<RefEntity, CommonProblemDescriptor[]>();
      problems.put(refElement, descriptors);
      final Map<String, Set<RefEntity>> contents = new HashMap<String, Set<RefEntity>>();
      final String groupName = refElement.getRefManager().getGroupName((RefElement)refElement);
      Set<RefEntity> content = contents.get(groupName);
      if (content == null) {
        content = new HashSet<RefEntity>();
        contents.put(groupName, content);
      }
      content.add(refElement);

      UIUtil.invokeLaterIfNeeded(new Runnable() {
        @Override
        public void run() {
          if (!isDisposed()) {
            view.getProvider().appendToolNodeContent(context, myToolNode,
                                                     (InspectionTreeNode)myToolNode.getParent(), context.getUIOptions().SHOW_STRUCTURE,
                                                     contents, problems, (DefaultTreeModel)view.getTree().getModel());
            context.addView(view);
          }
        }
      });

    }
  }

  protected boolean isDisposed() {
    return isDisposed;
  }

  private void writeOutput(@Nonnull final CommonProblemDescriptor[] descriptions, @Nonnull RefEntity refElement) {
    final Element parentNode = new Element(InspectionsBundle.message("inspection.problems"));
    exportResults(descriptions, refElement, parentNode);
    final List list = parentNode.getChildren();

    @NonNls final String ext = ".xml";
    final String fileName = ourOutputPath + File.separator + myToolWrapper.getShortName() + ext;
    final PathMacroManager pathMacroManager = PathMacroManager.getInstance(getContext().getProject());
    PrintWriter printWriter = null;
    try {
      new File(ourOutputPath).mkdirs();
      final File file = new File(fileName);
      final CharArrayWriter writer = new CharArrayWriter();
      if (!file.exists()) {
        writer.append("<").append(InspectionsBundle.message("inspection.problems")).append(" " + GlobalInspectionContextImpl.LOCAL_TOOL_ATTRIBUTE + "=\"")
                .append(Boolean.toString(myToolWrapper instanceof LocalInspectionToolWrapper)).append("\">\n");
      }
      for (Object o : list) {
        final Element element = (Element)o;
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

  @Override
  @Nonnull
  public Collection<CommonProblemDescriptor> getProblemDescriptors() {
    return getProblemToElements().keySet();
  }

  private void collectQuickFixes(final QuickFix[] fixes, @Nonnull RefEntity refEntity) {
    if (fixes != null && fixes.length != 0) {
      Set<QuickFix> localQuickFixes = getQuickFixActions().get(refEntity);
      if (localQuickFixes == null) {
        localQuickFixes = new HashSet<QuickFix>();
        getQuickFixActions().put(refEntity, localQuickFixes);
      }
      ContainerUtil.addAll(localQuickFixes, fixes);
    }
  }

  @Override
  public void ignoreElement(@Nonnull final RefEntity refEntity) {
    getProblemElements().remove(refEntity);
    getQuickFixActions().remove(refEntity);
  }

  @Override
  public void ignoreCurrentElement(RefEntity refEntity) {
    if (refEntity == null) return;
    getIgnoredElements().put(refEntity, getProblemElements().get(refEntity));
  }

  @Override
  public void amnesty(RefEntity refEntity) {
    getIgnoredElements().remove(refEntity);
  }

  @Override
  public void ignoreProblem(RefEntity refEntity, CommonProblemDescriptor problem, int idx) {
    if (refEntity == null) return;
    final Set<QuickFix> localQuickFixes = getQuickFixActions().get(refEntity);
    final QuickFix[] fixes = problem.getFixes();
    if (isIgnoreProblem(fixes, localQuickFixes, idx)){
      getProblemToElements().remove(problem);
      Map<RefEntity, CommonProblemDescriptor[]> problemElements = getProblemElements();
      synchronized (lock) {
        CommonProblemDescriptor[] descriptors = problemElements.get(refEntity);
        if (descriptors != null) {
          ArrayList<CommonProblemDescriptor> newDescriptors = new ArrayList<CommonProblemDescriptor>(Arrays.asList(descriptors));
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

  private void ignoreProblemElement(RefEntity refEntity){
    final CommonProblemDescriptor[] problemDescriptors = getProblemElements().remove(refEntity);
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

  private static boolean isIgnoreProblem(QuickFix[] problemFixes, Set<QuickFix> fixes, int idx){
    if (problemFixes == null || fixes == null) {
      return true;
    }
    if (problemFixes.length <= idx){
      return true;
    }
    for (QuickFix fix : problemFixes) {
      if (fix != problemFixes[idx] && !fixes.contains(fix)){
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
    final CommonProblemDescriptor[] problems = getProblemElements().get(refEntity);
    if (problems == null) return null;

    if (!refEntity.isValid()) {
      ignoreElement(refEntity);
      return null;
    }

    return problems;
  }

  @Nonnull
  @Override
  public HTMLComposerImpl getComposer() {
    if (myComposer == null) {
      myComposer = new DescriptorComposer(this);
    }
    return myComposer;
  }

  @Override
  public void exportResults(@Nonnull final Element parentNode, @Nonnull RefEntity refEntity) {
    synchronized (lock) {
      if (getProblemElements().containsKey(refEntity)) {
        CommonProblemDescriptor[] descriptions = getDescriptions(refEntity);
        if (descriptions != null) {
          exportResults(descriptions, refEntity, parentNode);
        }
      }
    }
  }

  private void exportResults(@Nonnull final CommonProblemDescriptor[] descriptors, @Nonnull RefEntity refEntity, @Nonnull Element parentNode) {
    for (CommonProblemDescriptor descriptor : descriptors) {
      @NonNls final String template = descriptor.getDescriptionTemplate();
      int line = descriptor instanceof ProblemDescriptor ? ((ProblemDescriptor)descriptor).getLineNumber() : -1;
      final PsiElement psiElement = descriptor instanceof ProblemDescriptor ? ((ProblemDescriptor)descriptor).getPsiElement() : null;
      @NonNls String problemText = StringUtil.replace(StringUtil.replace(template, "#ref", psiElement != null ? ProblemDescriptorUtil
              .extractHighlightedText(descriptor, psiElement) : ""), " #loc ", " ");

      Element element = refEntity.getRefManager().export(refEntity, parentNode, line);
      if (element == null) return;
      @NonNls Element problemClassElement = new Element(InspectionsBundle.message("inspection.export.results.problem.element.tag"));
      problemClassElement.addContent(myToolWrapper.getDisplayName());
      if (refEntity instanceof RefElement){
        final RefElement refElement = (RefElement)refEntity;
        final HighlightSeverity severity = getSeverity(refElement);
        ProblemHighlightType problemHighlightType = descriptor instanceof ProblemDescriptor
                                                    ? ((ProblemDescriptor)descriptor).getHighlightType()
                                                    : ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
        final String attributeKey = getTextAttributeKey(refElement.getRefManager().getProject(), severity, problemHighlightType);
        problemClassElement.setAttribute("severity", severity.myName);
        problemClassElement.setAttribute("attribute_key", attributeKey);
      }
      element.addContent(problemClassElement);
      if (myToolWrapper instanceof GlobalInspectionToolWrapper) {
        final GlobalInspectionTool globalInspectionTool = ((GlobalInspectionToolWrapper)myToolWrapper).getTool();
        final QuickFix[] fixes = descriptor.getFixes();
        if (fixes != null) {
          @NonNls Element hintsElement = new Element("hints");
          for (QuickFix fix : fixes) {
            final String hint = globalInspectionTool.getHint(fix);
            if (hint != null) {
              @NonNls Element hintElement = new Element("hint");
              hintElement.setAttribute("value", hint);
              hintsElement.addContent(hintElement);
            }
          }
          element.addContent(hintsElement);
        }
      }
      try {
        Element descriptionElement = new Element(InspectionsBundle.message("inspection.export.results.description.tag"));
        descriptionElement.addContent(problemText);
        element.addContent(descriptionElement);
      }
      catch (IllegalDataException e) {
        //noinspection HardCodedStringLiteral,UseOfSystemOutOrSystemErr
        System.out.println("Cannot save results for " + refEntity.getName() + ", inspection which caused problem: " + myToolWrapper.getShortName());
      }
    }
  }

  @Override
  public boolean isGraphNeeded() {
    return false;
  }

  @Override
  public boolean hasReportedProblems() {
    final GlobalInspectionContextImpl context = getContext();
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
    if (!getProblemElements().isEmpty()) return true;
    return !isDisposed() && context.getUIOptions().SHOW_DIFF_WITH_PREVIOUS_RUN && myOldProblemElements != null && !myOldProblemElements.isEmpty();
  }

  @Override
  public void updateContent() {
    myContents = new HashMap<String, Set<RefEntity>>();
    myModulesProblems = new HashSet<RefModule>();
    final Set<RefEntity> elements = getProblemElements().keySet();
    for (RefEntity element : elements) {
      if (getContext().getUIOptions().FILTER_RESOLVED_ITEMS && getIgnoredElements().containsKey(element)) continue;
      if (element instanceof RefModule) {
        myModulesProblems.add((RefModule)element);
      }
      else {
        String groupName = element instanceof RefElement ? element.getRefManager().getGroupName((RefElement)element) : null;
        Set<RefEntity> content = myContents.get(groupName);
        if (content == null) {
          content = new HashSet<RefEntity>();
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
    if (myOldProblemElements == null) return null;
    final HashMap<String, Set<RefEntity>>
            oldContents = new HashMap<String, Set<RefEntity>>();
    final Set<RefEntity> elements = myOldProblemElements.keySet();
    for (RefEntity element : elements) {
      String groupName = element instanceof RefElement ? element.getRefManager().getGroupName((RefElement)element) : element.getName();
      final Set<RefEntity> collection = myContents.get(groupName);
      if (collection != null) {
        final Set<RefEntity> currentElements = new HashSet<RefEntity>(collection);
        if (RefUtil.contains(element, currentElements)) continue;
      }
      Set<RefEntity> oldContent = oldContents.get(groupName);
      if (oldContent == null) {
        oldContent = new HashSet<RefEntity>();
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
  public QuickFixAction[] getQuickFixes(@Nonnull final RefEntity[] refElements) {
    return extractActiveFixes(refElements, getQuickFixActions());
  }

  @Override
  @Nullable
  public QuickFixAction[] extractActiveFixes(@Nonnull RefEntity[] refElements, @Nonnull Map<RefEntity, Set<QuickFix>> actions) {
    Map<Class, QuickFixAction> result = new HashMap<Class, QuickFixAction>();
    for (RefEntity refElement : refElements) {
      final Set<QuickFix> localQuickFixes = actions.get(refElement);
      if (localQuickFixes == null) continue;
      for (QuickFix fix : localQuickFixes) {
        if (fix == null) continue;
        final Class klass = fix instanceof ActionClassHolder ? ((ActionClassHolder ) fix).getActionClass() : fix.getClass();
        final QuickFixAction quickFixAction = result.get(klass);
        if (quickFixAction != null) {
          try {
            String familyName = fix.getFamilyName();
            familyName = !familyName.isEmpty() ? "\'" + familyName + "\'" : familyName;
            ((LocalQuickFixWrapper)quickFixAction).setText(InspectionsBundle.message("inspection.descriptor.provider.apply.fix", familyName));
          }
          catch (AbstractMethodError e) {
            //for plugin compatibility
            ((LocalQuickFixWrapper)quickFixAction).setText(InspectionsBundle.message("inspection.descriptor.provider.apply.fix", ""));
          }
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
      final QuickFix[] fixes = descriptor.getFixes();
      for (int i = 0; i < fixes.length; i++) {
        if (fixes[i] == fix){
          ignoreProblem(refElement, descriptor, i);
          return;
        }
      }
    }
  }


  @Override
  public boolean isElementIgnored(final RefEntity element) {
    for (RefEntity entity : getIgnoredElements().keySet()) {
      if (Comparing.equal(entity, element)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isProblemResolved(RefEntity refEntity, CommonProblemDescriptor descriptor) {
    if (descriptor == null) return true;
    for (RefEntity entity : getIgnoredElements().keySet()) {
      if (Comparing.equal(entity, refEntity)) {
        final CommonProblemDescriptor[] descriptors = getIgnoredElements().get(refEntity);
        return ArrayUtil.contains(descriptor, descriptors);
      }
    }
    return false;
  }

  @Override
  @Nonnull
  public FileStatus getProblemStatus(@Nonnull final CommonProblemDescriptor descriptor) {
    final GlobalInspectionContextImpl context = getContext();
    if (!isDisposed() && context.getUIOptions().SHOW_DIFF_WITH_PREVIOUS_RUN){
      if (myOldProblemElements != null){
        final Set<CommonProblemDescriptor> allAvailable = new HashSet<CommonProblemDescriptor>();
        for (CommonProblemDescriptor[] descriptors : myOldProblemElements.values()) {
          if (descriptors != null) {
            ContainerUtil.addAll(allAvailable, descriptors);
          }
        }
        final boolean old = containsDescriptor(descriptor, allAvailable);
        final boolean current = containsDescriptor(descriptor, getProblemToElements().keySet());
        return calcStatus(old, current);
      }
    }
    return FileStatus.NOT_CHANGED;
  }

  private static boolean containsDescriptor(@Nonnull CommonProblemDescriptor descriptor, Collection<CommonProblemDescriptor> descriptors){
    PsiElement element = null;
    if (descriptor instanceof ProblemDescriptor){
      element = ((ProblemDescriptor)descriptor).getPsiElement();
    }
    for (CommonProblemDescriptor problemDescriptor : descriptors) {
      if (problemDescriptor instanceof ProblemDescriptor){
        if (!Comparing.equal(element, ((ProblemDescriptor)problemDescriptor).getPsiElement())){
          continue;
        }
      }
      if (Comparing.strEqual(problemDescriptor.getDescriptionTemplate(), descriptor.getDescriptionTemplate())){
        return true;
      }
    }
    return false;
  }


  @Nonnull
  @Override
  public FileStatus getElementStatus(final RefEntity element) {
    final GlobalInspectionContextImpl context = getContext();
    if (!isDisposed() && context.getUIOptions().SHOW_DIFF_WITH_PREVIOUS_RUN){
      if (myOldProblemElements != null){
        final boolean old = RefUtil.contains(element, myOldProblemElements.keySet());
        final boolean current = RefUtil.contains(element, getProblemElements().keySet());
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
  public InspectionNode createToolNode(@Nonnull GlobalInspectionContextImpl globalInspectionContext, @Nonnull InspectionNode node,
                                       @Nonnull InspectionRVContentProvider provider,
                                       @Nonnull InspectionTreeNode parentNode,
                                       boolean showStructure) {
    return node;
  }


  @Override
  @Nullable
  public IntentionAction findQuickFixes(@Nonnull final CommonProblemDescriptor problemDescriptor, final String hint) {
    InspectionProfileEntry tool = getToolWrapper().getTool();
    if (!(tool instanceof GlobalInspectionTool)) return null;
    final QuickFix fix = ((GlobalInspectionTool)tool).getQuickFix(hint);
    if (fix == null) {
      return null;
    }
    if (problemDescriptor instanceof ProblemDescriptor) {
      final ProblemDescriptor descriptor = new ProblemDescriptorImpl(((ProblemDescriptor)problemDescriptor).getStartElement(),
                                                                     ((ProblemDescriptor)problemDescriptor).getEndElement(),
                                                                     problemDescriptor.getDescriptionTemplate(),
                                                                     new LocalQuickFix[]{(LocalQuickFix)fix},
                                                                     ProblemHighlightType.GENERIC_ERROR_OR_WARNING, false, null, false);
      return QuickFixWrapper.wrap(descriptor, 0);
    }
    return new IntentionAction() {
      @Override
      @Nonnull
      public String getText() {
        return fix.getName();
      }

      @Override
      @Nonnull
      public String getFamilyName() {
        return fix.getFamilyName();
      }

      @Override
      public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        return true;
      }

      @Override
      public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        fix.applyFix(project, problemDescriptor); //todo check type consistency
      }

      @Override
      public boolean startInWriteAction() {
        return true;
      }
    };
  }

  public static void setOutputPath(final String output) {
    ourOutputPath = output;
  }
}
