package de.robv.android.xposed;

/** Compile-only stub. LSPosed supplies the real class at runtime. */
public abstract class XC_MethodHook {
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
    }

    public static final class MethodHookParam {
        public Object[] args;
    }

    public class Unhook {
    }
}
