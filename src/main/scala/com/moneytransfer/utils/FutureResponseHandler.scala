package com.moneytransfer.utils

import spray.http.StatusCodes.{OK, InternalServerError}
import spray.httpx.marshalling.Marshaller
import spray.routing.{HttpService, Route}

import scala.concurrent.Future
import scala.util.{Failure, Success}

import spray.routing.Directive.pimpApply
import spray.routing.directives.OnCompleteFutureMagnet.apply

trait FutureResponseHandler {
  self: HttpService with ExecutionContextComponent with LoggingComponent =>

  type FutureFailureHandler = PartialFunction[Throwable, Route]

  case class FutureHandler[T](result: Future[T],
                              resultHandler: Option[T => Route],
                              failureHandler: Option[Throwable => Route]) {

    def handleResult(resultHandler: T => Route): FutureHandler[T] =
      this.copy(resultHandler = Some(resultHandler))

    def handleFailure(failureHandler: FutureFailureHandler): FutureHandler[T] =
      this.copy(failureHandler = Some(failureHandler orElse {
        case failure => complete(InternalServerError, failure)
      }))
  }

  object FutureHandler {
    implicit def toRoute[T](responder: FutureHandler[T])(implicit m: Marshaller[T] = null): Route =
      onComplete[T](responder.result) {
        case Success(result) => responder.resultHandler match {
          case Some(resultHandler) => resultHandler(result)
          case None => complete(OK, result)
        }
        case Failure(failure) => responder.failureHandler match {
          case Some(failureHandler) => failureHandler(failure)
          case None => complete(InternalServerError, failure.getMessage)
        }
      }
  }

  implicit class FutureHandlerInitializer[E, T](futureValidation: => Future[T]) extends FutureHandler[T](futureValidation, None, None)

}