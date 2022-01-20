# copy from android-26 org.json source code

因为 android 4.4 JSONObject 的实现没有使用 LinkedHashMap, 解析 JS 发送的 css 样式表时顺序不对, 导致后声明的样式无法覆盖先声明的样式.

