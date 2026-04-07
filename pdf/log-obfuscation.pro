# ==================== Log 混淆规则 ====================
# 在发布版本中移除所有log相关代码和字符串
# 这个文件专门用于移除log，减少包大小并提高安全性

# 移除所有android.util.Log调用
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# 移除所有System.out.println调用（如果有的话）
-assumenosideeffects class java.lang.System {
    public static void out.println(...);
    public static void err.println(...);
}

# 移除所有printStackTrace调用（如果有的话）
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
    public void printStackTrace(java.io.PrintStream);
    public void printStackTrace(java.io.PrintWriter);
}
