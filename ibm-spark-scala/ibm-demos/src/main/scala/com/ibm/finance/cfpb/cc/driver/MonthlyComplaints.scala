package com.ibm.finance.cfpb.cc.driver

import java.sql.Timestamp


/**
  * Created by arunwagle on 8/17/16.
  */
case class MonthlyComplaints (complaint_id: String, year: Int, month: Int, day: Int , received_date: Timestamp, product: String, issue: String, company: String, state: String)
