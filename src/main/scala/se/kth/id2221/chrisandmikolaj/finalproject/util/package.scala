package se.kth.id2221.chrisandmikolaj.finalproject

import java.util.concurrent.ConcurrentHashMap

import com.paulgoldbaum.influxdbclient.Parameter.Consistency.Consistency
import com.paulgoldbaum.influxdbclient.Parameter.Precision.Precision
import com.paulgoldbaum.influxdbclient.{InfluxDB, Point}
import org.apache.spark.streaming.dstream.DStream

import scala.concurrent.ExecutionContext

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

  implicit class InfluxMethods[T](private val stream: DStream[T]) extends AnyVal {
    def saveToInfluxDb(databaseName: String, measurementName: String, precision: Precision = null,
                       consistency: Consistency = null, retentionPolicy: String = null)
                      (implicit config: InfluxConfig, toPoint: ToPoint[T]): Unit = {
      stream.foreachRDD { rdd =>
        rdd.foreachPartition { values =>
          val influxDb = getOrCreateInfluxDb(config)
          val database = influxDb.selectDatabase(databaseName)
          database.bulkWrite(values.map(toPoint(_, measurementName)).toSeq, precision, consistency, retentionPolicy)
        }
      }
    }
  }
}
