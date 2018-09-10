FROM jboss/wildfly:13.0.0.Final

MAINTAINER Robert Brem <brem_robert@hotmail.com>

ADD target/panthers.war /opt/jboss/wildfly/standalone/deployments/