package se.kth.id2221.chrisandmikolaj.finalproject

import java.time.Instant

import com.github.fsanaulla.chronicler.core.model.{InfluxCredentials, InfluxWriter, Point}
import com.github.fsanaulla.chronicler.spark.core.CallbackHandler
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Minutes, Seconds, StreamingContext}
import org.json4s.jackson.JsonMethods
import org.json4s.DefaultReaders._
import com.github.fsanaulla.chronicler.spark.streaming._
import com.github.fsanaulla.chronicler.urlhttp.shared.InfluxConfig

import scala.util.Try
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.dstream.DStream

/*
 * Objective:
 *  1. current popular post
 *  2. active user
 *  3. comment rates
 *  4. the trend of interesting topics and/or user
 *  5. type of posted content statistics
 *  6. Write the streams into database
 */

object Main {
  def main(args: Array[String]): Unit = {

    val conf = new SparkConf().setMaster("local[*]").setAppName("id2221-chris-and-mikolaj-final-project")
    val ssc = new StreamingContext(conf, Seconds(1))

    implicit val influxConfig: InfluxConfig = InfluxConfig(
      host = sys.env.getOrElse("INFLUXDB_HOST", "influxdb"),
      port = sys.env.getOrElse("INFLUXDB_PORT", "8086").toInt,
      credentials = Some(InfluxCredentials(username = "admin", password = "admin")),
      compress = false
    )

    val commentStream = KafkaUtils.createDirectStream[String, String](
      ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](Seq("comments"), mkKafkaParams("comment-stream"))
    )

    val postStream = KafkaUtils.createDirectStream[String, String](
      ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](Seq("posts"), mkKafkaParams("post-stream"))
    )

    val jsonPostStream = postStream.map(record => JsonMethods.parse(record.value()))
    val jsonCommentStream = commentStream.map(record => JsonMethods.parse(record.value()))
    // -----------------------------------------------------------------------
    // 2. active user
    // The most active 10 users in a period of time (comment + post)
    // var activePostUserStream = jsonPostStream
    // .map(json => ((json \ "author").as[String], 1))

    // var activeCommentUserStream = jsonCommentStream
    // .map(json => ((json \ "author").as[String], 1))

    // var activeUserStream = activePostUserStream.fullOuterJoin(activeCommentUserStream)
    // .map{case (s:String, (a:Option[Int], b:Option[Int])) => (s, a.getOrElse(0) + b.getOrElse(0))}
    // .reduceByKeyAndWindow((a:Int, b:Int) => (a + b), Seconds(60), Seconds(10))
    // .transform(rdd => {
    //   // This is a bit complicated. as transform required to return RDD
    //   // We use takeOrdered to have a list and filter RDDs of this window
    //   // of DStream
    //   val arr = rdd.takeOrdered(10)(Ordering[Int].reverse.on(x => x._2))
    //   rdd.filter(arr.contains)
    // })

    // println("-----------------------")
    // println("Top 10 active users")
    // activeUserStream.print()
    // println("-----------------------")
    // ---------------------------------------------------------------------------------------
    // 3. Comment rate according processing time
    // /**
    //  * Save the discrete stree to influx database
    //  *
    //  * @param stream The stream
    //  * @param measurementName
    //  */
    // def saveRatesToInflux(stream: DStream[ConsumerRecord[String, String]], measurementName: String): Unit =
    //   stream
    //     .map { record =>
    //       JsonMethods.parse(record.value())
    //     }
    //     .map { json =>   // create key-value pairs
    //       (json \ "subreddit_name_prefixed").as[String] -> json
    //     }
    //     .groupByKeyAndWindow(Seconds(10), Seconds(10))
    //     .map { case (subreddit, jsons) =>
    //       val nanos = Instant.now().toEpochMilli * 1000000
    //       RedditRate(measurementName, subreddit, jsons.size, nanos)
    //     }
    //     .saveToInfluxDB("reddit_stats", measurementName, ch = Some(CallbackHandler(
    //       { _ => println("pushed to influx") },
    //       { e => e.printStackTrace() },
    //       { e => e.printStackTrace() }
    //     )))
    // saveRatesToInflux(commentStream, "comment_rate")
    // saveRatesToInflux(postStream, "post_rate")
    // -------------------------------------------------------------------------
    // 5. type of posted content statistics
    var titlePostTypeStream = jsonPostStream
    .map(json => {
      val k: String = (json \ "title").as[String]
      val is_video: Boolean = (json \ "is_video").as[Boolean]
      (k, is_video)
    })
    .map{case(k, is_video) => (k, if (is_video) "video" else "text")}

    var windowCountPerMinutes = titlePostTypeStream
    .map{case (k, v) => (v, 1)}
    .reduceByKeyAndWindow((a:Int, b:Int) => (a + b), Seconds(60), Seconds(10))
    windowCountPerMinutes.print()
    // -------------------------------------------------------------------------

    ssc.start()
    ssc.awaitTermination()
  }

  def mkKafkaParams(groupId: String): Map[String, AnyRef] = Map[String, AnyRef](
    "bootstrap.servers" -> sys.env.getOrElse("KAFKA_URL", "kafka:9092"),
    "key.deserializer" -> classOf[StringDeserializer],
    "value.deserializer" -> classOf[StringDeserializer],
    "group.id" -> groupId,
    "auto.offset.reset" -> "latest",
    "enable.auto.commit" -> (true: java.lang.Boolean)
  )
}

final case class RedditRate(measurementName: String, subreddit: String, rate: Int, timestamp: Long)

object RedditRate {
  implicit val writer: InfluxWriter[RedditRate] = { rate =>
    // TODO: change InfluxDB library, because this one kinda sucks - I have to write stuff like the line below
    Right(s"subreddit=${rate.subreddit} rate=${rate.rate} ${rate.timestamp}")
  }
}

