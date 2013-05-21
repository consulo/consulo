import java.io.IOException;

class Test {
  void foo(Exception targetElement) {
    PsiJavaPackage aPack = newMethod((IOException) targetElement);
  }

    private PsiJavaPackage newMethod(IOException targetElement) {
        return JavaDirectoryService.getInstance().getPackage((IOException)targetElement);
    }
}