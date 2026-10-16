# APIBalance R8 混淆规则
# 保留清单：Android 组件、Fragment、以及需要在运行时按类名被系统实例化的类型。
# 其余辅助类的类名 / 字段名 / 方法名全部会被重命名（见 mapping.txt，即混淆表）。

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions
-keepattributes SourceFile,LineNumberTable

# 入口组件必须保持原类名，否则系统无法启动
-keep class asia.rxh.apibalance.MainActivity { *; }
-keep class asia.rxh.apibalance.CrashActivity { *; }
-keep class asia.rxh.apibalance.OnboardingActivity { *; }
-keep class asia.rxh.apibalance.FloatingWindowService { *; }
-keep class asia.rxh.apibalance.AutoRefreshReceiver { *; }
-keep class asia.rxh.apibalance.BalanceWidgetProvider { *; }

# Fragment 由框架创建，保持类名与无参构造
-keep class asia.rxh.apibalance.*Fragment { *; }

# 系统基类兜底
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Fragment
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.appwidget.AppWidgetProvider

# 资源 id 与枚举、Parcelable
-keep class asia.rxh.apibalance.R$* { *; }
-keepclassmembers enum * { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 内部私有构造的常量类不参与裁剪
-keepclassmembers class asia.rxh.apibalance.ProviderConfig { *; }
-keepclassmembers class asia.rxh.apibalance.PeakOffpeak { *; }

# 强化混淆：类名并入统一包，允许修改访问权限
-repackageclasses 'asia.rxh.a'
-allowaccessmodification
-overloadaggressively
-optimizationpasses 5
-dontpreverify

# 使用形近字符字典，人工逆向时阅读成本显著提高
-obfuscationdictionary obfuscation-dict.txt
-classobfuscationdictionary obfuscation-dict.txt
-packageobfuscationdictionary obfuscation-dict.txt

# 保留必要的异常与注解信息
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
