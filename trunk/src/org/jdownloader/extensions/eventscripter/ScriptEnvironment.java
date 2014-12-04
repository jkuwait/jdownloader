package org.jdownloader.extensions.eventscripter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

import javax.swing.JTextPane;

import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.NativeJSON;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.jackson.JacksonMapper;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OSFamily;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.appwork.utils.processes.ProcessOutput;
import org.appwork.utils.reflection.Clazz;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

public class ScriptEnvironment {
    @GlobalField(description = "JDownloader Installation Directory")
    public static String JD_HOME = Application.getResource("").getAbsolutePath();

    @GlobalField(description = "Call the MyJDownloader API", parameters = { "\"namespace\"", "\"methodname\"", "parameter1", "parameter2", "..." }, example = "callAPI(\"downloadsV2\", \"queryLinks\", { \"name\": true})")
    public static Object callAPI(String namespace, String method, Object... parameters) throws EnvironmentException {
        String js;
        try {
            Object ret = RemoteAPIController.getInstance().call(namespace, method, parameters);
            final ScriptThread env = getScriptThread();
            // convert to javascript object
            js = "(function(){ return " + JSonStorage.serializeToJson(ret) + ";}());";
            Object retObject = env.evalTrusted(js);

            return retObject;
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @GlobalField(description = "Show a Message Box", parameters = { "myObject1", "MyObject2", "..." }, example = "alert(JD_HOME);")
    public static void alert(Object... objects) {
        final ScriptThread env = getScriptThread();
        if (objects.length == 1) {
            if (Clazz.isPrimitiveWrapper(objects[0].getClass()) || Clazz.isString(objects[0].getClass())) {
                showMessageDialog(objects[0] + "");
                return;
            } else {
                try {
                    showMessageDialog(new JacksonMapper().objectToString(objects[0]));
                } catch (Throwable e) {

                    showMessageDialog(format(NativeJSON.stringify(env.getContext(), env.getScope(), objects[0], null, null) + ""));
                }
                return;
            }
        }
        try {
            showMessageDialog(new JacksonMapper().objectToString(objects));
        } catch (Throwable e) {

            showMessageDialog(format(NativeJSON.stringify(env.getContext(), env.getScope(), objects, null, null) + ""));
        }
        return;
    }

    private static String format(String js) {
        final ScriptThread env = getScriptThread();
        try {
            env.ensureLibrary("js_beautifier.js");
            String parametername;
            ScriptableObject.putProperty(env.getScope(), parametername = "text_" + System.currentTimeMillis(), js);

            String ret = env.evalTrusted("js_beautify(" + parametername + ", {   });") + "";
            return ret;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return js;
    }

    private static void showMessageDialog(String string) {
        final ScriptThread env = getScriptThread();

        UIOManager.I().show(ConfirmDialogInterface.class, new ConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL | ConfirmDialog.STYLE_SCROLLPANE, T._.showMessageDialog_title(env.getScript().getName(), env.getScript().getEventTrigger().getLabel()), string, new AbstractIcon(IconKey.ICON_INFO, 32), null, null) {
            @Override
            protected void modifyTextPane(JTextPane textField) {

            }

            @Override
            public void pack() {
                this.getDialog().pack();
            }

            @Override
            protected int getPreferredWidth() {
                return 600;
            }
        });
    }

    @GlobalField(description = "Loads a Javascript file or url. ATTENTION. The loaded script can access the API as well.", parameters = { "myFilePathOrUrl" }, example = "require(\"https://raw.githubusercontent.com/douglascrockford/JSON-js/master/json.js\");")
    public static void require(String fileOrUrl) throws EnvironmentException {
        try {
            getScriptThread().requireJavascript(fileOrUrl);
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @GlobalField(description = "Call a local Process asynchronous", parameters = { "\"myCallBackFunction\"|null", "\"commandline1\"", "\"commandline2\"", "\"...\"" }, example = "callAsync(function(exitCode,stdOut,errOut){ alert(\"Closed Notepad\");},\"notepad.exe\",JD_HOME+\"\\\\license.txt\");")
    public static void callAsync(final Function callback, final String... commands) throws EnvironmentException {
        try {
            final ScriptThread env = getScriptThread();
            if (commands != null && commands.length > 0) {
                new ScriptReferenceThread(env) {
                    @Override
                    public void run() {
                        try {
                            try {
                                ProcessOutput ret = ProcessBuilderFactory.runCommand(commands);

                                if (callback != null) {

                                    if (CrossSystem.getOSFamily() == OSFamily.WINDOWS) {
                                        executeCallback(callback, ret.getExitCode(), new String(new String(ret.getStdOutData(), "cp850").getBytes("UTF-8"), "UTF-8"), new String(new String(ret.getErrOutData(), "cp850").getBytes("UTF-8"), "UTF-8"));
                                    } else {
                                        executeCallback(callback, ret.getExitCode(), new String(ret.getStdOutData(), "UTF-8"), new String(ret.getErrOutData(), "UTF-8"));

                                    }
                                }
                            } catch (IOException e) {

                                if (callback != null) {
                                    executeCallback(callback, -1, null, Exceptions.getStackTrace(e));

                                }
                                env.getLogger().log(e);
                                env.notifyAboutException(e);
                            }
                        } catch (Throwable e) {
                            env.notifyAboutException(e);
                        }

                    }
                }.start();

            }
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    private static ScriptThread getScriptThread() {
        Thread ct = Thread.currentThread();
        if (ct instanceof ScriptThread) {
            return (ScriptThread) ct;
        } else if (ct instanceof ScriptReferenceThread) {
            return ((ScriptReferenceThread) ct).getScriptThread();
        } else {
            throw new IllegalStateException();
        }

    }

    @GlobalField(description = "Call a local Process. Blocks Until the process returns", parameters = { "\"commandline1\"", "\"commandline2\"", "\"...\"" }, example = "var pingResult = callSync(\"ping\",\"jdownloader.org\");")
    public static String callSync(final String... commands) throws EnvironmentException {
        try {
            ProcessBuilder pb = ProcessBuilderFactory.create(commands);
            pb.redirectErrorStream();
            ProcessOutput ret = ProcessBuilderFactory.runCommand(pb);
            if (CrossSystem.getOSFamily() == OSFamily.WINDOWS) {
                return new String(new String(ret.getStdOutData(), "cp850").getBytes("UTF-8"), "UTF-8");
            } else {
                return ret.getStdOutString("UTF-8");

            }

        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    public static String getAPIDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("// ========= Global Methods =========\r\n");
        for (Method f : ScriptEnvironment.class.getDeclaredMethods()) {
            GlobalField ann = f.getAnnotation(GlobalField.class);
            if (ann != null) {
                sb.append("//").append(ann.description()).append("\r\n");
                if (!Clazz.isVoid(f.getReturnType())) {
                    sb.append("// var my").append(f.getReturnType().getSimpleName().substring(0, 1).toUpperCase(Locale.ENGLISH)).append(f.getReturnType().getSimpleName().substring(1)).append(" = ");

                } else {
                    sb.append("// ");
                }
                sb.append(f.getName()).append("(");

                boolean first = true;
                if (ann.parameters() != null) {
                    for (String s : ann.parameters()) {
                        if (!first) {
                            sb.append(", ");
                        }
                        sb.append(s);
                        first = false;

                    }
                }
                sb.append(");\r\n");
                if (StringUtils.isNotEmpty(ann.example())) {
                    sb.append(ann.example()).append("\r\n");
                }

            }
        }
        sb.append("// ========= Global Properties =========\r\n");
        for (Field f : ScriptEnvironment.class.getDeclaredFields()) {
            GlobalField ann = f.getAnnotation(GlobalField.class);
            if (ann != null) {
                sb.append("var my").append(f.getType().getSimpleName().substring(0, 1).toUpperCase(Locale.ENGLISH)).append(f.getType().getSimpleName().substring(1)).append(" = ");

                sb.append(ann.description()).append(";\r\n");
                if (StringUtils.isNotEmpty(ann.example())) {
                    sb.append(ann.example()).append("\r\n");
                }
            }
        }

        return sb.toString();
    }
}