package com.moneytransfer.exceptions

class UnknownAccountException(accountId: String) extends ServiceException(s"Can't find account with ID $accountId", Some("UnknownAccount"))
