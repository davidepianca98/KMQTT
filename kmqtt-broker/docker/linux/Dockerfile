FROM ubuntu:18.04
COPY build/bin/linuxX64/releaseExecutable/kmqtt.kexe /bin/
COPY keyStore.p12 /bin/

RUN chmod +x /bin/kmqtt.kexe

ENTRYPOINT ["/bin/kmqtt.kexe", "-p", "8883", "--key-store", "/bin/keyStore.p12", "--key-store-psw", "changeit"]
