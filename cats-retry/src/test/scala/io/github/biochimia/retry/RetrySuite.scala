// Copyright 2025 João Abecasis
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.github.biochimia.retry

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._

import io.github.biochimia.retry.instances.all._
import io.github.biochimia.retry.syntax.all._

import support.{EvalTry => F}

final class RetrySuite extends AnyFunSuite {

  import RetrySuite._

  val alwaysRetry = Retry.instance(Retry.Policy.alwaysRetry[F])
  val neverRetry  = Retry.instance(Retry.Policy.neverRetry[F])
  val retryOnce   = Retry.instance(Retry.Policy.maxRetries(1))

  test("Retry.instance instantiates the policy lazily, on each use") {
    var instantiations = 0

    val instance = Retry.instance {
      instantiations += 1
      Retry.Policy.alwaysRetry[F]
    }

    instantiations should equal(0)

    instance.use(_ => F.pure(3)).value should equal(Success(3))
    instance.use(_ => F.pure(1)).value should equal(Success(1))

    instantiations should equal(2)
  }

  test("Retry should not retry on a successful run") {
    implicit val retryPolicy = alwaysRetry

    val values   = Iterator(42, 54, 32)
    def f(): Int = values.next()

    testRetry[Int](f()) should equal(Result(Success(42), 1, Some(0)))
    testRetryNarrow[Int](f()) should equal(Result(Success(54), 1))
    testRetryWith[Int](f()) should equal(Result(Success(32), 1, Some(0)))
  }

  test("Retry should retry on a matched exception") {
    implicit val retryPolicy = retryOnce

    def f(): Int = ???

    testRetry[Int](f()) should matchPattern {
      case Result(Failure(OutOfRetriesException(_: NotImplementedError)), 2, Some(1)) =>
    }
    testRetryNarrow[Int](f()) should matchPattern {
      case Result(Failure(OutOfRetriesException(_: NotImplementedError)), 2, None) =>
    }
    testRetryWith[Int](f()) should matchPattern {
      case Result(Failure(OutOfRetriesException(_: NotImplementedError)), 2, Some(1)) =>
    }
  }

  test("Retry should let unmatched exceptions fall through") {
    implicit val retryPolicy = alwaysRetry

    val values =
      Seq(
        new NotImplementedError,
        new NotImplementedError,
        new NotImplementedError,
        new NotImplementedError,
        new RuntimeException("boom!"),
      )

    val iterator = (values ++ values ++ values).iterator
    def f(): Int = throw iterator.next()

    testRetry[Int](f()) should matchPattern {
      case Result(Failure(e: RuntimeException), 5, Some(4)) if e.getMessage() == "boom!" =>
    }
    testRetryNarrow[Int](f()) should matchPattern {
      case Result(Failure(e: RuntimeException), 5, None) if e.getMessage() == "boom!" =>
    }
    testRetryWith[Int](f()) should matchPattern {
      case Result(Failure(e: RuntimeException), 5, Some(4)) if e.getMessage() == "boom!" =>
    }
  }

  test("Retry should not retry with the neverRetry policy") {
    implicit val retryPolicy = neverRetry

    def f(): Int = ???

    testRetry[Int](f()) should matchPattern {
      case Result(Failure(OutOfRetriesException(_: NotImplementedError)), 1, Some(0)) =>
    }
    testRetryNarrow[Int](f()) should matchPattern {
      case Result(Failure(OutOfRetriesException(_: NotImplementedError)), 1, None) =>
    }
    testRetryWith[Int](f()) should matchPattern {
      case Result(Failure(OutOfRetriesException(_: NotImplementedError)), 1, Some(0)) =>
    }
  }

}

object RetrySuite {

  import support.{EvalTry => F}

  final case class Result[A](value: Try[A], attempts: Int, retryableFailures: Option[Int] = None)

  def testRetry[A](f: => A)(implicit retry: Retry[F]): Result[A] = {
    var attempts          = 0
    var retryableFailures = 0

    val value = F[A] {
      attempts += 1
      f
    }.retry { case _: NotImplementedError =>
      retryableFailures += 1
    }.value

    Result(value, attempts, Some(retryableFailures))
  }

  def testRetryNarrow[A](f: => A)(implicit retry: Retry[F]): Result[A] = {
    var attempts = 0

    val value =
      F[A] {
        attempts += 1
        f
      }.retryNarrow[NotImplementedError].value

    Result(value, attempts)
  }

  def testRetryWith[A](f: => A)(implicit retry: Retry[F]): Result[A] = {
    var attempts          = 0
    var retryableFailures = 0

    val value = F[A] {
      attempts += 1
      f
    }.retryWith { case _: NotImplementedError =>
      retryableFailures += 1
      F.unit
    }.value

    Result(value, attempts, Some(retryableFailures))
  }

}
