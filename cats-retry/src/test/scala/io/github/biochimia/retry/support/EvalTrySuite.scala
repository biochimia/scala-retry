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

package io.github.biochimia.retry.support

import scala.util.Failure
import scala.util.Success

import cats.MonadThrow
import cats.Now

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._

import io.github.biochimia.retry.Backoff
import io.github.biochimia.retry.support.EvalTry

final class EvalTrySuite extends AnyFunSuite {

  def evalTrySyntax[A](a: A): Unit = {
    val _: EvalTry[A]    = EvalTry(a)
    val _: EvalTry[A]    = EvalTry.pure(a)
    val _: EvalTry[A]    = EvalTry.raiseError[A](new RuntimeException)
    val _: EvalTry[Unit] = EvalTry.unit
  }

  def instancesForEvalTry(): Unit = {
    import io.github.biochimia.retry.instances.all._

    val _: MonadThrow[EvalTry] = MonadThrow[EvalTry]
    val _: Backoff[EvalTry]    = Backoff[EvalTry]
  }

  test("EvalTry constructors") {
    EvalTry.pure(42) should equal(Now(Success(42)))
    EvalTry.raiseError(new RuntimeException) should matchPattern { case Now(Failure(_: RuntimeException)) => }
    EvalTry.unit should equal(Now(Success(())))
  }

  test("EvalTry$.apply returns a lazily evaluated instance") {
    var evaluationCount = 0

    val _ = EvalTry {
      evaluationCount += 1
      evaluationCount
    }

    evaluationCount should equal(0)
  }

  test("EvalTry.value should evaluate and return a wrapped result") {
    var evaluationCount = 0

    val instance = EvalTry {
      evaluationCount += 1
      evaluationCount
    }

    instance.value should equal(Success(1))
  }

  test("EvalTry.value will repeatedly evaluate the result") {
    var evaluationCount = 0

    val instance = EvalTry {
      evaluationCount += 1
      evaluationCount
    }

    instance.value should equal(Success(1))
    instance.value should equal(Success(2))
    instance.value should equal(Success(3))
  }

  test("EvalTry catches (non-fatal) exceptions and wraps the error") {
    var evaluationCount = 0

    val instance = EvalTry {
      evaluationCount += 1
      ???
    }

    instance.value should matchPattern { case Failure(_: NotImplementedError) => }
    instance.value should matchPattern { case Failure(_: NotImplementedError) => }

    evaluationCount should equal(2)
  }

  test("EvalTry.handleErrorWith does not re-evaluate successful instances") {
    import io.github.biochimia.retry.instances.all._

    var evaluationCount = 0

    val instance =
      MonadThrow[EvalTry].handleErrorWith(EvalTry {
        evaluationCount += 1
        evaluationCount
      }) { case _ =>
        EvalTry.raiseError(new RuntimeException("failed"))
      }

    instance.value should equal(Success(1))
  }

}
