package de.robv.android.xposed.callbacks;

/** Compile-only stub. LSPosed supplies the real class at runtime. */
public abstract class XC_LoadPackage {
    public static final class LoadPackageParam {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
    }
}
