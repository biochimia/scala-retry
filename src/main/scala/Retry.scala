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

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.util.{Failure, Random, Try}

final object Retry {

  trait Strategy { self =>

    def retries: Iterator[FiniteDuration]

    def withMaxRetries(n: Int) =
      new Strategy {
        def retries = self.retries.take(n)
      }

    def withMaxDelay(maxDelay: FiniteDuration) =
      new Strategy {
        def retries = self.retries.map(_.min(maxDelay))
      }

    def withJitter =
      new Strategy {
        def retries = self.retries.map(delay => Random.nextLong(delay.toMillis).millis)
      }

  }

  final object Never extends Strategy {
    def retries = Iterator.empty[FiniteDuration]
  }

  final case class FixedDelay(delay: FiniteDuration) extends Strategy {
    def retries = Iterator.continually(delay)
  }

  val Immediately = FixedDelay(Duration.Zero)

  final case class ExponentialBackoff(initialDelay: FiniteDuration) extends Strategy {
    def retries = Iterator.iterate(initialDelay)(_ * 2)
  }

  def apply[A](f: => A)(pf: PartialFunction[Throwable, Unit])(implicit strategy: Strategy): A =
    tryWithRetries(() => f, pf, strategy.retries)

  @tailrec
  private def tryWithRetries[A](
      f: () => A,
      pf: PartialFunction[Throwable, Unit],
      retries: Iterator[FiniteDuration],
  ): A =
    Try(f()) match {
      case Failure(error) if retries.hasNext && pf.isDefinedAt(error) =>
        pf(error)
        Thread.sleep(retries.next().toMillis)

        tryWithRetries(f, pf, retries)

      case result => result.get
    }

}
