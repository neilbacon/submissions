package au.csiro.data61.submissions

import java.io.File

object Data {
  case class SubFile(partId: Int, url: String, pdf: File)
  case class Sub(subId: Int, subName: String, files: List[SubFile])
  
//  implicit val subFileCodec: CodecJson[SubFile] = casecodec2(SubFile.apply, SubFile.unapply)("partId", "url")
//  implicit val subCodec: CodecJson[Sub] = casecodec3(Sub.apply, Sub.unapply)("subId", "subName", "files")
  
  
}