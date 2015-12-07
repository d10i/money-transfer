package com.moneytransfer.utils

import com.moneytransfer.entities.ErrorResponse
import spray.http.StatusCodes.BadRequest
import spray.routing.Route
import spray.routing.directives.RouteDirectives

trait RequestValidators {
  self: JsonSupport with LoggingComponent with RouteDirectives =>

  def withValidatedName(firstName: String, lastName: String)(f: => Route): Route = {
    if(!firstName.isEmpty && firstName.length > 1 & !lastName.isEmpty && lastName.length > 1) {
      f
    } else {
      log.info(s"Invalid name: $firstName $lastName")
      complete(BadRequest, ErrorResponse("Sorry, bot first name and last name must be longer than 1 character"))
    }
  }
}
