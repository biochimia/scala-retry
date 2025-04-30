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

package io.github.biochimia.retry.instances

import scala.concurrent.duration.FiniteDuration
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import cats.Eval
import cats.MonadThrow

import io.github.biochimia.retry.Backoff
import io.github.biochimia.retry.support.EvalTry

trait EvalTryInstances {

  implicit val monadThrowForEvalTry: MonadThrow[EvalTry] =
    new MonadThrow[EvalTry] {
      override def flatMap[A, B](eta: EvalTry[A])(f: (A) => EvalTry[B]): EvalTry[B] =
        eta.flatMap {
          case Success(a)    => f(a)
          case e: Failure[_] => Eval.now(e.asInstanceOf[Try[B]])
        }

      override def handleErrorWith[A](eta: EvalTry[A])(f: (Throwable) => EvalTry[A]): EvalTry[A] =
        eta.flatMap {
          case Failure(e)    => f(e)
          case s: Success[?] => Eval.now(s)
        }

      override def pure[A](a: A): EvalTry[A]               = Eval.now(Success(a))
      override def raiseError[A](t: Throwable): EvalTry[A] = Eval.now(Failure(t))

      override def tailRecM[A, B](a: A)(f: (A) => EvalTry[Either[A, B]]): EvalTry[B] =
        f(a).flatMap {
          case Success(Left(a))  => tailRecM(a)(f)
          case Success(Right(b)) => pure(b)
          case e: Failure[?]     => Eval.now(e.asInstanceOf[Try[B]])
        }
    }

  implicit val backoffForEvalTry: Backoff[EvalTry] =
    new Backoff[EvalTry] {
      override def backoff(d: FiniteDuration): EvalTry[Unit] = EvalTry(Thread.sleep(d.toMillis))
    }

}
