package au.csiro.data61.submissions

import au.csiro.data61.submissions.Main.CliOption
import com.entopix.maui.main.MauiModelBuilder
import com.entopix.maui.util.DataLoader
import com.typesafe.scalalogging.Logger
import java.nio.file.Files
import com.entopix.maui.main.MauiTopicExtractor
import scala.collection.JavaConverters._
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

object Summarize {
  private val log = Logger(getClass)  
  
  def train(c: CliOption): Unit = {
		val docs = DataLoader.loadTestDocuments(c.trainingDir.getPath)
    log.info(s"train: training with ${docs.size} examples")

    val mb = new MauiModelBuilder
		mb.minNumOccur = 2	// change to 1 for short documents
		mb.minPhraseLength = 2
		mb.maxPhraseLength = 5

    val mauiFilter = mb.buildModel(docs)
		
    c.model.getParentFile.mkdirs
    mb.modelName = c.model.getPath // model written to this file
		mb.saveModel(mauiFilter)
  }
  
  def writeFile(path: String, content: String) = Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8))
  
  def summarize(c: CliOption): Unit = {
    val te = new MauiTopicExtractor
		te.modelName = c.model.getPath
		te.loadModel

		val docs = DataLoader.loadTestDocuments(c.submissionDir.getPath)
    te.topicsPerDocument = 10
		te.setTopicProbability(0.0)
		for {
		  topics <- te.extractTopics(docs).asScala
		} {
		  writeFile(topics.getFilePath.replace(".txt", ".sum"), topics.getTopics.asScala.map(_.getTitle).mkString("", "\n", "\n"))
		  // log.debug(s"summarize: id = ${t.getId}, path = ${topics.getFilePath}, probability = ${t.getProbability}, title = ${t.getTitle}")
		}
    
  }
}