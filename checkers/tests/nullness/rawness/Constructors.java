import checkers.initialization.quals.UnknownInitialization;
import checkers.initialization.quals.Initialized;
import checkers.nullness.quals.*;
import java.util.*;

//@skip-test -- should be fixed, but is a bit tricky to implement, so defer for now.
// See http://code.google.com/p/checker-framework/issues/detail?id=223
class Constructors {

  static void requireInitialized(Object o) { }


  static class Box {
    Object f;
    Box(Object arg) {
      this.f = arg;
    }
  }

  public Constructors () {

    //:: error: (argument.type.incompatible)
    Box b1 = new Box(this);
    Box b2 = new Box("hello");
    requireInitialized(b2);
  }

  public class Options {
  
    class OptionInfo {
      Date d;
      /*@UnknownInitialization*/ /*@Raw*/ Object obj;

      OptionInfo (Date d, /*@UnknownInitialization*/ /*@Raw*/ Object obj) {
        this.d = d;
        this.obj = obj;
      }

    }

    public Options (Date d, /*@UnknownInitialization*/ /*@Raw*/ Object obj) {
      OptionInfo oi = new OptionInfo(d, obj);
      // oi should be considered initialized at this point, because
      // the argument type of the constructor was @UnknownInitialization.
      requireInitialized(oi);
    }


  }
}


/* Local Variables: */
/* compile-command: "javac -processor checkers.nullness.NullnessChecker -Xbootclasspath/p:$CHECKERS/jdk/jdk.jar Constructors.java" */
/* compile-history: ("javac -processor checkers.nullness.NullnessChecker -Xbootclasspath/p:$CHECKERS/jdk/jdk.jar Constructors.java") */
/* End: */