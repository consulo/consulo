// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal;

import consulo.application.ReadAction;
import consulo.application.util.AtomicNotNullLazyValue;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.DocumentUtil;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.XSourcePosition;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.navigation.Navigatable;
import consulo.navigation.NonNavigatable;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.http.HttpVirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

public abstract class XSourcePositionImpl implements XSourcePosition {
    private final VirtualFile myFile;

    private XSourcePositionImpl(@Nonnull VirtualFile file) {
        myFile = file;
    }

    @Override
    @Nonnull
    public VirtualFile getFile() {
        return myFile;
    }

    /**
     * do not call this method from plugins, use {@link XDebuggerUtil#createPositionByOffset(VirtualFile, int)} instead
     */
    @Nullable
    public static XSourcePositionImpl createByOffset(@Nullable VirtualFile file, final int offset) {
        if (file == null) {
            return null;
        }

        return new XSourcePositionImpl(file) {
            private final AtomicNotNullLazyValue<Integer> myLine = new AtomicNotNullLazyValue<Integer>() {
                @Nonnull
                @Override
                protected Integer compute() {
                    return ReadAction.compute(() -> {
                        Document document = FileDocumentManager.getInstance().getDocument(file);
                        if (document == null) {
                            return -1;
                        }
                        return DocumentUtil.isValidOffset(offset, document) ? document.getLineNumber(offset) : -1;
                    });
                }
            };

            @Override
            public int getLine() {
                return myLine.getValue();
            }

            @Override
            public int getOffset() {
                return offset;
            }
        };
    }

    /**
     * do not call this method from plugins, use {@link XDebuggerUtil#createPositionByElement(PsiElement)} instead
     */
    @Nullable
    public static XSourcePositionImpl createByElement(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }

        PsiFile psiFile = element.getContainingFile();
        if (psiFile == null) {
            return null;
        }

        final VirtualFile file = psiFile.getVirtualFile();
        if (file == null) {
            return null;
        }

        final SmartPsiElementPointer<PsiElement> pointer = SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);

        return new XSourcePositionImpl(file) {
            private final AtomicNotNullLazyValue<XSourcePosition> myDelegate = new AtomicNotNullLazyValue<XSourcePosition>() {
                @Nonnull
                @Override
                protected XSourcePosition compute() {
                    return ReadAction.compute(() -> {
                        PsiElement elem = pointer.getElement();
                        return XSourcePositionImpl.createByOffset(pointer.getVirtualFile(), elem != null ? elem.getTextOffset() : -1);
                    });
                }
            };

            @Override
            public int getLine() {
                return myDelegate.getValue().getLine();
            }

            @Override
            public int getOffset() {
                return myDelegate.getValue().getOffset();
            }

            @Nonnull
            @Override
            public Navigatable createNavigatable(@Nonnull Project project) {
                // no need to create delegate here, it may be expensive
                if (myDelegate.isComputed()) {
                    return myDelegate.getValue().createNavigatable(project);
                }
                PsiElement elem = pointer.getElement();
                if (elem instanceof Navigatable) {
                    return ((Navigatable) elem);
                }
                return NonNavigatable.INSTANCE;
            }
        };
    }

    /**
     * do not call this method from plugins, use {@link XDebuggerUtil#createPosition(VirtualFile, int)} instead
     */
    @Contract("null , _ -> null; !null, _ -> !null")
    public static XSourcePositionImpl create(@Nullable VirtualFile file, int line) {
        return file == null ? null : create(file, line, 0);
    }

    /**
     * do not call this method from plugins, use {@link XDebuggerUtil#createPosition(VirtualFile, int, int)} instead
     */
    @Contract("null , _, _ -> null; !null, _, _ -> !null")
    public static XSourcePositionImpl create(@Nullable VirtualFile file, final int line, final int column) {
        if (file == null) {
            return null;
        }

        return new XSourcePositionImpl(file) {
            private final AtomicNotNullLazyValue<Integer> myOffset = new AtomicNotNullLazyValue<Integer>() {
                @Nonnull
                @Override
                protected Integer compute() {
                    return ReadAction.compute(() -> {
                        int offset;
                        if (file instanceof LightVirtualFile || file instanceof HttpVirtualFile) {
                            return -1;
                        }
                        else {
                            Document document = FileDocumentManager.getInstance().getDocument(file);
                            if (document == null) {
                                return -1;
                            }
                            int l = Math.max(0, line);
                            int c = Math.max(0, column);

                            offset = l < document.getLineCount() ? document.getLineStartOffset(l) + c : -1;

                            if (offset >= document.getTextLength()) {
                                offset = document.getTextLength() - 1;
                            }
                        }
                        return offset;
                    });
                }
            };

            @Override
            public int getLine() {
                return line;
            }

            @Override
            public int getOffset() {
                return myOffset.getValue();
            }
        };
    }

    @Override
    @Nonnull
    public Navigatable createNavigatable(@Nonnull Project project) {
        return doCreateOpenFileDescriptor(project, this);
    }

    @Nonnull
    public static OpenFileDescriptor createOpenFileDescriptor(@Nonnull Project project, @Nonnull XSourcePosition position) {
        Navigatable navigatable = position.createNavigatable(project);
        if (navigatable instanceof OpenFileDescriptor openFileDescriptor) {
            return openFileDescriptor;
        }
        else {
            return doCreateOpenFileDescriptor(project, position);
        }
    }

    @Nonnull
    public static OpenFileDescriptor doCreateOpenFileDescriptor(@Nonnull Project project, @Nonnull XSourcePosition position) {
        OpenFileDescriptorFactory factory = OpenFileDescriptorFactory.getInstance(project);

        OpenFileDescriptorFactory.Builder builder = factory.newBuilder(position.getFile());
        builder = builder.line(position.getLine());
        if (position.getOffset() != -1) {
            builder = builder.offset(position.getOffset());
        } else {
            builder = builder.column(0);
        }
        return builder.build();
    }

    @Override
    public String toString() {
        return "XSourcePositionImpl[" + myFile + ":" + getLine() + "(" + getOffset() + ")]";
    }
}
