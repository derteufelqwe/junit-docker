FROM openjdk:11

ADD build/libs/JUnitDocker-1.0-all.jar JUnitDocker.jar

CMD ["java", "-jar", "JUnitDocker.jar"]