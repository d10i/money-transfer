package com.moneytransfer

import com.moneytransfer.exceptions.CommonExceptionHandler
import com.moneytransfer.repositories.InMemoryAccountRepoComponent
import com.moneytransfer.resources.AccountResource
import com.moneytransfer.services.DefaultAccountServiceComponent

import akka.actor.{Props, ActorSystem, Actor}
import akka.io.IO
import akka.util.Timeout
import akka.pattern.ask
import com.moneytransfer.utils.{ActorRefFactoryComponent, ExecutionContextComponent, LoggingComponent}
import spray.can.Http
import spray.routing.{RejectionHandler, ExceptionHandler}
import spray.util.LoggingContext
import scala.concurrent.duration._

class MoneyTransferService extends Actor
  with AccountResource
  with DefaultAccountServiceComponent
  with InMemoryAccountRepoComponent
  with ExecutionContextComponent
  with LoggingComponent
  with ActorRefFactoryComponent
  with CommonExceptionHandler {

  override implicit def actorRefFactory = context
  override implicit def executionContext = actorRefFactory.dispatcher
  override implicit def log = LoggingContext.fromActorRefFactory(actorRefFactory)

  implicit def exceptionHandler(implicit log: LoggingContext): ExceptionHandler = commonExceptionHandler
  implicit def rejectionHandler(implicit log: LoggingContext): RejectionHandler = commonRejectionHandler

  def receive = runRoute {
    accountRoute // could add more routes from other resources by injecting them and joining them using '~'
  }
}

object Boot extends App {

  implicit val system = ActorSystem("on-spray-can")

  val service = system.actorOf(Props[MoneyTransferService], "money-transfer-service")

  implicit val timeout = Timeout(5.seconds)

  IO(Http) ? Http.Bind(service, interface = "localhost", port = 8080)
}
