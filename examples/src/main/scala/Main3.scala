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

import cats.MonadThrow

import io.github.biochimia.retry.OutOfRetriesException
import io.github.biochimia.retry.Retry
import io.github.biochimia.retry.instances.all._
import io.github.biochimia.retry.support.EvalTry
import io.github.biochimia.retry.syntax.all._

object Main3 {

  val keepErrors: Retry[EvalTry] =
    new Retry[EvalTry] {
      def use[A](f: (Retry.Policy[EvalTry]) => EvalTry[A]): EvalTry[A] = {
        var suppressed = List.empty[Throwable]

        MonadThrow[EvalTry].adaptError(
          f(
            new Retry.Policy[EvalTry] {
              def shouldRetry: Boolean = suppressed.length < 3
              def redeem(e: Throwable): EvalTry[Unit] = {
                suppressed = e :: suppressed
                EvalTry.unit
              }
            }
          )
        ) { case e: OutOfRetriesException =>
          suppressed.foreach(e.addSuppressed)
          e

        }
      }
    }

  def main(args: Array[String]): Unit = {
    implicit val retryPolicy: Retry[EvalTry] = keepErrors

    println("Using a custom retry policy that keeps track of suppressed errors")
    EvalTry[Unit](???)
      .retryWith { case _: NotImplementedError =>
        EvalTry(println("Still, no luck."))
      }
      .map(_.fold(error => error.printStackTrace(), identity))
      .value
  }

}
