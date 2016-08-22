
Run locally
./bin/spark-submit --class "com.ibm.finance.cfpb.cc.driver.ConsumerComplaintsTSDriver"  --executor-memory=4g --driver-memory=4G --master spark://localhost:7077 /Users/arunwagle/Bluemix/Projects/Personal/ibm-spark-scala/ibm-nlp/target/ibm-nlp-1.0-SNAPSHOT-jar-with-dependencies.jar