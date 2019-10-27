package se.kth.id2221.chrisandmikolaj.finalproject.data

import com.paulgoldbaum.influxdbclient.Point
import se.kth.id2221.chrisandmikolaj.finalproject.util.ToPoint

case class RedditRate(subreddit: String, count: Int)

object RedditRate {
  implicit val redditRateToPoint: ToPoint[RedditRate] = {
    case (RedditRate(subreddit, count), meas) =>
      Point(meas)
        .addTag("subreddit", subreddit)
        .addField("count", count)
  }
}
