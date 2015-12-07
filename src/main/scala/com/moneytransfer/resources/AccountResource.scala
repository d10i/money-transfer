package com.moneytransfer.resources

import com.moneytransfer.entities._
import com.moneytransfer.exceptions.{BalanceTooLowException, UnknownAccountException}
import com.moneytransfer.models.Account
import com.moneytransfer.services.AccountServiceComponent
import com.moneytransfer.utils._
import spray.http.StatusCodes._
import spray.routing.Directive.pimpApply
import spray.routing._

trait AccountResource extends HttpService with FutureResponseHandler with RequestValidators with JsonSupport {
  self: AccountServiceComponent with ExecutionContextComponent with LoggingComponent =>

  def createAccount(accountCreationRequest: AccountCreationRequest) = {
    log.info(s"Resource received request to create account: $accountCreationRequest")

    withValidatedName(accountCreationRequest.firstName, accountCreationRequest.lastName) {
      val newAccount = Account(accountCreationRequest.firstName, accountCreationRequest.lastName, accountCreationRequest.balance)
      accountService.create(newAccount).handleResult { case createdAccount =>
        log.info(s"Resource created createdAccount $createdAccount")
        complete(Created, AccountCreationResponse(createdAccount.id))
      }.handleFailure {
        case e: Throwable =>
          log.error(e, "Resource got unexpected error creating account")
          complete(InternalServerError, ErrorResponse("Sorry, unexpected error creating account", Some("AccountCreationError")))
      }
    }
  }

  def updateAccount(id: String, accountUpdateRequest: AccountUpdateRequest) = {
    log.info(s"Resource updating account with ID $id. Request: $accountUpdateRequest")

    withValidatedName(accountUpdateRequest.firstName, accountUpdateRequest.lastName) {
      val newAccount = Account(accountUpdateRequest.firstName, accountUpdateRequest.lastName, accountUpdateRequest.balance)
      accountService.update(id, newAccount).handleResult { _ =>
        log.info(s"Updated account with ID $id")
        complete(NoContent)
      }.handleFailure {
        case e: Throwable =>
          log.error(e, s"Resource got unexpected error updating account with ID $id")
          complete(InternalServerError, ErrorResponse("Sorry, unexpected error updating account", Some("AccountUpdateError")))
      }
    }
  }

  def getAccountById(id: String) = {
    log.info(s"Resource looking up account by ID $id")
    accountService.findOne(id).handleResult {
      case Some(account) =>
          log.info(s"Resource found account with ID $id: $account")
          complete(OK, AccountFetchResponse(id = id, firstName = account.firstName,
            lastName = account.lastName, balance = account.balance))
      case None =>
          log.warning(s"Resource unable to find account with ID $id")
          complete(NotFound, ErrorResponse("Unable to find account with the specified ID"))
    }.handleFailure {
      case e: Throwable =>
        log.error(e, s"Resource got unexpected error looking up account by ID $id")
        complete(InternalServerError, ErrorResponse("Sorry, unexpected error looking up account", Some("AccountLookupError")))
    }
  }

  def deleteAccountById(id: String) = {
    log.info(s"Resource received request to delete account with ID $id")
    accountService.delete(id).handleResult { _ =>
      log.info(s"Resource deleted account with ID $id")
      complete(NoContent)
    }.handleFailure {
      case e: Throwable =>
        log.error(e, "Resource got unexpected error deleting account")
        complete(InternalServerError, ErrorResponse("Sorry, unexpected error deleting account", Some("AccountDeletionError")))
    }
  }

  def transferMoney(mtr: MoneyTransferRequest) = {
    log.info(s"Resource processing money transfer. Request: $mtr")
    accountService.transferMoney(mtr.fromAccountId, mtr.toAccountId, mtr.amount).handleResult { _ =>
      log.info(s"Transferred $mtr.amount from ${mtr.fromAccountId} to ${mtr.toAccountId}")
      complete(NoContent)
    }.handleFailure {
      case e: UnknownAccountException =>
        complete(NotFound, ErrorResponse(e.getMessage, e.code))
      case e: BalanceTooLowException =>
        complete(BadRequest, ErrorResponse(e.getMessage, e.code))
      case e: Throwable =>
        log.error(e, s"Resource got unexpected error transferring money from from ${mtr.fromAccountId} to ${mtr.toAccountId}")
        complete(InternalServerError, ErrorResponse("Sorry, unexpected error transferring money", Some("MoneyTransferError")))
    }
  }

  val accountRoute =
    pathPrefix("accounts") {
      path("money-transfer") {
        post {
          entity(as[MoneyTransferRequest]) { moneyTransferRequest =>
            dynamic(transferMoney(moneyTransferRequest))
          }
        }
      } ~
        path(Segment) { id =>
          put {
            entity(as[AccountUpdateRequest]) { accountUpdateRequest =>
              dynamic(updateAccount(id, accountUpdateRequest))
            }
          } ~
            dynamic(get(getAccountById(id))) ~
            dynamic(delete(deleteAccountById(id)))
        } ~
        pathEnd {
          post {
            entity(as[AccountCreationRequest]) { accountCreationRequest =>
              dynamic(createAccount(accountCreationRequest))
            }
          }
        }
    }
}