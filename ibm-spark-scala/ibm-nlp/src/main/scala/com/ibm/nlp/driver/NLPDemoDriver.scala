package com.ibm.nlp.driver

import java.util.Properties

import com.fasterxml.jackson.databind.{ObjectMapper, ObjectReader}
import com.mongodb.spark.MongoSpark
import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.util.{CoreMap, PropertiesUtils}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrameWriter, SaveMode}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Seconds, StreamingContext}
import com.mongodb.spark.sql._
import edu.stanford.nlp.dcoref.CorefChain
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import edu.stanford.nlp.trees.Tree
import org.bson.Document
import play.api.libs.json.{Json, Writes}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

// This is required for implicit convertion of java collections to Scala collections
import scala.collection.JavaConversions._

/**
  * Created by arunwagle on 7/29/16.
  */
object NLPDemoDriver {

  def main(args: Array[String]) {
//    if (args.length < 2) {
//      System.err.println("Usage: NetworkWordCount <hostname> <port>")
//      System.exit(1)
//    }


    val uri: String = args.headOption.getOrElse("mongodb://localhost/test.coll")

    @transient val sparkConf = new SparkConf()
      .setAppName("NLPDemoDriver")
      .setMaster("local[8]")
      .set("spark.mongodb.input.uri", uri)
      .set("spark.mongodb.output.uri", uri)
    @transient val ssc = new StreamingContext(sparkConf, Seconds(1))


//      val lines = ssc.socketTextStream(args(0), args(1).toInt, StorageLevel.MEMORY_AND_DISK_SER)
    val lines = ssc.socketTextStream("localhost", 9999, StorageLevel.MEMORY_AND_DISK_SER)


    val nlpDataDStream = lines.map(
      lineRdd => {

        var mutableNLPDataSet: mutable.Set[NLPData]  = mutable.Set()
        var mutableWordsSet: mutable.Set[String]  =  mutable.Set.empty
        var mutablePOSSet: mutable.Set[POS]  =  mutable.Set.empty
        var mutableNERSet: mutable.Set[NER]  =  mutable.Set.empty
        var mutableLEMMASet: mutable.Set[LEMMA]  =  mutable.Set.empty


        val document: Annotation = new Annotation(lineRdd)
        // Pipeline instance
        val stanfordCoreNLP =  getStanfordCoreNLPInstance

        // run all Annotators on this text
        stanfordCoreNLP.annotate(document);

        // List of Sentences
        val jSentences : java.util.List[CoreMap] = document.get(classOf[SentencesAnnotation])


        for (jSentence: CoreMap <- jSentences){
          val jCoreLabelList : java.util.List[CoreLabel] = jSentence.get(classOf[TokensAnnotation])

          // initialize words set for each sentence
          mutableWordsSet = mutable.Set()
          mutablePOSSet = mutable.Set()
          mutableNERSet = mutable.Set()
          mutableLEMMASet = mutable.Set()

          // a CoreLabel is a CoreMap with additional token-specific methods
          for (jCoreLabel: CoreLabel <- jCoreLabelList){
            // this is the text of the token
            val word: String = jCoreLabel.get(classOf[TextAnnotation]);

            // Create Set of words in a sentence
            mutableWordsSet += word

            // this is the POS tag of the token
            val pos: String = jCoreLabel.get(classOf[PartOfSpeechAnnotation])
            // println(" word : " + word + " POS: " + pos)

            val posRef: POS = new POS(word, pos)
            mutablePOSSet += posRef

            val lemma: String = jCoreLabel.get(classOf[LemmaAnnotation])
            val lemmaRef: LEMMA = new LEMMA(word, lemma)
            mutableLEMMASet += lemmaRef

            val ner: String = jCoreLabel.get(classOf[NamedEntityTagAnnotation])
            val nerRef: NER = new NER(word, pos)
            mutableNERSet += nerRef

//            println(" lemma : " + lemma + " ner: " + ner)

          }

          // Sentiment Analysis
          val tree: Tree = jSentence.get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree]);
          val sentiment = RNNCoreAnnotations.getPredictedClass(tree);
//          println(" =sentiment : " + sentiment)

          // NLP Data
          val nlpData: NLPData = new NLPData(jSentence.toString, mutableWordsSet.toSeq, mutablePOSSet.toSeq, mutableNERSet.toSeq, mutableLEMMASet.toSeq, sentiment)

          // Add the nlp data object to the
          mutableNLPDataSet += nlpData

        }

        val graph : java.util.Map[Integer, CorefChain]  = document.get(classOf[CorefChainAnnotation]);
        println(" =graph : " + graph)

        // return a mutable set of NLPData oject
        mutableNLPDataSet
    })

    // Convert to a flat map of NLP RDD's
    val nlpDataFlatDStream = nlpDataDStream.flatMap(
      iterRdd => {
        iterRdd
    })




//    nlpDataFlatDStream.print()

    nlpDataFlatDStream.foreachRDD({
      rdd => {
        if (!rdd.isEmpty()) {
//        val sparkSession = SparkSessionSingleton.getInstance(rdd.sparkContext.getConf)
//        import sparkSession.implicits._

//        val df = rdd.toDF()
        // ds.write.mode("append")
//        MongoSpark.write(df).option("collection", "nlpData").mode(SaveMode.Append)
          rdd.collect()
          val mongoRdd = rdd.map(v => {

            val jsonNLPData = Json.toJson(v).toString()
//            println(" NLPData===" + v + " jsonNLPData== " + jsonNLPData)
            Document.parse(jsonNLPData);
          })


          MongoSpark.save(mongoRdd)

      }

      }
    })





    ssc.start()
    ssc.awaitTermination()
  }


  // Get an instance of stanfordPipeline instance.
  def getStanfordCoreNLPInstance : StanfordCoreNLP ={

    //
    val pipelineProperties: Properties  = PropertiesUtils.asProperties("annotators", "tokenize,cleanxml,ssplit,pos,lemma,ner,parse,sentiment",
      "ssplit.isOneSentence", "false",
      "parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz",
      "tokenize.language", "en")

    val stanfordCoreNLPInstance = new StanfordCoreNLP(pipelineProperties)

    stanfordCoreNLPInstance

  }

  /** Lazily instantiated singleton instance of SparkSession
    * Introduced in Spark 2.0
    *
    **/
//  object SparkSessionSingleton {
//
//    @transient  private var instance: SparkSession = _
//
//    def getInstance(sparkConf: SparkConf): SparkSession = {
//      if (instance == null) {
//        instance = SparkSession
//          .builder
//          .config(sparkConf)
//          .getOrCreate()
//      }
//      instance
//    }
//  }


  case class  NLPData(sentence: String, words: Seq[String], POS: Seq[POS], NER: Seq[NER], LEMMA: Seq[LEMMA], sentiment: Int)
  case class  POS(word: String, pos: String)
  case class  NER(word: String, ner: String)
  case class  LEMMA(word: String, lemma: String)

  // Required for converting to JSON string

  implicit val posWrites = new Writes[POS] {
    def writes(posRef: POS) = Json.obj(
      "word" -> posRef.word,
      "pos" -> posRef.pos
    )
  }

  implicit val nerWrites = new Writes[NER] {
    def writes(nerRef: NER) = Json.obj(
      "word" -> nerRef.word,
      "ner" -> nerRef.ner
    )
  }

  implicit val lemmaWrites = new Writes[LEMMA] {
    def writes(lemmaRef: LEMMA) = Json.obj(
      "word" -> lemmaRef.word,
      "lemma" -> lemmaRef.lemma
    )
  }

  implicit val nlpDataWrites = new Writes[NLPData] {
    def writes(nlpDataRef: NLPData) = Json.obj(
      "sentence" -> nlpDataRef.sentence,
      "words" -> nlpDataRef.words,
      "POS" -> nlpDataRef.POS,
      "NER" -> nlpDataRef.NER,
      "LEMMA" -> nlpDataRef.LEMMA,
      "sentiment" -> nlpDataRef.sentiment
    )
  }




}
