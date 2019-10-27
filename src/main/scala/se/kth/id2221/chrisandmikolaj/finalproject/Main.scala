package se.kth.id2221.chrisandmikolaj.finalproject

import ch.qos.logback.classic.Logger
import com.paulgoldbaum.influxdbclient._
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Minutes, Seconds, StreamingContext}
import org.json4s.DefaultReaders._
import org.json4s.JValue
import org.json4s.jackson.JsonMethods
import org.slf4j.LoggerFactory
import se.kth.id2221.chrisandmikolaj.finalproject.util._
import se.kth.id2221.chrisandmikolaj.finalproject.data._

import scala.util.Try

/*
 * Objective:
 *  1. current popular post [Done]
 *  2. active user [Done]
 *  3. comment rates [Done]
 *  4. the trend of interesting topics and/or user [Deleted; Hard to do or find replacement]
 *  5. type of posted content statistics [Done]
 *  6. Write the streams into database
 *  TODO: Check item 4
 */

object Main {
  def main(args: Array[String]): Unit = {
    val logger = LoggerFactory.getLogger(Main.getClass)

    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("id2221-chris-and-mikolaj-final-project")
    val ssc = new StreamingContext(conf, Seconds(1))

    implicit val influxConfig: InfluxConfig = InfluxConfig.fromEnv
    val database = "reddit_stats"

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

    @inline def getSubreddit(postOrCommentJson: JValue): String =
      (postOrCommentJson \ "subreddit_name_prefixed").as[String]

    // -----------------------------------------------------------------------
    // 1. Topic trend according to comment permalink count; Count Last 30 mins, update every 30s
    jsonCommentStream
      .map { json =>
        val permalink = (json \ "link_permalink").as[String]
        val subreddit = getSubreddit(json)
        permalink -> TrendingPost(permalink, subreddit, 1)
      }
      .reduceByKeyAndWindow((a: TrendingPost, b: TrendingPost) => a + b, Minutes(30), Seconds(30))
      .map(_._2)
      .saveToInfluxDb(database, "trending_post")

    // -----------------------------------------------------------------------
    // 2. active user
    // The most active 10 users in a period of time (comment + post)
    val extractActivity = { json: JValue =>
      val username = (json \ "author").as[String]
      username -> ActiveUser(username, 1)
    }

    val activePostUserStream = jsonPostStream.map(extractActivity)
    val activeCommentUserStream = jsonCommentStream.map(extractActivity)

    activePostUserStream.union(activeCommentUserStream)
      .reduceByKeyAndWindow((a: ActiveUser, b: ActiveUser) => a + b, Minutes(30), Seconds(10))
      .map(_._2)
      .saveToInfluxDb(database, "active_user")

    // ---------------------------------------------------------------------------------------
    // 3. Comment rate according processing time
    def saveRatesToInflux(stream: DStream[ConsumerRecord[String, String]], measurementName: String): Unit =
      stream
        .map { record =>
          JsonMethods.parse(record.value())
        }
        .map { json =>
          getSubreddit(json) -> json
        }
        .groupByKeyAndWindow(Minutes(1), Seconds(30))
        .map { case (subreddit, jsons) =>
          RedditRate(subreddit, jsons.size)
        }
        .saveToInfluxDb(database, measurementName)

    saveRatesToInflux(commentStream, "comment_rate")
    saveRatesToInflux(postStream, "post_rate")

    // -------------------------------------------------------------------------
    // 5. type of posted content statistics
    jsonPostStream
      .map(json => {
        val isVideo = Try((json \ "is_video").as[Boolean]).getOrElse(false)
        val isSelf = Try((json \ "is_self").as[Boolean]).getOrElse(false)
        var typeHint = Try((json \ "post_hint").as[String]).getOrElse(null)
        if (typeHint == "hosted:video" || typeHint == "rich:video") typeHint = "video"
        val typ = if (typeHint != null) typeHint else if (isSelf) "self" else if (isVideo) "video" else "unknown"
        typ -> TypeCount(typ, 1)
      })
      .reduceByKeyAndWindow((a: TypeCount, b: TypeCount) => a + b, Seconds(60), Seconds(10))
      .map(_._2)
      .saveToInfluxDb(database, "type_count")

    // -------------------------------------------------------------------------
    // 6. Word cloud use
    //  remove few common stop words; static
    val stopWords = Set("are", "is", "i", "can", "he", "she", "has")
    val postContents = jsonPostStream
      .map(json => ((json \ "title").as[String], (json \ "selftext").as[String]))
      .map { case (a: String, b: String) => s"$a $b" }

    val commentContents = jsonCommentStream
      .map(json => (json \ "body").as[String])

    commentContents.union(postContents)
      .flatMap(_.split("\\s+"))
      .map(_.toLowerCase())
      .filter(!stopWords.contains(_))
      .map(w => (w, 1))
      .reduceByKeyAndWindow((a: Int, b: Int) => a + b, Seconds(10), Seconds(10))
      .filter { case (w, i) => i > 5 && !w.isEmpty }
      .map((WordFrequency.apply _).tupled)
      .saveToInfluxDb(database, "word_frequency")
    // -------------------------------------------------------------------------

    logger.info("Starting Spark jobs")
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
