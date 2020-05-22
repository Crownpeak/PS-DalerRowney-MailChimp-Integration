FROM tomcat:8.0-jre8
ARG WAR_FILE
COPY tomcat-users.xml /usr/local/tomcat/conf/tomcat-users.xml
COPY ${WAR_FILE} /usr/local/tomcat/webapps/mailchimp-signup.war

