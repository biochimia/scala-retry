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

import scala.concurrent.duration._
import scala.util.Random

trait Backoff[F[_]] {
  def backoff(d: FiniteDuration): F[Unit]
}

object Backoff {

  object Duration {

    def fixed(duration: FiniteDuration): Iterator[FiniteDuration] =
      Iterator.continually(duration)

    def plainExponential(initial: FiniteDuration): Iterator[FiniteDuration] =
      Iterator.iterate(initial)(_ * 2)

    def exponential(initial: FiniteDuration): Iterator[FiniteDuration] =
      plainExponential(initial).map(fullJitter)

    def exponential(initial: FiniteDuration, max: FiniteDuration): Iterator[FiniteDuration] =
      (plainExponential(initial).takeWhile(_ < max) ++ fixed(max)).map(fullJitter)

    def decorrelated(initial: FiniteDuration, max: FiniteDuration): Iterator[FiniteDuration] = {
      if (initial.unit == max.unit)
        decorrelated(initial.length, max.length, initial.unit)
      else {
        val timeUnit =
          if (max.unit.convert(1, initial.unit) == 0)
            initial.unit
          else
            max.unit

        decorrelated(
          timeUnit.convert(initial.length, initial.unit),
          timeUnit.convert(max.length, max.unit),
          timeUnit,
        )
      }
    }

    private def decorrelated(initial: Long, max: Long, timeUnit: TimeUnit): Iterator[FiniteDuration] =
      Iterator
        .iterate(initial) { previous =>
          max.min(Random.between(initial, previous * 3))
        }
        .drop(1) // Drop non-random initial value
        .map(FiniteDuration(_, timeUnit))

    private def fullJitter(duration: FiniteDuration): FiniteDuration =
      new FiniteDuration(Random.nextLong(duration.length), duration.unit)

  }

  def apply[F[_]](implicit B: Backoff[F]): Backoff[F] = B

  def fixed[F[_]: Backoff](duration: FiniteDuration): Iterator[F[Unit]] =
    Duration.fixed(duration).map(toBackoff[F])

  def plainExponential[F[_]: Backoff](initial: FiniteDuration): Iterator[F[Unit]] =
    Duration.plainExponential(initial).map(toBackoff[F])

  def exponential[F[_]: Backoff](initial: FiniteDuration): Iterator[F[Unit]] =
    Duration.exponential(initial).map(toBackoff[F])

  def exponential[F[_]: Backoff](initial: FiniteDuration, max: FiniteDuration): Iterator[F[Unit]] =
    Duration.exponential(initial, max).map(toBackoff[F])

  def decorrelated[F[_]: Backoff](min: FiniteDuration, max: FiniteDuration): Iterator[F[Unit]] =
    Duration.decorrelated(min, max).map(toBackoff[F])

  private def toBackoff[F[_]: Backoff](duration: FiniteDuration): F[Unit] =
    Backoff[F].backoff(duration)

}
