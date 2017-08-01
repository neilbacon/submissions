package au.csiro.data61.submissions

import com.typesafe.scalalogging.Logger
import au.csiro.data61.dataFusion.common.Data._, JsonProtocol._
import au.csiro.data61.submissions.Main.CliOption
import scala.io.Codec
import spray.json._

object NerFilter {
  private val log = Logger(getClass)

  /**
   * return a `best Ner` from a list sorted by offStr asc, offEnd desc
   * result text and offsets are the union, but we don't bother with posStr/End because positions are not consistent across different NER impls
   */
  def merge(l: List[Ner]): Option[Ner] = {
    if (l.size == 1) l.headOption
    else {
      val l2 = l.filter(!_.text.contains("\n"))
      if (l2.size == 1) l2.headOption
      else {
        val l3 = if (l2.nonEmpty) l2 else l
        val (text, offEnd) = l3.foldLeft(("", 0)) {
          case (("", _), n) =>
            (n.text, n.offEnd)
          case (z@(s, off), n) =>
            if (off > n.offEnd) z
            else {
              var idx = n.text.length - (n.offEnd - off)
              if (idx < 1) z
              else (s + n.text.substring(idx), n.offEnd)
            }
        }
        l3.headOption.map(_.copy(text = text, offEnd = offEnd))
      }
    }
  }
  
  def filter(c: CliOption): Unit = {
    implicit val utf8 = Codec.UTF8
    val nerTypes = Set("PERSON", "ORGANIZATION", "LOCATION") // not interested in others
    val nerTypPred = (n: Ner) => nerTypes.contains(n.typ)
    
    val docs = io.Source.fromInputStream(System.in).getLines.map(_.parseJson.convertTo[Doc]).toList
    
    // get top and bottom quartiles for each NER impl separately for PERSON, ORGANIZATION and LOCATION
    val quartiles = {
      val ners = docs.flatMap { d => d.ner.filter(nerTypPred) ++ d.embedded.flatMap(_.ner.filter(nerTypPred)) }.groupBy(n => (n.impl, n.typ))
      (for {
        typ <- nerTypes
      } yield ("CoreNLP", typ) -> (0.5d, 2.0d) // fake because all CoreNLP scores are 1.0d
      ) ++ (
      for {
        impl <- Seq("OpenNLP", "MITIE")
        typ <- nerTypes
        key = (impl, typ)
        scores = ners(key).map(_.score).sorted.toIndexedSeq
        sz = scores.size
        lo <- scores.drop(sz/4).headOption
        hi <- scores.drop(sz*3/4).headOption
      } yield key -> (lo, hi)
      )
    }.toMap
    log.debug(s"filter: quartiles = $quartiles")
    
    docs.foreach { d =>
      // for now forget about d.embedded (its None for submissions)
      val typAndNotLowerQuartile = (n: Ner) => nerTypes.contains(n.typ) && n.score > quartiles((n.impl, n.typ))._1      
      val topQuartile = (n: Ner) => n.score > quartiles((n.impl, n.typ))._2
      val ner = d.ner.filter(typAndNotLowerQuartile).groupBy(_.typ).flatMap { case (typ, ners) =>
        // group overlapping Ners of same typ
        val ovlp = {
          // sort offStr asc, offEnd desc
          val srt = ners.sortWith((a, b) => a.offStr < b.offStr || (a.offStr == b.offStr && a.offEnd > b.offEnd))
          val o = srt.foldLeft((List.empty[List[Ner]], List.empty[Ner], 0, 0)) {
            case ((ll, l@_::_, str, end), n) =>
              if (n.offStr < end)
                (ll, n :: l, str, Math.max(n.offEnd, end))
              else
                (l.reverse :: ll, List(n), n.offStr, n.offEnd) // foldLeft then reverse to maintain sort order
            case ((ll, Nil, _, _), n) => // 
              (ll, List(n), n.offStr, n.offEnd)
          }
          (o._2 :: o._1).reverse
        }
        // for each group of overlapping Ners of same typ emit 0 or 1 merged Ner
        ovlp.flatMap { l =>
          val (top, mid) = l.partition(topQuartile)
          val n = if (top.nonEmpty) merge(top) else if (mid.size > 1) merge(mid) else None
          log.debug(s"filter: typ = $typ, top = ${top.size}, mid = ${mid.size}, ner = $n\ntop: $top\nmid: $mid")
          n
        }
      }.toList
      println(d.copy(ner = ner).toJson.compactPrint)
    }
  }
  
}