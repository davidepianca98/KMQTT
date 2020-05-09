FROM ubuntu:18.04
COPY build/bin/linuxX64/releaseExecutable/KMQTT.kexe /bin/
COPY keyStore.p12 /bin/

RUN chmod +x /bin/KMQTT.kexe

ENTRYPOINT ["/bin/KMQTT.kexe", "-p", "8883", "--key-store", "/bin/keyStore.p12", "--key-store-psw", "changeit"]
