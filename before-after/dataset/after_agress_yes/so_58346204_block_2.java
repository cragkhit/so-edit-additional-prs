public class PApplet {
  static public final String javaVersionName =
    System.getProperty("java.version");
  static public final int javaPlatform =
     PApplet.parseInt(PApplet.split(javaVersionName, '.')[1]);
  static public String[] split(String value, char delim) {
    // do this so that the exception occurs inside the user's
    // program, rather than appearing to be a bug inside split()
    if (value == null) return null;
    //return split(what, String.valueOf(delim));  // huh
    char chars[] = value.toCharArray();
    int splitCount = 0; //1;
    for (int i = 0; i < chars.length; i++) {
      if (chars[i] == delim) splitCount++;
    }
    // make sure that there is something in the input string
    //if (chars.length > 0) {
      // if the last char is a delimeter, get rid of it..
      //if (chars[chars.length-1] == delim) splitCount--;
      // on second thought, i don't agree with this, will disable
    //}
    if (splitCount == 0) {
      String splits[] = new String[1];
      splits[0] = value;
      return splits;
    }
    //int pieceCount = splitCount + 1;
    String splits[] = new String[splitCount + 1];
    int splitIndex = 0;
    int startIndex = 0;
    for (int i = 0; i < chars.length; i++) {
      if (chars[i] == delim) {
        splits[splitIndex++] =
          new String(chars, startIndex, i-startIndex);
        startIndex = i + 1;
      }
    }
    //if (startIndex != chars.length) {
      splits[splitIndex] =
        new String(chars, startIndex, chars.length-startIndex);
    //}
    return splits;
  }
  static final public int parseInt(String what) {
    return parseInt(what, 0);
  }
  static final public int parseInt(String what, int otherwise) {
    try {
      int offset = what.indexOf('.');
      if (offset == -1) {
        return Integer.parseInt(what);
      } else {
        return Integer.parseInt(what.substring(0, offset));
      }
    } catch (NumberFormatException e) { }
    return otherwise;
  }
  public static void main(String [] args) {
    System.out.println("Java version: " + javaPlatform);
  }
}
and for main
public class Main {
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Demo" };
    PApplet.main(appletArgs);
  }
}
where you will get your Exception
> javac PApplet.java
> javac Main.java
> java -cp . Main
Exception in thread "main" java.lang.ExceptionInInitializerError
	at Main.main(Main.java:7)
Caused by: java.lang.ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1
	at PApplet.<clinit>(PApplet.java:6)
	... 1 more
if you use the tick suggested in comment, you can get it "fixed"
public class Main {
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Demo" };
    System.setProperty("java.version", "1.1.1");
    PApplet.main(appletArgs);
  }
}
and you will get your code running
> javac PApplet.java
> javac Main.java
> java -cp . Main
Java version: 1
So, concluding, why not building it from most recent sources? :)
**Alternative approach - little bit hacky**
Let's say you can't alter neither `Main.java` nor `core.jar`. What you can do here is to cheat `Java` where to look for `PApplet.class`. You can create a dir - let's call it `fix`. Inside `fix` you can compile just one class: `PApplet.java`. And then, you can run the code like this:
> java -cp fix:. Main
where fix contains your specially crafted version
public class PApplet {
  static public final String javaVersionName =
    System.getProperty("java.version");
  static public final int javaPlatform = 1;
  public static void main(String [] args) {
    System.out.println("Java version: " + javaPlatform);
  }
}