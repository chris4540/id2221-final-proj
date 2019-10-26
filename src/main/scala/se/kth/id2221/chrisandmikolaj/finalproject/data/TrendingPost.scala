package se.kth.id2221.chrisandmikolaj.finalproject.data

import com.paulgoldbaum.influxdbclient.Point
import se.kth.id2221.chrisandmikolaj.finalproject.util.ToPoint

case class TrendingPost(permalink: String, subreddit: String, commentCount: Int) {
  def +(other: TrendingPost): TrendingPost = this.copy(commentCount = this.commentCount + other.commentCount)
}

object TrendingPost {
  implicit val trendingPostToPoint: ToPoint[TrendingPost] = {
    case (TrendingPost(permalink, subreddit, commentCount), meas) =>
      Point(meas)
        .addTag("permalink", permalink)
        .addTag("subreddit", subreddit)
        .addField("comment_count", commentCount)
  }
}
