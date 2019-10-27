FROM mozilla/sbt:8u212_1.2.8

WORKDIR /project

COPY . .
RUN sbt package

ENTRYPOINT sbt run