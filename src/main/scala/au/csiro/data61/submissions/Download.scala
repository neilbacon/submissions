package au.csiro.data61.submissions

import com.typesafe.scalalogging.Logger
import org.jsoup.Jsoup
import scala.collection.JavaConverters._

//import argonaut.Argonaut._
//import argonaut.CodecJson
import java.net.URL

import resource.managed
import java.io.{ FileOutputStream, InputStream, OutputStream }
import au.csiro.data61.submissions.Main.CliOption
import java.io.File
import java.nio.file._
import Data._

object Download {
  private val log = Logger(getClass)

  def download(c: CliOption): List[Sub] = {
    // Where there are multiple docs with the same subId, e.g.
    //   http://www.aph.gov.au/DocumentStore.ashx?id=6dfe2a35-9833-47e5-b0ad-79dc676b3423&subId=460082
    //   http://www.aph.gov.au/DocumentStore.ashx?id=41caa351-5333-4923-aa84-c6ba12369927&subId=460082
    // The first is the main doc and subsequent are supplementary material.
    // We may want to summarize only the main doc, but do NER on them all.
    // However sub-043-part-{0..6}.pdf look like each doc is quite independent, so maybe summarize them all separately.
    
    // Here we use being in the same <td> to group the docs with the same subId, and assign our own subIdx and partIdx.
    val html = io.Source.fromFile("data/submissions/Submissions – Parliament of Australia.html").getLines.mkString
    val doc = Jsoup.parse(html)
    val submissions = doc.select("table.rgMasterTable td:has(a.fileLink)").asScala.view.zipWithIndex.map { case (td, subIdx) =>
      Sub(subIdx, td.select("strong").text, 
          td.select("a.fileLink").asScala.view.zipWithIndex.map { case (a, partIdx) => 
            val f = new File(c.submissionDir, f"data/sub-${subIdx}%03d-part-${partIdx}%01d.pdf")
            SubFile(partIdx, a.attr("href"), f)
          }.toList
      )
    }.toList
    // println(submissions.asJson.spaces2)

    for {
      sub <- submissions
      fl <- sub.files      
    } {
      log.debug(s"writing ${fl.pdf.getPath} ...")
      for (in <- managed(new URL(fl.url).openStream)) Files.copy(in, fl.pdf.toPath, StandardCopyOption.REPLACE_EXISTING)
    } 
    
    submissions
  }
}