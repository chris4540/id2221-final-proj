# Data Intensive Computing Final Project
> Chris & Mikołaj team

## Directory structure overview
* `.` - the root of the repository is a scala/sbt project; it also contains some additional files technically unrelated
to the JVM side of things
* `./build.sbt` - sbt build file for the scala spark application
* `./src/` - sources directory of the scala spark application
* `./reddit-client/` - directory containing the sources of a small reddit scraper that feeds the data straight to a 
kafka message queue
* `./init-data/` - directory for initialization data, right now mostly just grafana configuration files
* `./docker-compose.yml` - a docker-compose file that describes all the services needed to run this project

## Running
To start all the services just run
```bash
docker-compose up
```
If you don't have docker and/or docker-compose, install them.

If you make some changes, you may have to run
```bash
docker-compose build
```
to rebuild any docker images that need rebuilding.

To check if all the data is properly pushed from the python reddit scrapers to kafka, you can run
```bash
docker run -it --rm --network id2221-final-project_default confluentinc/cp-kafkacat kafkacat -b kafka:9094 -C -t <TOPIC>
```
where `<TOPIC>` is one of `comments`, `posts`. The command will start reading the given topic and printing the messages
into your terminal. The network name `id2221-final-project_default` depends on where your project is located in the
file system, you may need to change that in the command above.

To kill and remove any containers created by docker-compose, run
```bash
docker-compose down
```

To see the grafana dashboard, open [`http://localhost:3000`](http://localhost:3000) in your favorite web browser and log
in using `admin` for both username and password. The actual dashboard should be listed in the "Recently viewed 
dashboards" panel.