import java.io.IOException;

class Test {
  void foo(Exception targetElement) {
    PsiJavaPackage aPack = <selection>JavaDirectoryService.getInstance().getPackage((IOException)targetElement)</selection>;
  }
}