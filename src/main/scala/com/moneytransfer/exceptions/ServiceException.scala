package com.moneytransfer.exceptions

class ServiceException(val message: String, val code: Option[String] = None) extends RuntimeException {

  override def getMessage = message
}
