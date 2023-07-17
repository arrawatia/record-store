# EKS (amazonlinux) version:
# --build-arg FDBVERSION=7.1.10-${OKTETO_USER}-debug \
# --build-arg FDBREPO=${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/foundationdb \
# --build-arg JDKPKG=java-11-amazon-corretto-11.0.13+8-1.amzn2

ARG FDBVERSION=7.1.10
ARG FDBREPO=foundationdb

FROM ${FDBREPO}/foundationdb:${FDBVERSION} AS java

ARG JDKPKG=java-11-openjdk-11.0.13.0.8-1.el7_9

RUN yum -y install ${JDKPKG} && yum clean all && rm -rf /var/cache/yum

FROM java AS standalone

ARG RECORD_STORE_VERSION=1.0.0-SNAPSHOT
RUN mkdir /var/config

COPY record-store/build/libs/record-store-${RECORD_STORE_VERSION}-all.jar record-store-all.jar

ENTRYPOINT ["java", "-Dlog4j.configurationFile=log4j2.xml", "-jar", "record-store-all.jar", "-conf", "/var/config/config.json"]
