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

-keep class !org.hapjs.** { *; }

-keep class * extends org.hapjs.bridge.MetaDataSet
-keep class * extends org.hapjs.bridge.AbstractExtension
-keep class * extends org.hapjs.component.Component {
    public <init>(...);
}
-keep class * extends org.hapjs.event.EventTargetDataSet
-keep class * implements org.hapjs.event.EventTarget
-keep class * implements org.hapjs.event.Event
-keep,allowobfuscation @interface org.hapjs.bridge.annotation.DependencyAnnotation
-keep @org.hapjs.bridge.annotation.DependencyAnnotation class *

-keep class * extends com.eclipsesource.v8.V8Object {
    public *;
}

# keep for inspector reference: start
-keep class org.hapjs.runtime.HapEngine {
    public *;
}
-keep class org.hapjs.bridge.HybridView {
    public *;
}
-keep class org.hapjs.bridge.Widget {
    public *;
}
-keep class org.hapjs.component.Component {
    public *;
}
-keep class org.hapjs.component.constants.Attributes {
    public *;
}
-keep class org.hapjs.component.view.state.State {
    public *;
}
-keep class org.hapjs.render.jsruntime.JsThread {
    public *;
}
-keep class org.hapjs.render.jsruntime.JsContext {
    public *;
}
-keep class org.hapjs.render.Page {
    public *;
}
-keep class org.hapjs.render.PageManager {
    public *;
}
-keep class org.hapjs.render.RootView {
    public *;
}
-keep class org.hapjs.render.VDomChangeAction {
    public *;
}
-keep class org.hapjs.render.vdom.VDocument {
    public *;
}
-keep class org.hapjs.render.vdom.VElement {
    public *;
}
-keep class org.hapjs.render.vdom.VGroup {
    public *;
}
-keep class org.hapjs.runtime.inspect.InspectorManager {
    public *;
}
-keep class org.hapjs.runtime.inspect.InspectorProvider {
    public *;
}
-keep class org.hapjs.runtime.ProviderManager {
    public *;
}
-keep class org.hapjs.runtime.RuntimeActivity {
    public *;
}
-keep class org.hapjs.common.utils.DisplayUtil {
    public *;
}
-keep interface org.hapjs.runtime.inspect.protocols.IDOMStorage {
    public *;
}
-keep class org.hapjs.features.storage.data.DOMStorageImpl {
    public *;
}
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
# end

-keepclassmembers class * extends android.webkit.WebChromeClient{
    public void openFileChooser(...);
}

# card
-keep class org.hapjs.card.api.** { *; }

-keep class org.hapjs.card.support.impl.** {
    public *;
}

-keep class org.hapjs.card.support.CardView** {
    public *;
}

# event bus
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

-dontwarn org.hapjs.card.**

# this dontwarn must be the last dontwarn
-dontwarn !org.hapjs.**, **
