package com.moneytransfer.entities

import com.moneytransfer.exceptions.ServiceException

case class ErrorResponse(message: String, code: Option[String] = None)

object ErrorResponse {
  def apply(serviceException: ServiceException): ErrorResponse = ErrorResponse(serviceException.getMessage, serviceException.code)
  def apply(throwable: Throwable): ErrorResponse = ErrorResponse(throwable.getMessage)
}