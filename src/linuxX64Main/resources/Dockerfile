FROM ubuntu:18.04

####################################################
########               GCC   and tools          ###########
####################################################
# The GNU Compiler Collection 5.3.0-r0

RUN set -x \
    && apt-get update \
    && apt-get -y install \
        openjdk-8-jdk \
        bash \
		wget \
        gcc \
        tar \
        perl \
        make \
        mingw-w64 \
        gcc-arm-linux-gnueabihf \
    && rm -rf /var/lib/apt/lists/*

###############################################################################
#                                INSTALLATION
###############################################################################

### Some env variables
### I don't know how to get the latest version of OpenSSL, like openssl-latest.tar.gz
### So I have to put the version here
ENV OPENSSL_VERSION="1.1.1g"

RUN set -x \
 && wget --no-check-certificate -O /tmp/kotlin-native-linux-1.3.72.tar.gz "https://github.com/JetBrains/kotlin/releases/download/v1.3.72/kotlin-native-linux-1.3.72.tar.gz" \
 && cd /tmp \
 && tar -xzvf kotlin-native-linux-1.3.72.tar.gz

RUN set -x \
 && /tmp/kotlin-native-linux-1.3.72/bin/cinterop -def openssl.def; exit 0

RUN set -x \
 ### BUILD OpenSSL
 && wget --no-check-certificate -O /tmp/openssl-${OPENSSL_VERSION}.tar.gz "https://www.openssl.org/source/openssl-${OPENSSL_VERSION}.tar.gz" \
 && mkdir /tmp/linuxX64 \
 && mkdir /tmp/mingwX64 \
 && mkdir /tmp/linuxArm32Hfp \
 && tar -xvf /tmp/openssl-${OPENSSL_VERSION}.tar.gz -C /tmp/linuxX64/ \
 && tar -xvf /tmp/openssl-${OPENSSL_VERSION}.tar.gz -C /tmp/mingwX64/ \
 && tar -xvf /tmp/openssl-${OPENSSL_VERSION}.tar.gz -C /tmp/linuxArm32Hfp/ \
 && rm -rf /tmp/openssl-${OPENSSL_VERSION}.tar.gz

RUN set -x \
 && cd /tmp/linuxX64/openssl-${OPENSSL_VERSION} \
 && ./Configure --prefix=$PWD/dist no-idea no-mdc2 no-rc5 no-shared linux-x86_64 \
 && make

RUN set -x \
 && cd /tmp/mingwX64/openssl-${OPENSSL_VERSION} \
 && ./Configure --cross-compile-prefix=x86_64-w64-mingw32- no-idea no-mdc2 no-rc5 no-shared mingw64 \
 && make

RUN set -x \
 && cd /tmp/linuxArm32Hfp/openssl-${OPENSSL_VERSION} \
 && ./Configure no-idea no-mdc2 no-rc5 no-shared no-asm --cross-compile-prefix=arm-linux-gnueabihf- linux-armv4 \
 && make

COPY openssl.def /tmp/

RUN set -x \
 && cd /tmp \
 && mkdir klib_linux_x64 \
 && cd /tmp/klib_linux_x64 \
 && ../kotlin-native-linux-1.3.72/bin/cinterop -def ../openssl.def -target "linux_x64" -o openssl

#RUN set -x \
# && cd /tmp \
# && mkdir klib_mingw_x64 \
# && cd /tmp/klib_mingw_x64 \
# && ../kotlin-native-linux-1.3.72/bin/cinterop -def ../openssl.def -target "mingw_x64" -o openssl

RUN set -x \
 && cd /tmp \
 && mkdir klib_linux_arm32_hfp \
 && cd /tmp/klib_linux_arm32_hfp \
 && ../kotlin-native-linux-1.3.72/bin/cinterop -def ../openssl.def -target "linux_arm32_hfp" -o openssl
