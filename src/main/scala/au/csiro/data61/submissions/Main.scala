package au.csiro.data61.submissions

import com.typesafe.scalalogging.Logger
import org.jsoup.Jsoup
import scala.collection.JavaConverters._

//import argonaut.Argonaut._
//import argonaut.CodecJson
import java.net.URL

import resource.managed
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.File

object Main {
  private val log = Logger(getClass)
  
  case class CliOption(download: Boolean, extract: Boolean, train: Boolean, summarize: Boolean, nerFilter: Boolean, submissionDir: File, trainingDir: File, model: File)
  
  def getCliOption(args: Seq[String]) = {
    val defaultCliOption = CliOption(false, false, false, false, false, new File("data/submissions"), new File("data/train"), new File("data/model/tagging_model"))
    
    val parser = new scopt.OptionParser[CliOption]("submissions") {
      head("submissions", "0.x")
      note("Process submissions.")
      opt[Unit]('d', "download") action { (_, c) =>
        c.copy(download = true)
      } text (s"download docs (default ${defaultCliOption.download})")
      opt[Unit]('e', "extract") action { (_, c) =>
        c.copy(extract = true)
      } text (s"extract text from docs (default ${defaultCliOption.extract})")
      opt[Unit]('t', "train") action { (_, c) =>
        c.copy(train = true)
      } text (s"train (create) model (default ${defaultCliOption.train})")
      opt[Unit]('s', "summarize") action { (_, c) =>
        c.copy(summarize = true)
      } text (s"summarize docs - key phrase extraction (default ${defaultCliOption.summarize})")
      opt[Unit]('n', "nerFilter") action { (_, c) =>
        c.copy(nerFilter = true)
      } text (s"filter JSON ner data from stdin to stdout (default ${defaultCliOption.nerFilter})")
      opt[File]('z', "submissionDir") action { (f, c) =>
        c.copy(submissionDir = f)
      } text (s"work directory for files (default ${defaultCliOption.submissionDir})")
      opt[File]('r', "trainingDir") action { (f, c) =>
        c.copy(trainingDir = f)
      } text (s"maui trainingDir (default ${defaultCliOption.trainingDir})")
      opt[File]('m', "model") action { (f, c) =>
        c.copy(model = f)
      } text (s"maui model file (default ${defaultCliOption.model})")
    }
    
    parser.parse(args, defaultCliOption)
  }
  
  def main(args: Array[String]): Unit = {
    for (c <- getCliOption(args)) {
      if (c.download) Download.download(c)
      if (c.extract) PDFExtract.text(c)
      if (c.train) Summarize.train(c)
      if (c.summarize) Summarize.summarize(c)
      if (c.nerFilter) NerFilter.filter(c)
    }
  }
}