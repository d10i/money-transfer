package com.moneytransfer.services

import com.moneytransfer.exceptions.{UnknownAccountException, BalanceTooLowException}
import com.moneytransfer.models.Account
import com.moneytransfer.repositories.AccountRepoComponent
import com.moneytransfer.utils.{ExecutionContextComponent, LoggingComponent}

import scala.concurrent.{Future, ExecutionContext}

trait DefaultAccountServiceComponent extends AccountServiceComponent {
  self: AccountRepoComponent with ExecutionContextComponent with LoggingComponent =>

  val accountService = new DefaultAccountService

  class DefaultAccountService(implicit executionContext: ExecutionContext) extends AccountService {

    override def create(account: Account) = {
      log.info(s"Service creating account: $account")
      accountRepo.create(account).map { createdAccount =>
        log.info(s"Service created account $createdAccount")
        createdAccount
      }.recover {
        case e: Throwable =>
          log.error(e, "Service got unexpected error creating account")
          throw e
      }
    }

    override def update(id: String, account: Account) = {
      log.info(s"Service updating account with id $id. Account: $account")
      accountRepo.update(id, account).map { _ =>
        log.info(s"Service updated account with id $id")
      }.recover {
        case e: Throwable =>
          log.error(e, s"Service got unexpected error updating account with id $id")
          throw e
      }
    }

    override def findOne(id: String) = {
      log.info(s"Service looking up account by id $id")
      accountRepo.findOne(id).map {
        case Some(account) =>
          log.info(s"Service found account with id $id: $account")
          Some(account)
        case None =>
          log.warning(s"Service unable to find account with id $id")
          None
      }.recover {
        case e: Throwable =>
          log.error(e, s"Service got unexpected error looking up account by id $id")
          throw e
      }
    }

    override def delete(id: String) = {
      log.info(s"Service received request to delete account with id $id")
      accountRepo.delete(id).map { _ =>
        log.info(s"Service deleted account with id $id")
      }.recover {
        case e: Throwable =>
          log.error(e, "Service got unexpected error deleting account")
          throw e
      }
    }

    override def transferMoney(fromAccountId: String, toAccountId: String, amount: Float) = {
      val accountsOpt = for {
        fromAccountOpt <- findOne(fromAccountId)
        toAccountOpt <- findOne(toAccountId)
      } yield (fromAccountOpt, toAccountOpt)

      accountsOpt.flatMap {
        case (Some(fromAccount), Some(toAccount)) => transferMoney(fromAccount, toAccount, amount)
        case (None, _) => throw new UnknownAccountException(fromAccountId)
        case (_, None) => throw new UnknownAccountException(toAccountId)
      }
    }

    private def transferMoney(fromAccount: Account, toAccount: Account, amount: Float): Future[Unit] = {
      def rollback() = for {
        _ <- accountRepo.update(fromAccount.id, fromAccount)
        _ <- accountRepo.update(toAccount.id, toAccount)
      } yield ()

      if (fromAccount.balance < amount) throw new BalanceTooLowException(fromAccount.id, amount)

      val newFromAccount = fromAccount.copy(balance = fromAccount.balance - amount)
      val newToAccount = toAccount.copy(balance = toAccount.balance + amount)

      tryAndRollbackOnFailure(rollback) {
        for {
          _ <- accountRepo.update(newFromAccount.id, newFromAccount)
          _ <- accountRepo.update(newToAccount.id, newToAccount)
        } yield ()
      }
    }

    private def tryAndRollbackOnFailure[T](rollback: () => Future[T])(action: => Future[T]): Future[T] = {
      action.recover {
        case e: Throwable =>
          rollback()
          throw e
      }
    }
  }

}