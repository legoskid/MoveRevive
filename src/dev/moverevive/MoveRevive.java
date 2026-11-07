package dev.moverevive;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Rewrites Move's Graph API calls to an in-process responder. Same logic as frida/redirect.js. */
public final class MoveRevive implements IXposedHookLoadPackage {
    private static final String PACKAGE = "com.oculus.fitnesstracker";
    private static final Pattern GRAPHQL =
            Pattern.compile("^https://graph\\.oculus\\.com(/graphql[^?]*)(\\?.*)?$");

    private static Responder responder;

    private static synchronized int responderPort() throws IOException {
        if (responder == null) {
            responder = new Responder();
            XposedBridge.log("MoveRevive: responder on port " + responder.port());
        }
        return responder.port();
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!PACKAGE.equals(lpparam.packageName)) {
            return;
        }
        try {
            XposedHelpers.findAndHookMethod(
                    "com.facebook.react.modules.network.NetworkingModule",
                    lpparam.classLoader,
                    "sendRequest",
                    String.class, String.class, double.class,
                    "com.facebook.react.bridge.ReadableArray",
                    "com.facebook.react.bridge.ReadableMap",
                    String.class, boolean.class, double.class, boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Matcher match = GRAPHQL.matcher((String) param.args[1]);
                            if (match.matches()) {
                                param.args[1] = "http://127.0.0.1:" + responderPort() + match.group(1);
                            }
                        }
                    });
            XposedBridge.log("MoveRevive: hooked " + lpparam.processName);
        } catch (Throwable t) {
            XposedBridge.log("MoveRevive: hook failed: " + t);
        }
    }
}
