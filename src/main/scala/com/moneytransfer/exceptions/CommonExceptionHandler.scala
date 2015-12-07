package com.moneytransfer.exceptions

import com.moneytransfer.entities.ErrorResponse
import com.moneytransfer.utils.JsonSupport
import spray.http.StatusCodes._
import spray.routing.{HttpService, RejectionHandler, ExceptionHandler}
import spray.util.LoggingContext

trait CommonExceptionHandler {
  self: HttpService with JsonSupport =>

  implicit def commonExceptionHandler(implicit log: LoggingContext): ExceptionHandler.PF = {
    case ex =>
      log.error(ex, ex.getMessage)
      complete(InternalServerError, ErrorResponse("There was an internal server error."))
  }

  implicit def commonRejectionHandler(implicit log: LoggingContext): RejectionHandler.PF = {
    case _ =>
      complete(InternalServerError, ErrorResponse("There was an internal server error."))
  }
}
