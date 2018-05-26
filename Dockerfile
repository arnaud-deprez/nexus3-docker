FROM sonatype/nexus3:3.11.0

LABEL maintainer="Arnaud Deprez <arnaudeprez@gmail.com>"

USER root

ADD docker/docker-entrypoint.sh /docker-entrypoint.sh
ADD build/scripts/* /docker-entrypoint-init.d/

RUN chmod a+x /docker-entrypoint.sh &&\
    mkdir -p /home/nexus &&\
    chown -R nexus:nexus /home/nexus &&\
    usermod -d /home/nexus nexus

USER nexus

ENV ORCHESTRATION_ENABLED=false

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["run"]