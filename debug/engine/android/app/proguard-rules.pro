-verbose

-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-dontshrink

-keepnames class * implements org.hapjs.bridge.Extention {
}

-keepnames class * implements org.hapjs.component.Component {
}

-keep class * extends com.eclipsesource.v8.V8Object {
    public *;
}

-keepclassmembers class org.hapjs.inspector.V8Inspector {
    public *;
    void runMessageLoopOnPause(int);
    void quitMessageLoopOnPause();
    native <methods>;
}

-keep class org.hapjs.inspector.reflect.* {
    native <methods>;
}

-keep class * implements com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain {
    @com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod public *;
}

-keepclassmembers class ** {
    void <init>();
    @com.facebook.stetho.json.annotation.JsonProperty public *;
    @com.facebook.stetho.json.annotation.JsonValue public *;
}

-dontwarn !org.hapjs.debug.core.**, **
-keep class !org.hapjs.debug.core.** { *; }