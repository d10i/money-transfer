package com.moneytransfer.utils

import scala.concurrent.ExecutionContext

trait ExecutionContextComponent {

  implicit def executionContext: ExecutionContext
}
