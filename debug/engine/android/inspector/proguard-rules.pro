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

-dontwarn !org.hapjs.**
-keep class !org.hapjs.** { *; }

-keep class org.hapjs.inspector.V8Inspector {
    public *;
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

-keepclassmembers class org.hapjs.inspector.V8Inspector {
    public static org.hapjs.inspector.V8Inspector getInstance();
    public void init(android.content.Context, java.lang.String);
    public void sendResponse(int, int, java.lang.String);
    public void sendNotification(int, int, java.lang.String);
    void runMessageLoopOnPause(int);
    void quitMessageLoopOnPause();
}
