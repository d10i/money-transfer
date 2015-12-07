package com.moneytransfer.repositories

import com.moneytransfer.models.Account
import com.moneytransfer.utils.{ExecutionContextComponent, LoggingComponent}
import spray.util.LoggingContext

import scala.concurrent.{ExecutionContext, Future}

trait InMemoryAccountRepoComponent extends AccountRepoComponent {
  self: ExecutionContextComponent with LoggingComponent =>

  override val accountRepo = new InMemoryAccountRepo

  class InMemoryAccountRepo(implicit val executionContext: ExecutionContext, val log: LoggingContext) extends AccountRepo {
    val storage =  scala.collection.concurrent.TrieMap[String, Account]()

    override def create(account: Account) = Future {
      storage.put(account.id, account)
      log.info(s"Repo created account $account")
      account
    }

    override def update(id: String, account: Account) = Future {
      storage.replace(id, account.copy(id = id))
      log.info(s"Repo updated account with id $id")
    }

    override def findOne(id: String) = Future {
      storage.get(id) match {
        case Some(account) =>
          log.info(s"Repo found account with id $id: $account")
          Some(account)
        case None =>
          log.warning(s"Repo unable to find account with id $id")
          None
      }
    }

    override def delete(id: String) = Future {
      log.info(s"Repo deleted account with id $id")
      storage -= id
    }
  }
}