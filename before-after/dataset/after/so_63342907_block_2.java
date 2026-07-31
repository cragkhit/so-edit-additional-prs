ClassReader cr = new ClassReader(Target.class.getResourceAsStream("Target.class"));
ClassWriter cw = new ClassWriter(cr, 0);
cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
    @Override
    public void visit(int version, int access, String name,
                      String signature, String superName, String[] interfaces) {
        super.visit(version, access, "ClonedTarget", signature, superName, interfaces);
    }
}, 0);
byte[] code = cw.toByteArray();
When chaining a `ClassReader` with a `ClassWriter`, the `ClassVisitor` in the middle only needs to overwrite those methods corresponding to an artifact it wants to change. So, to change the name and nothing else, we only need to override the `visit` method for the class? declaration and pass a different name to the `super` method.
By passing the class reader to the class writer?s constructor, we?re even denoting that only little changes will be made, enabling subsequent optimizations of the transform process, i.e. most of the constant pool, as well as the code of the methods, will just get copied here.
---
It?s worth considering the implications. On the bytecode level, constructors have the special name `<init>`, so they keep being constructors in the resulting class, regardless of its name. Trivial constructors calling a superclass constructor may continue to work in the resulting class.
When invoking instance methods on `ClonedTarget` objects, the `this` reference has the type `ClonedTarget`. This fundamental property does not need to be declared and thus, there is no declaration that needs adaptation in this regard.
Herein lies the problem. The original code assumes that `this` is of type `Target` and since nothing has been adapted, the copied code still wrongly assumes that `this` is of type `Target`, which can break in various ways.
Consider:
public class Target {
  public Target clone() { return new Target(); }
  public int compare(Target t) { return 0;}
}
This looks like not being affected by the issue. The generated default constructor just calls `super()` and will continue to work. The `compare` method has an unused parameter type left as-is. And the `clone()` method instantiates `Target` (unchanged) and returns it, matching the return type `Target` (unchanged). Seems fine.
But what?s not visible here, the `clone` method overrides the method `Object clone()` inherited from `java.lang.Object` and therefore, a bridge method will be generated. This bridge method will have the declaration `Object clone()` and just delegate to the `Target clone()` method. The problem is that this delegation is an invocation on `this` and the assumed type of the invocation target is encoded within the invocation instruction. This will cause a `VerifierError`.
Generally, we can not simply tell apart which invocations are applied on `this` and which on an unchanged reference, like a parameter or field. It does not even need to have a definite answer. Consider:
public void method(Target t, boolean b) {
    (b? this: t).otherMethod();
}
Implicitly assuming that `this` has type `Target`, it can use `this` and a `Target` instance from another source interchangeably. We can not change the `this` type and keep the parameter type without rewriting the code.
Other issues arise with visibility. For the renamed class, the verifier will reject unchanged accesses to `private` members of the original class.
Besides failing with a `VerifyError`, problematic code may slip through and cause problems at a later time. Consider:
public class Target implements Cloneable {
    public Target duplicate() {
        try {
            return (Target)super.clone();
        } catch(CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }
}
Since this `duplicate()` does not override a superclass method, there won?t be a bridge method and all unchanged uses of `Target` are correct from the verifier?s perspective.
But the `clone()` method of `Object` does not return an instance of `Target` but of the `this`? class, `ClonedTarget` in the renamed clone. So this will fail with a `ClassCastException`, only when being executed.
---
This doesn?t preclude working use cases for a class with known content. But generally, it?s very fragile.