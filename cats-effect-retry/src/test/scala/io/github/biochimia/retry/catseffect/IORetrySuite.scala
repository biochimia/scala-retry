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

package io.github.biochimia.retry.catseffect

import scala.concurrent.duration._

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers._

import io.github.biochimia.retry.Backoff
import io.github.biochimia.retry.OutOfRetriesException
import io.github.biochimia.retry.Retry
import io.github.biochimia.retry.catseffect.instances.all._
import io.github.biochimia.retry.catseffect.syntax.all._
import io.github.biochimia.retry.syntax.all._

final class IORetrySuite extends AsyncFunSuite with AsyncIOSpec {

  val alwaysRetry = IO(Retry.Policy.alwaysRetry[IO]).toResource
  val neverRetry  = IO(Retry.Policy.neverRetry[IO]).toResource
  val retryOnce   = IO(Retry.Policy.maxRetries[IO](1)).toResource

  test("IO.retry should not retry on a successful run") {
    implicit val retryPolicy: Retry[IO] = alwaysRetry

    var attempts        = 0
    var retryableErrors = 0

    IO
      .delay {
        attempts += 1
        42
      }
      .retry { case _ =>
        retryableErrors += 1
      }
      .asserting { result =>
        result should equal(42)
        attempts should equal(1)
        retryableErrors should equal(0)
      }
  }

  test("IO.retry should retry on a matched exception") {
    implicit val retryPolicy: Retry[IO] = retryOnce

    var attempts        = 0
    var retryableErrors = 0

    IO
      .delay[Int] {
        attempts += 1
        ???
      }
      .retry { case _: NotImplementedError =>
        retryableErrors += 1
      }
      .attempt
      .asserting { result =>
        result should matchPattern { case Left(OutOfRetriesException(_: NotImplementedError)) => }
        attempts should equal(2)
        retryableErrors should equal(1)
      }
  }

  test("IO.Retry should let unmatched exceptions fall through") {
    implicit val retryPolicy: Retry[IO] = alwaysRetry

    var attempts        = 0
    var retryableErrors = 0

    val values =
      Iterator(
        new NotImplementedError,
        new NotImplementedError,
        new NotImplementedError,
        new NotImplementedError,
        new RuntimeException("boom!"),
      )

    IO
      .delay[Int] {
        attempts += 1
        throw values.next()
      }
      .retry { case _: NotImplementedError =>
        retryableErrors += 1
      }
      .attempt
      .asserting { result =>
        result should matchPattern { case Left(e: RuntimeException) if e.getMessage() == "boom!" => }
        attempts should equal(5)
        retryableErrors should equal(4)
      }
  }

  test("IO.Retry should not retry with the neverRetry policy") {
    implicit val retryPolicy: Retry[IO] = neverRetry

    var attempts        = 0
    var retryableErrors = 0

    IO
      .delay[Int] {
        attempts += 1
        ???
      }
      .retry { case _: NotImplementedError =>
        retryableErrors += 1
      }
      .attempt
      .asserting { result =>
        result should matchPattern { case Left(OutOfRetriesException(_: NotImplementedError)) => }
        attempts should equal(1)
        retryableErrors should equal(0)
      }
  }

  test("IO.Retry with a backoff policy") {
    implicit val retryPolicy: Retry[IO] =
      IO(Retry.Policy.fromRetries(Backoff.plainExponential(50.millis))).toResource

    val threshold: FiniteDuration = 100.millis

    var attempts: Int                      = 0
    var sleepStart: Option[FiniteDuration] = None

    IO
      .defer[FiniteDuration] {
        IO.monotonic
          .map { time =>
            attempts += 1

            sleepStart match {
              case Some(sleepStart) if time - sleepStart > threshold => time - sleepStart
              case _                                                 => ???
            }
          }
      }
      .retryWith { case _: NotImplementedError =>
        IO.monotonic.map { time =>
          sleepStart = Some(time)
        }
      }
      .asserting { result =>
        result should be >= 100.millis
        attempts should equal(3)
      }
  }

}
