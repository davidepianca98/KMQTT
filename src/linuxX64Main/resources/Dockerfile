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
        gcc-aarch64-linux-gnu \
    && rm -rf /var/lib/apt/lists/*

###############################################################################
#                                INSTALLATION
###############################################################################

### Some env variables
ENV OPENSSL_VERSION="1.1.1k"
ENV KOTLIN_VERSION2="1.5.0"
ENV KOTLIN_VERSION="1.5.0"

RUN set -x \
 && wget --no-check-certificate -O /tmp/kotlin-native-linux-${KOTLIN_VERSION}.tar.gz "https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VERSION2}/kotlin-native-linux-${KOTLIN_VERSION}.tar.gz" \
 && cd /tmp \
 && tar -xzvf kotlin-native-linux-${KOTLIN_VERSION}.tar.gz

RUN set -x \
 && /tmp/kotlin-native-linux-${KOTLIN_VERSION}/bin/cinterop -def openssl.def; exit 0

RUN set -x \
 ### BUILD OpenSSL
 && wget --no-check-certificate -O /tmp/openssl-${OPENSSL_VERSION}.tar.gz "https://www.openssl.org/source/openssl-${OPENSSL_VERSION}.tar.gz" \
 && mkdir /tmp/linuxX64 \
 && mkdir /tmp/mingwX64 \
 && mkdir /tmp/linuxArm32Hfp \
 && mkdir /tmp/linuxArm64 \
 && tar -xvf /tmp/openssl-${OPENSSL_VERSION}.tar.gz -C /tmp/linuxX64/ \
 && tar -xvf /tmp/openssl-${OPENSSL_VERSION}.tar.gz -C /tmp/mingwX64/ \
 && tar -xvf /tmp/openssl-${OPENSSL_VERSION}.tar.gz -C /tmp/linuxArm32Hfp/ \
 && tar -xvf /tmp/openssl-${OPENSSL_VERSION}.tar.gz -C /tmp/linuxArm64/ \
 && rm -rf /tmp/openssl-${OPENSSL_VERSION}.tar.gz

RUN set -x \
 && cd /tmp/linuxX64/openssl-${OPENSSL_VERSION} \
 && ./Configure --prefix=$PWD/dist no-idea no-mdc2 no-rc5 no-shared linux-x86_64 \
 && make \
 && ar -x libssl.a \
 && ar -x libcrypto.a \
 && ar -qc libopenssl.a *.o

#RUN set -x \
# && cd /tmp/mingwX64/openssl-${OPENSSL_VERSION} \
# && ./Configure --cross-compile-prefix=x86_64-w64-mingw32- no-idea no-mdc2 no-rc5 no-shared mingw64 \
# && make

RUN set -x \
 && cd /tmp/linuxArm32Hfp/openssl-${OPENSSL_VERSION} \
 && ./Configure no-idea no-mdc2 no-rc5 no-shared no-asm --cross-compile-prefix=arm-linux-gnueabihf- linux-armv4 \
 && make \
 && ar -x libssl.a \
 && ar -x libcrypto.a \
 && ar -qc libopenssl.a *.o

RUN set -x \
 && cd /tmp/linuxArm64/openssl-${OPENSSL_VERSION} \
 && ./Configure no-shared no-asm --cross-compile-prefix=aarch64-linux-gnu- linux-aarch64 \
 && make \
 && ar -x libssl.a \
 && ar -x libcrypto.a \
 && ar -qc libopenssl.a *.o

COPY openssl.def /tmp/

RUN set -x \
 && cd /tmp \
 && mkdir klib_linux_x64 \
 && cd /tmp/klib_linux_x64 \
 && ../kotlin-native-linux-${KOTLIN_VERSION}/bin/cinterop -def ../openssl.def -target "linux_x64" -o openssl

#RUN set -x \
# && cd /tmp \
# && mkdir klib_mingw_x64 \
# && cd /tmp/klib_mingw_x64 \
# && ../kotlin-native-linux-${KOTLIN_VERSION}/bin/cinterop -def ../openssl.def -target "mingw_x64" -o openssl

RUN set -x \
 && cd /tmp \
 && mkdir klib_linux_arm32_hfp \
 && cd /tmp/klib_linux_arm32_hfp \
 && ../kotlin-native-linux-${KOTLIN_VERSION}/bin/cinterop -def ../openssl.def -target "linux_arm32_hfp" -o openssl

RUN set -x \
 && cd /tmp \
 && mkdir klib_linux_arm64 \
 && cd /tmp/klib_linux_arm64 \
 && ../kotlin-native-linux-${KOTLIN_VERSION}/bin/cinterop -def ../openssl.def -target "linux_arm64" -o openssl
