FROM openjdk:8-slim

# set default build arguments
ARG SDK_VERSION=sdk-tools-linux-4333796.zip
ARG ANDROID_BUILD_VERSION=28
ARG ANDROID_TOOLS_VERSION=28.0.3
ARG NDK_VERSION=17c

# set default environment variables
ENV ADB_INSTALL_TIMEOUT=60
ENV ANDROID_HOME=/opt/android
ENV ANDROID_SDK_HOME=${ANDROID_HOME}
ENV PATH=${PATH}:${ANDROID_HOME}/emulator:${ANDROID_HOME}/tools:${ANDROID_HOME}/tools/bin:${ANDROID_HOME}/platform-tools
ENV ANDROID_NDK=/opt/ndk/android-ndk-r$NDK_VERSION
ENV ANDROID_NDK_HOME=/opt/ndk/android-ndk-r$NDK_VERSION
ENV PATH=${PATH}:${ANDROID_NDK}

# install system dependencies
RUN apt-get update -qq && apt-get install -qq -y --no-install-recommends \
        apt-transport-https \
        curl \
        file \
        git \
        gnupg2 \
        openjdk-8-jre \
        python \
        unzip \
    && rm -rf /var/lib/apt/lists/*;

# install nodejs and yarn packages from nodesource and yarn apt sources
RUN echo "deb https://deb.nodesource.com/node_12.x stretch main" > /etc/apt/sources.list.d/nodesource.list \
    && echo "deb https://dl.yarnpkg.com/debian/ stable main" > /etc/apt/sources.list.d/yarn.list \
    && curl -sS https://deb.nodesource.com/gpgkey/nodesource.gpg.key | apt-key add - \
    && curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add - \
    && apt-get update -qq \
    && apt-get install -qq -y --no-install-recommends nodejs yarn \
    && rm -rf /var/lib/apt/lists/*

# download and unpack NDK
RUN curl -sS https://dl.google.com/android/repository/android-ndk-r$NDK_VERSION-linux-x86_64.zip -o /tmp/ndk.zip \
    && mkdir /opt/ndk \
    && unzip -q -d /opt/ndk /tmp/ndk.zip \
    && rm /tmp/ndk.zip

# Full reference at https://dl.google.com/android/repository/repository2-1.xml
# download and unpack android
RUN curl -sS https://dl.google.com/android/repository/${SDK_VERSION} -o /tmp/sdk.zip \
    && mkdir /opt/android \
    && unzip -q -d /opt/android /tmp/sdk.zip \
    && rm /tmp/sdk.zip

RUN cd ~ && mkdir ~/.android && echo '### User Sources for Android SDK Manager' > ~/.android/repositories.cfg \
    && yes | sdkmanager --licenses && sdkmanager --update \
    && yes | sdkmanager "platform-tools" \
    "emulator" \
    "platforms;android-$ANDROID_BUILD_VERSION" \
    "build-tools;$ANDROID_TOOLS_VERSION" \
    "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.2" \
    "extras;m2repository;com;android;support;constraint;constraint-layout-solver;1.0.2" \
    "add-ons;addon-google_apis-google-23" \
    "system-images;android-19;google_apis;armeabi-v7a" \
    "extras;android;m2repository"
