package com.ibm.finance.cfpb.cc.driver

import java.sql.Timestamp

/**
  * Created by arunwagle on 8/17/16.
  */
case class ConsumerComplaintsFiltered (complaint_id: String, received_date : Timestamp
                                       , issue: String, product: String, state: String
                                       , company: String, year: Int, month: Int, day: Int)