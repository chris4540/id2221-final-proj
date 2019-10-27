package se.kth.id2221.chrisandmikolaj.finalproject.data

import com.paulgoldbaum.influxdbclient.Point
import se.kth.id2221.chrisandmikolaj.finalproject.util.ToPoint

case class WordFrequency(word: String, count: Int) {
  def +(other: WordFrequency): WordFrequency = copy(count = this.count + other.count)
}

object WordFrequency {
  implicit val wordFrequencyToPoint: ToPoint[WordFrequency] = {
    case (WordFrequency(word, count), meas) =>
      Point(meas)
        .addTag("word", word)
        .addField("count", count)
  }
}
