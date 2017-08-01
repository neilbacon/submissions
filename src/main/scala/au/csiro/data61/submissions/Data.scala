package au.csiro.data61.submissions

import java.io.File

object Data {
  case class SubFile(partId: Int, url: String, pdf: File)
  case class Sub(subId: Int, subName: String, files: List[SubFile])
}