package au.csiro.data61.submissions

import com.typesafe.scalalogging.Logger
import au.csiro.data61.submissions.Main.CliOption
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.FileOutputStream
import java.io.Writer
import java.io.OutputStreamWriter
import resource.managed
import java.io.FileFilter

object PDFExtract {
  private val log = Logger(getClass)

  def extract(pdf: File, txt: File) {
    val s = new PDFTextStripper
    s.setAddMoreFormatting(true)
    for {
      in <- managed(PDDocument.load(pdf))
      out <- managed(new OutputStreamWriter(new FileOutputStream(txt), "UTF-8"))
    } s.writeText(in, out)
  }
  
  def text(c: CliOption): Unit = {
    for (pdf <- c.submissionDir.listFiles(new FileFilter { def accept(f: File) = f.getName.endsWith(".pdf") })) {
      val txt = new File(pdf.getParentFile, pdf.getName.replace(".pdf", ".txt"))
      log.debug(s"writing ${txt.getPath} ...")
      extract(pdf, txt)
    }
  }
  
}