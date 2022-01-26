// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package consulo.language.psi.stub;

import consulo.language.ast.ASTNode;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.ast.IElementType;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public abstract class IStubElementType<StubT extends StubElement, PsiT extends PsiElement> extends IElementType implements StubSerializer<StubT> {
  private static volatile boolean ourInitializedStubs;
  private static volatile Set<String> ourLazyExternalIds = Collections.emptySet();
  private static final Logger LOG = Logger.getInstance(IStubElementType.class);

  public IStubElementType(@Nonnull final String debugName, @Nullable final Language language) {
    super(debugName, language);
    if (!isLazilyRegistered()) {
      checkNotInstantiatedTooLate();
    }
  }

  public static void checkNotInstantiatedTooLate() {
    if (ourInitializedStubs) {
      LOG.error("All stub element types should be created before index initialization is complete.\n" +
                "Please add the class containing stub element type constants to \"stubElementTypeHolder\" extension.\n" +
                "Registered extensions: " + Arrays.toString(StubElementTypeHolderEP.EP_NAME.getExtensions()));
    }
  }

  private boolean isLazilyRegistered() {
    try {
      return ourLazyExternalIds.contains(getExternalId());
    }
    catch (Throwable e) {
      // "getExternalId" might throw when called from constructor, if it accesses subclass fields
      // Lazily-registered types have a contract that their "getExternalId" doesn't throw like this,
      // so getting an exception here is a sign that someone indeed creates their stub type after StubElementTypeHolderEP initialization.
      return false;
    }
  }

  public static List<StubFieldAccessor> loadRegisteredStubElementTypes() {
    List<StubFieldAccessor> result = new ArrayList<>();
    for (StubElementTypeHolderEP bean : StubElementTypeHolderEP.EP_NAME.getExtensionList()) {
      result.addAll(bean.initializeOptimized());
    }

    Set<String> lazyIds = new HashSet<>();
    for (StubFieldAccessor accessor : result) {
      lazyIds.add(accessor.externalId);
    }
    ourLazyExternalIds = lazyIds;
    ourInitializedStubs = true;
    return result;
  }

  public abstract PsiT createPsi(@Nonnull StubT stub);

  @Nonnull
  public abstract StubT createStub(@Nonnull PsiT psi, final StubElement parentStub);

  public boolean shouldCreateStub(ASTNode node) {
    return true;
  }

}