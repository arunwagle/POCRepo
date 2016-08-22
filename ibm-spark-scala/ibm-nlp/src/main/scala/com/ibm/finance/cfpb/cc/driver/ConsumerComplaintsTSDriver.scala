package com.ibm.finance.cfpb.cc.driver

import java.sql.Timestamp

import com.mongodb.spark.MongoSpark
import com.mongodb.spark.config.{ReadConfig, WriteConfig}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{SQLContext, SaveMode}
import com.mongodb.spark.sql._
import play.api.libs.json.{Json, Writes}
import org.apache.spark.sql.functions.{date_format, from_unixtime, from_utc_timestamp, to_date, to_utc_timestamp, unix_timestamp}


import scala.collection.mutable


/**
  * Created by arunwagle on 7/29/16.
  */
object ConsumerComplaintsTSDriver {

  org.apache.spark.sql.catalyst.encoders.OuterScopes.addOuterScope(this)

  val MONGODB_URI: String = "mongodb://cfpbAdmin:cfpbAdmin@aws-us-east-1-portal.19.dblayer.com:11490/CFPB.consumer_complaints"
  val MONGODB_OUTPUT_URI: String = "mongodb://cfpbAdmin:cfpbAdmin@aws-us-east-1-portal.19.dblayer.com:11490/CFPB.monthly_consumer_complaints"

  def main(args: Array[String]) {
//    if (args.length < 2) {
//      System.err.println("Usage: NetworkWordCount <hostname> <port>")
//      System.exit(1)
//    }




    @transient val sparkConf = new SparkConf()
      .setAppName("ConsumerComplaintsTSDriver")
      .setMaster("local[8]")
      .set("spark.mongodb.input.uri", MONGODB_URI)
      .set("spark.mongodb.output.uri", MONGODB_OUTPUT_URI)
//      .set("spark.mongodb.output.database", MONGODB_DB_URI)



    val sc = new SparkContext(sparkConf)

    val count = generateTimeSeriesModel(sc)

    println("count = " + count)

//    MongoSpark.save(monthlyComplaintsDS.toDF().write.option("collection", "monthly_complaints_test").mode(SaveMode.Append))
    sc.stop()

  }




  case class ConsumerComplaints (company: String, company_public_response: String
                                  , company_response_to_consumer: String, complaint_id: String
                                  , consumer_complaint_narrative: String, consumer_consent_provided: String
                                  , consumer_disputed: String, date_received: String, date_sent_to_company: String
                                  , issue: String, product: String, state: String, sub_issue: String, sub_product: String
                                  , submitted_via: String, tags: String, timely_response: String, zip_code: String, day:String)



  def generateTimeSeriesModel( sc: SparkContext): Long = {



      // create a SQL context to read data
      val sqlContext = SQLContext.getOrCreate(sc)

      val readConfig: ReadConfig = ReadConfig.apply(sc).withOptions(Map("uri"-> MONGODB_URI))
//        ReadConfig(Map("uri"-> MONGODB_URI))


      val consumerComplaintsRawDF = MongoSpark.load(sc, readConfig).toDF[ConsumerComplaints]()
      consumerComplaintsRawDF.registerTempTable("consumer_complaints_tmp")

      consumerComplaintsRawDF.printSchema()

      // Write time series data
      //    val monthlyComplaintsDF = consumerComplaintsRawDF.select(to_date(from_unixtime(unix_timestamp(consumerComplaintsRawDF("date_received"), "MM/dd/yy"))), consumerComplaintsRawDF("date_received"))
      //    to_date(from_unixtime(unix_timestamp(a.date_received, 'MM/dd/yy')))


      val consumerComplaintsFilteredDF = sqlContext.sql("SELECT complaint_id, to_date(from_unixtime(unix_timestamp(a.date_received, 'MM/dd/yy'))) as received_date" +
        ",issue, product, state, company, year(to_date(from_unixtime(unix_timestamp(a.date_received, 'MM/dd/yy')))) as year" +
        ", month(to_date(from_unixtime(unix_timestamp(a.date_received, 'MM/dd/yy')))) as month" +
        ", day(to_date(from_unixtime(unix_timestamp(a.date_received, 'MM/dd/yy')))) as day  " +
        " FROM consumer_complaints_tmp a ")


      import sqlContext.implicits._
      val consumerComplaintsDS = consumerComplaintsFilteredDF.toDF().as[ConsumerComplaintsFiltered]


      val monthlyComplaintsDS = consumerComplaintsDS.map(r => {

        val complaintData : ComplaintData = new  ComplaintData(r.day, r.product, r.issue, r.company, r.state)
        // not required
        val id = r.complaint_id + ":" + r.received_date

        val monthlyComplaints: MonthlyComplaints = new MonthlyComplaints(r.complaint_id, r.year, r.month, r.day, r.received_date, r.product, r.issue, r.company, r.state)

        monthlyComplaints

      })



      monthlyComplaintsDS.show(50)


      // Save to DB
      val writeConfig = WriteConfig(Map("collection" -> "monthly_complaints_test", "writeConcern.w" -> "majority"), Some(WriteConfig(sc)))
      MongoSpark.save(monthlyComplaintsDS.toDF().write.mode(SaveMode.Ignore), writeConfig)

      val count = monthlyComplaintsDS.count()
      return count

  }




}
