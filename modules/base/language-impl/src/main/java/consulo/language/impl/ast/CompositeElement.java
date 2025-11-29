/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.language.impl.ast;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.util.concurrent.ThreadDumper;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.impl.DebugUtil;
import consulo.language.impl.file.FreeThreadedFileViewProvider;
import consulo.language.impl.internal.pom.TreeChangeEventImpl;
import consulo.language.impl.psi.*;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.util.collection.ArrayFactory;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.IntFunction;

public class CompositeElement extends TreeElement {
    private static final Logger LOG = Logger.getInstance(CompositeElement.class);
    public static final CompositeElement[] EMPTY_ARRAY = new CompositeElement[0];

    private TreeElement firstChild;
    private TreeElement lastChild;

    private volatile int myCachedLength = -1;
    private volatile int myHC = -1;
    private volatile PsiElement myWrapper;
    private static final boolean ASSERT_THREADING = true;
    //DebugUtil.CHECK || ApplicationManagerEx.getApplicationEx().isInternal() || ApplicationManagerEx.getApplicationEx().isUnitTestMode();

    private static final VarHandle ourPsiUpdater;

    static {
        try {
            ourPsiUpdater = MethodHandles.lookup().findVarHandle(CompositeElement.class, "myWrapper", PsiElement.class);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public CompositeElement(@Nonnull IElementType type) {
        super(type);
    }

    @Nonnull
    @Override
    public CompositeElement clone() {
        CompositeElement clone = (CompositeElement) super.clone();

        clone.firstChild = null;
        clone.lastChild = null;
        clone.myWrapper = null;
        for (ASTNode child = rawFirstChild(); child != null; child = child.getTreeNext()) {
            clone.rawAddChildrenWithoutNotifications((TreeElement) child.clone());
        }
        clone.clearCaches();
        return clone;
    }

    public void subtreeChanged() {
        CompositeElement compositeElement = this;
        while (compositeElement != null) {
            compositeElement.clearCaches();
            if (!(compositeElement instanceof PsiElement)
                && compositeElement.myWrapper instanceof PsiElementWithSubtreeChangeNotifier changeNotifier) {
                changeNotifier.subtreeChanged();
            }

            compositeElement = compositeElement.getTreeParent();
        }
    }

    @Override
    public void clearCaches() {
        myCachedLength = -1;

        myHC = -1;

        TreeElement.clearRelativeOffsets(rawFirstChild());
    }

    private static void assertThreading(@Nonnull PsiFile file) {
        if (ASSERT_THREADING) {
            boolean ok = file.getApplication().isWriteAccessAllowed() || isNonPhysicalOrInjected(file);
            if (!ok) {
                LOG.error("Threading assertion. " + getThreadingDiagnostics(file));
            }
        }
    }

    private static String getThreadingDiagnostics(@Nonnull PsiFile psiFile) {
        return "psiFile: " + psiFile +
            "; psiFile.getViewProvider(): " + psiFile.getViewProvider() +
            "; psiFile.isPhysical(): " + psiFile.isPhysical() +
            "; nonPhysicalOrInjected: " + isNonPhysicalOrInjected(psiFile);
    }

    private static boolean isNonPhysicalOrInjected(@Nonnull PsiFile psiFile) {
        return psiFile instanceof DummyHolder || psiFile.getViewProvider() instanceof FreeThreadedFileViewProvider || !psiFile.isPhysical();
    }

    @Override
    public void acceptTree(@Nonnull TreeElementVisitor visitor) {
        visitor.visitComposite(this);
    }

    @Override
    public LeafElement findLeafElementAt(int offset) {
        TreeElement element = this;
        if (element.getTreeParent() == null && offset >= element.getTextLength()) {
            return null;
        }
        startFind:
        while (true) {
            TreeElement child = element.getFirstChildNode();
            TreeElement lastChild = element.getLastChildNode();
            int elementTextLength = element.getTextLength();
            boolean fwd = lastChild == null || elementTextLength / 2 > offset;
            if (!fwd) {
                child = lastChild;
                offset = elementTextLength - offset;
            }
            while (child != null) {
                int textLength = child.getTextLength();
                if (textLength > offset || !fwd && textLength >= offset) {
                    if (child instanceof LeafElement leafElement) {
                        if (leafElement instanceof ForeignLeafPsiElement) {
                            child = fwd ? leafElement.getTreeNext() : leafElement.getTreePrev();
                            continue;
                        }
                        return leafElement;
                    }
                    offset = fwd ? offset : textLength - offset;
                    element = child;
                    continue startFind;
                }
                offset -= textLength;
                child = fwd ? child.getTreeNext() : child.getTreePrev();
            }
            return null;
        }
    }

    @Nullable
    public PsiElement findPsiChildByType(@Nonnull IElementType type) {
        ASTNode node = findChildByType(type);
        return node == null ? null : node.getPsi();
    }

    @Nullable
    public PsiElement findPsiChildByType(@Nonnull TokenSet types) {
        ASTNode node = findChildByType(types);
        return node == null ? null : node.getPsi();
    }

    @Override
    public ASTNode findChildByType(@Nonnull IElementType type) {
        if (DebugUtil.CHECK_INSIDE_ATOMIC_ACTION_ENABLED) {
            assertReadAccessAllowed();
        }

        for (ASTNode element = getFirstChildNode(); element != null; element = element.getTreeNext()) {
            if (element.getElementType() == type) {
                return element;
            }
        }
        return null;
    }

    @Override
    public ASTNode findChildByType(@Nonnull IElementType type, ASTNode anchor) {
        if (DebugUtil.CHECK_INSIDE_ATOMIC_ACTION_ENABLED) {
            assertReadAccessAllowed();
        }

        return TreeUtil.findSibling(anchor, type);
    }

    @Nullable
    @Override
    public ASTNode findChildByType(@Nonnull TokenSet types) {
        if (DebugUtil.CHECK_INSIDE_ATOMIC_ACTION_ENABLED) {
            assertReadAccessAllowed();
        }
        for (ASTNode element = getFirstChildNode(); element != null; element = element.getTreeNext()) {
            if (types.contains(element.getElementType())) {
                return element;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public ASTNode findChildByType(@Nonnull TokenSet typesSet, ASTNode anchor) {
        if (DebugUtil.CHECK_INSIDE_ATOMIC_ACTION_ENABLED) {
            assertReadAccessAllowed();
        }
        return TreeUtil.findSibling(anchor, typesSet);
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public String getText() {
        return new String(textToCharArray());
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public CharSequence getChars() {
        return getText();
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public char[] textToCharArray() {
        assertReadAccessAllowed();

        int len = getTextLength();
        char[] buffer = new char[len];
        int endOffset;
        try {
            endOffset = AstBufferUtil.toBuffer(this, buffer, 0);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            String msg = "Underestimated text length: " + len;
            try {
                int length = AstBufferUtil.toBuffer(this, new char[len], 0);
                msg += ";\n repetition gives success (" + length + ")";
            }
            catch (ArrayIndexOutOfBoundsException e1) {
                msg += ";\n repetition fails as well";
            }
            throw new RuntimeException(msg, e);
        }
        if (endOffset != len) {
            String msg = "len=" + len + ";\n endOffset=" + endOffset;
            msg += diagnoseTextInconsistency(new String(buffer, 0, Math.min(len, endOffset)));
            throw new AssertionError(msg);
        }
        return buffer;
    }

    @RequiredReadAction
    private String diagnoseTextInconsistency(String text) {
        StringBuilder msg = new StringBuilder();
        msg.append(";\n nonPhysicalOrInjected=").append(isNonPhysicalOrInjected(SharedImplUtil.getContainingFile(this)));
        msg.append(";\n buffer=").append(text);
        try {
            msg.append(";\n this=").append(this);
        }
        catch (StackOverflowError e) {
            msg.append(";\n this.toString produces SOE");
        }
        int shitStart = textMatches(text, 0);
        msg.append(";\n matches until ").append(shitStart);
        LeafElement leaf = findLeafElementAt(Math.abs(shitStart));
        msg.append(";\n element there=").append(leaf);
        if (leaf != null) {
            PsiElement psi = leaf.getPsi();
            msg.append(";\n leaf.text=").append(leaf.getText());
            msg.append(";\n leaf.psi=").append(psi);
            msg.append(";\n leaf.lang=").append(psi == null ? null : psi.getLanguage());
            msg.append(";\n leaf.type=").append(leaf.getElementType());
        }
        PsiElement psi = getPsi();
        if (psi != null) {
            boolean valid = psi.isValid();
            msg.append(";\n psi.valid=").append(valid);
            if (valid) {
                PsiFile file = psi.getContainingFile();
                if (file != null) {
                    msg.append(";\n psi.file=").append(file);
                    msg.append(";\n psi.file.tl=").append(file.getTextLength());
                    msg.append(";\n psi.file.lang=").append(file.getLanguage());
                    msg.append(";\n psi.file.vp=").append(file.getViewProvider());
                    msg.append(";\n psi.file.vp.lang=").append(file.getViewProvider().getLanguages());
                    msg.append(";\n psi.file.vp.lang=").append(file.getViewProvider().getLanguages());

                    PsiElement fileLeaf = file.findElementAt(getTextRange().getStartOffset());
                    LeafElement myLeaf = findLeafElementAt(0);
                    msg.append(";\n leaves at start=").append(fileLeaf).append(" and ").append(myLeaf);
                }
            }
        }
        return msg.toString();
    }

    @Override
    public boolean textContains(char c) {
        for (ASTNode child = getFirstChildNode(); child != null; child = child.getTreeNext()) {
            if (child.textContains(c)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int textMatches(@Nonnull final CharSequence buffer, int start) {
        final int[] curOffset = {start};
        acceptTree(new RecursiveTreeElementWalkingVisitor() {
            @Override
            public void visitLeaf(LeafElement leaf) {
                matchText(leaf);
            }

            private void matchText(TreeElement leaf) {
                curOffset[0] = leaf.textMatches(buffer, curOffset[0]);
                if (curOffset[0] < 0) {
                    stopWalking();
                }
            }

            @Override
            public void visitComposite(CompositeElement composite) {
                if (composite instanceof LazyParseableElement lazyParseableElement && !lazyParseableElement.isParsed()) {
                    matchText(lazyParseableElement);
                }
                else {
                    super.visitComposite(composite);
                }
            }
        });
        return curOffset[0];
    }

    @Nullable
    public final PsiElement findChildByRoleAsPsiElement(int role) {
        ASTNode element = findChildByRole(role);
        if (element == null) {
            return null;
        }
        return SourceTreeToPsiMap.treeElementToPsi(element);
    }

    @Nullable
    public ASTNode findChildByRole(int role) {
        // assert ChildRole.isUnique(role);
        for (ASTNode child = getFirstChildNode(); child != null; child = child.getTreeNext()) {
            if (getChildRole(child) == role) {
                return child;
            }
        }
        return null;
    }

    public int getChildRole(@Nonnull ASTNode child) {
        LOG.assertTrue(child.getTreeParent() == this, child);
        return 0; //ChildRole.NONE;
    }

    protected final int getChildRole(@Nonnull ASTNode child, int roleCandidate) {
        if (findChildByRole(roleCandidate) == child) {
            return roleCandidate;
        }
        return 0; //ChildRole.NONE;
    }

    @Nonnull
    @Override
    public ASTNode[] getChildren(@Nullable TokenSet filter) {
        int count = countChildren(filter);
        if (count == 0) {
            return EMPTY_ARRAY;
        }
        ASTNode[] result = new ASTNode[count];
        count = 0;
        for (ASTNode child = getFirstChildNode(); child != null; child = child.getTreeNext()) {
            if (filter == null || filter.contains(child.getElementType())) {
                result[count++] = child;
            }
        }
        return result;
    }

    @Nonnull
    public <T extends PsiElement> T[] getChildrenAsPsiElements(@Nullable TokenSet filter, @Nonnull IntFunction<? extends T[]> constructor) {
        assertReadAccessAllowed();
        int count = countChildren(filter);
        T[] result = constructor.apply(count);
        if (count == 0) {
            return result;
        }
        int idx = 0;
        for (ASTNode child = getFirstChildNode(); child != null && idx < count; child = child.getTreeNext()) {
            if (filter == null || filter.contains(child.getElementType())) {
                @SuppressWarnings("unchecked") T element = (T) child.getPsi();
                LOG.assertTrue(element != null, child);
                result[idx++] = element;
            }
        }
        return result;
    }

    @Nonnull
    public <T extends PsiElement> T[] getChildrenAsPsiElements(@Nonnull IElementType type, @Nonnull ArrayFactory<? extends T> constructor) {
        assertReadAccessAllowed();
        int count = countChildren(type);
        T[] result = constructor.create(count);
        if (count == 0) {
            return result;
        }
        int idx = 0;
        for (ASTNode child = getFirstChildNode(); child != null && idx < count; child = child.getTreeNext()) {
            if (type == child.getElementType()) {
                @SuppressWarnings("unchecked") T element = (T) child.getPsi();
                LOG.assertTrue(element != null, child);
                result[idx++] = element;
            }
        }
        return result;
    }

    public int countChildren(@Nullable TokenSet filter) {
        // no lock is needed because all chameleons are expanded already
        int count = 0;
        for (ASTNode child = getFirstChildNode(); child != null; child = child.getTreeNext()) {
            if (filter == null || filter.contains(child.getElementType())) {
                count++;
            }
        }

        return count;
    }

    private int countChildren(@Nonnull IElementType type) {
        // no lock is needed because all chameleons are expanded already
        int count = 0;
        for (ASTNode child = getFirstChildNode(); child != null; child = child.getTreeNext()) {
            if (type == child.getElementType()) {
                count++;
            }
        }

        return count;
    }

    /**
     * @return First element that was appended (for example whitespaces could be skipped)
     */
    @RequiredWriteAction
    public TreeElement addInternal(TreeElement first, ASTNode last, @Nullable ASTNode anchor, @Nullable Boolean before) {
        ASTNode anchorBefore;
        if (anchor == null) {
            anchorBefore = before == null || before ? null : getFirstChildNode();
        }
        else {
            anchorBefore = before ? anchor : anchor.getTreeNext();
        }
        return (TreeElement) CodeEditUtil.addChildren(this, first, last, anchorBefore);
    }

    @RequiredWriteAction
    public void deleteChildInternal(@Nonnull ASTNode child) {
        CodeEditUtil.removeChild(this, child);
    }

    @RequiredWriteAction
    public void replaceChildInternal(@Nonnull ASTNode child, @Nonnull TreeElement newElement) {
        CodeEditUtil.replaceChild(this, child, newElement);
    }

    @Override
    @RequiredReadAction
    public int getTextLength() {
        int cachedLength = myCachedLength;
        if (cachedLength >= 0) {
            return cachedLength;
        }

        assertReadAccessAllowed(); //otherwise a write action can modify the tree while we're walking it
        try {
            return walkCachingLength();
        }
        catch (AssertionError e) {
            myCachedLength = -1;
            String assertion = ExceptionUtil.getThrowableText(e);
            throw new AssertionError("Walking failure: ===\n" + assertion + "\n=== Thread dump:\n" + ThreadDumper.dumpThreadsToString() + "\n===\n");
        }
    }

    @Override
    public int hc() {
        int hc = myHC;
        if (hc == -1) {
            hc = 0;
            TreeElement child = firstChild;
            while (child != null) {
                hc += child.hc();
                child = child.getTreeNext();
            }
            myHC = hc;
        }
        return hc;
    }

    @Override
    public int getCachedLength() {
        return myCachedLength;
    }

    @Nonnull
    private static TreeElement drillDown(@Nonnull TreeElement start) {
        TreeElement cur = start;
        while (cur.getCachedLength() < 0) {
            TreeElement child = cur.getFirstChildNode();
            if (child == null) {
                break;
            }
            cur = child;
        }
        return cur;
    }

    // returns computed length
    @RequiredReadAction
    private int walkCachingLength() {
        TreeElement cur = drillDown(this);
        while (true) {
            int length = cur.getCachedLength();
            if (length < 0) {
                // can happen only in CompositeElement
                length = 0;
                for (TreeElement child = cur.getFirstChildNode(); child != null; child = child.getTreeNext()) {
                    length += child.getTextLength();
                }
                ((CompositeElement) cur).setCachedLength(length);
            }

            if (cur == this) {
                return length;
            }

            TreeElement next = cur.getTreeNext();
            cur = next != null ? drillDown(next) : getNotNullParent(cur);
        }
    }

    @RequiredReadAction
    private static TreeElement getNotNullParent(TreeElement cur) {
        TreeElement parent = cur.getTreeParent();
        if (parent == null) {
            diagnoseNullParent(cur);
        }
        return parent;
    }

    @RequiredReadAction
    private static void diagnoseNullParent(TreeElement cur) {
        PsiElement psi = cur.getPsi();
        if (psi != null) {
            PsiUtilCore.ensureValid(psi);
        }
        throw new IllegalStateException("Null parent of " + cur + " " + cur.getClass());
    }

    void setCachedLength(int cachedLength) {
        myCachedLength = cachedLength;
    }

    @Override
    public TreeElement getFirstChildNode() {
        return firstChild;
    }

    @Override
    public TreeElement getLastChildNode() {
        return lastChild;
    }

    void setFirstChildNode(TreeElement firstChild) {
        this.firstChild = firstChild;
        TreeElement.clearRelativeOffsets(firstChild);
    }

    void setLastChildNode(TreeElement lastChild) {
        this.lastChild = lastChild;
    }

    @Override
    public void addChild(@Nonnull ASTNode child, @Nullable ASTNode anchorBefore) {
        LOG.assertTrue(
            anchorBefore == null || ((TreeElement) anchorBefore).getTreeParent() == this,
            "anchorBefore == null || anchorBefore.getTreeParent() == parent"
        );
        TreeUtil.ensureParsed(getFirstChildNode());
        TreeUtil.ensureParsed(child);
        TreeElement last = ((TreeElement) child).getTreeNext();
        TreeElement first = (TreeElement) child;

        removeChildrenInner(first, last);

        ChangeUtil.prepareAndRunChangeAction(destinationTreeChange -> {
            if (anchorBefore != null) {
                insertBefore((TreeChangeEventImpl) destinationTreeChange, (TreeElement) anchorBefore, first);
            }
            else {
                add((TreeChangeEventImpl) destinationTreeChange, this, first);
            }
        }, this);
    }

    @Override
    @RequiredReadAction
    public void addLeaf(@Nonnull IElementType leafType, @Nonnull CharSequence leafText, ASTNode anchorBefore) {
        FileElement holder = new DummyHolder(getManager(), null).getTreeElement();
        LeafElement leaf = ASTFactory.leaf(leafType, holder.getCharTable().intern(leafText));
        CodeEditUtil.setNodeGenerated(leaf, true);
        holder.rawAddChildren(leaf);

        addChild(leaf, anchorBefore);
    }

    @Override
    public void addChild(@Nonnull ASTNode child) {
        addChild(child, null);
    }

    @Override
    public void removeChild(@Nonnull ASTNode child) {
        removeChildInner((TreeElement) child);
    }

    @Override
    public void removeRange(@Nonnull ASTNode first, ASTNode firstWhichStayInTree) {
        removeChildrenInner((TreeElement) first, (TreeElement) firstWhichStayInTree);
    }

    @Override
    public void replaceChild(@Nonnull ASTNode oldChild, @Nonnull ASTNode newChild) {
        LOG.assertTrue(((TreeElement) oldChild).getTreeParent() == this);
        TreeElement oldChild1 = (TreeElement) oldChild;
        TreeElement newChildNext = ((TreeElement) newChild).getTreeNext();
        TreeElement newChild1 = (TreeElement) newChild;

        if (oldChild1 == newChild1) {
            return;
        }

        removeChildrenInner(newChild1, newChildNext);

        ChangeUtil.prepareAndRunChangeAction(
            destinationTreeChange -> {
                replace((TreeChangeEventImpl) destinationTreeChange, oldChild1, newChild1);
                repairRemovedElement(this, oldChild1);
            },
            this
        );
    }

    @Override
    public void replaceAllChildrenToChildrenOf(@Nonnull ASTNode anotherParent) {
        TreeUtil.ensureParsed(getFirstChildNode());
        TreeUtil.ensureParsed(anotherParent.getFirstChildNode());
        ASTNode firstChild = anotherParent.getFirstChildNode();
        ChangeUtil.prepareAndRunChangeAction(
            event -> remove(
                (TreeChangeEventImpl) event,
                (TreeElement) anotherParent.getFirstChildNode(),
                null
            ),
            (TreeElement) anotherParent
        );

        if (firstChild != null) {
            ChangeUtil.prepareAndRunChangeAction(
                destinationTreeChange -> {
                    TreeElement first = getFirstChildNode();
                    TreeChangeEventImpl event = (TreeChangeEventImpl) destinationTreeChange;
                    CompositeElement parent = getTreeParent();
                    if (parent != null) {
                        // treat all replacements as one big childrenChanged to simplify resulting PSI/document events
                        event.addElementaryChange(parent);
                    }
                    remove(event, first, null);
                    add(event, this, (TreeElement) firstChild);
                    if (parent != null) {
                        repairRemovedElement(this, first);
                    }
                },
                this
            );
        }
        else {
            removeAllChildren();
        }
    }

    public void removeAllChildren() {
        TreeElement child = getFirstChildNode();
        if (child != null) {
            removeRange(child, null);
        }
    }

    @Override
    public void addChildren(@Nonnull ASTNode firstChild, ASTNode lastChild, ASTNode anchorBefore) {
        ASTNode next;
        for (ASTNode f = firstChild; f != lastChild; f = next) {
            next = f.getTreeNext();
            addChild(f, anchorBefore);
        }
    }

    /**
     * Don't call this method, it's here for implementation reasons.
     */
    @Nullable
    final PsiElement getCachedPsi() {
        return myWrapper;
    }

    @Override
    @RequiredReadAction
    public final PsiElement getPsi() {
        ProgressIndicatorProvider.checkCanceled(); // We hope this method is being called often enough to cancel daemon processes smoothly

        PsiElement wrapper = myWrapper;
        if (wrapper != null) {
            return wrapper;
        }

        wrapper = createPsiNoLock();
        return ourPsiUpdater.compareAndSet(this, null, wrapper) ? wrapper : ObjectUtil.assertNotNull(myWrapper);
    }

    @Override
    @RequiredReadAction
    public <T extends PsiElement> T getPsi(@Nonnull Class<T> clazz) {
        return LeafElement.getPsi(clazz, getPsi(), LOG);
    }

    @RequiredReadAction
    protected PsiElement createPsiNoLock() {
        return PsiElementFactory.EP.getValue(getElementType()).createElement(this);
    }

    public void setPsi(@Nonnull PsiElement psi) {
        PsiElement prev = myWrapper;
        if (prev != null && prev != psi) {
            DebugUtil.onInvalidated(prev);
        }
        myWrapper = psi;
    }

    void clearPsi() {
        myWrapper = null;
    }

    public final void rawAddChildren(@Nonnull TreeElement first) {
        rawAddChildrenWithoutNotifications(first);

        subtreeChanged();
    }

    public void rawAddChildrenWithoutNotifications(@Nonnull TreeElement first) {
        TreeElement last = getLastChildNode();
        if (last == null) {
            TreeElement chainLast = rawSetParents(first, this);
            setFirstChildNode(first);
            setLastChildNode(chainLast);
        }
        else {
            last.rawInsertAfterMeWithoutNotifications(first);
        }

        DebugUtil.checkTreeStructure(this);
    }

    @Nonnull
    static TreeElement rawSetParents(@Nonnull TreeElement child, @Nonnull CompositeElement parent) {
        child.rawRemoveUpToWithoutNotifications(null, false);
        while (true) {
            child.setTreeParent(parent);
            TreeElement treeNext = child.getTreeNext();
            if (treeNext == null) {
                return child;
            }
            child = treeNext;
        }
    }

    public void rawRemoveAllChildren() {
        TreeElement first = getFirstChildNode();
        if (first != null) {
            first.rawRemoveUpToLast();
        }
    }

    @RequiredReadAction
    private static void repairRemovedElement(@Nonnull CompositeElement oldParent, TreeElement oldChild) {
        if (oldChild == null) {
            return;
        }
        FileElement treeElement = DummyHolderFactory.createHolder(oldParent.getManager(), null, false).getTreeElement();
        treeElement.rawAddChildren(oldChild);
    }

    private static void add(
        @Nonnull TreeChangeEventImpl destinationTreeChange,
        @Nonnull CompositeElement parent,
        @Nonnull TreeElement first
    ) {
        destinationTreeChange.addElementaryChange(parent);
        parent.rawAddChildren(first);
    }

    private static void remove(@Nonnull TreeChangeEventImpl destinationTreeChange, TreeElement first, TreeElement last) {
        if (first != null) {
            destinationTreeChange.addElementaryChange(first.getTreeParent());
            first.rawRemoveUpTo(last);
        }
    }

    private static void insertBefore(
        @Nonnull TreeChangeEventImpl destinationTreeChange,
        @Nonnull TreeElement anchorBefore,
        @Nonnull TreeElement first
    ) {
        destinationTreeChange.addElementaryChange(anchorBefore.getTreeParent());
        anchorBefore.rawInsertBeforeMe(first);
    }

    private static void replace(
        @Nonnull TreeChangeEventImpl sourceTreeChange,
        @Nonnull TreeElement oldChild,
        @Nonnull TreeElement newChild
    ) {
        sourceTreeChange.addElementaryChange(oldChild.getTreeParent());
        oldChild.rawReplaceWithList(newChild);
    }

    private static void removeChildInner(@Nonnull TreeElement child) {
        removeChildrenInner(child, child.getTreeNext());
    }

    private static void removeChildrenInner(@Nonnull TreeElement first, TreeElement last) {
        FileElement fileElement = TreeUtil.getFileElement(first);
        if (fileElement != null) {
            ChangeUtil.prepareAndRunChangeAction(
                destinationTreeChange -> {
                    remove((TreeChangeEventImpl) destinationTreeChange, first, last);
                    repairRemovedElement(fileElement, first);
                },
                first.getTreeParent()
            );
        }
        else {
            first.rawRemoveUpTo(last);
        }
    }

    public TreeElement rawFirstChild() {
        return firstChild;
    }

    public TreeElement rawLastChild() {
        return lastChild;
    }
}