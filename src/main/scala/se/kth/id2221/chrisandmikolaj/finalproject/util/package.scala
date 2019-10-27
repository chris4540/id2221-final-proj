package se.kth.id2221.chrisandmikolaj.finalproject

import java.util.concurrent.ConcurrentHashMap

import com.paulgoldbaum.influxdbclient.InfluxDB
import com.paulgoldbaum.influxdbclient.Parameter.Consistency.Consistency
import com.paulgoldbaum.influxdbclient.Parameter.Precision.Precision
import org.apache.spark.streaming.dstream.DStream
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success}

package object util {
  def connectInfluxDb(conf: InfluxConfig): InfluxDB = {
    val InfluxConfig(host, port, username, password) = conf
    InfluxDB.connect(host, port, username, password)(ExecutionContext.global)
  }

  private val clients: ConcurrentHashMap[InfluxConfig, InfluxDB] = new ConcurrentHashMap[InfluxConfig, InfluxDB]()

  sys.addShutdownHook {
    clients.forEach { (_, client) =>
      client.close()
    }
  }

  def getOrCreateInfluxDb(conf: InfluxConfig): InfluxDB = {
    clients.computeIfAbsent(conf, connectInfluxDb(_))
  }

  private val logger = LoggerFactory.getLogger(classOf[InfluxMethods[Any]])

  implicit class InfluxMethods[T](private val stream: DStream[T]) extends AnyVal {
    def saveToInfluxDb(databaseName: String, measurementName: String, precision: Precision = null,
                       consistency: Consistency = null, retentionPolicy: String = null)
                      (implicit config: InfluxConfig, toPoint: ToPoint[T]): Unit = {
      stream.foreachRDD { (rdd, time) =>
        val timeNanoseconds = time.milliseconds * 1000000L
        rdd.foreachPartition { values =>
          val influxDb = getOrCreateInfluxDb(config)
          val database = influxDb.selectDatabase(databaseName)
          implicit val ec: ExecutionContextExecutor = ExecutionContext.global
          database.bulkWrite(values.map(toPoint(_, measurementName).copy(timestamp = timeNanoseconds)).toSeq, precision, consistency, retentionPolicy)
            .onComplete {
              case Failure(exception) =>
                logger.error("Exception when writing to InfluxDB", exception)
              case Success(value) =>
                logger.debug("Write to InfluxDB successful!")
            }
        }
      }
    }
  }

}
