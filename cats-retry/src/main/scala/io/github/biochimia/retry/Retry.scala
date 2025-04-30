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

import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

import cats.MonadThrow

trait Retry[F[_]] {
  def use[A](f: (Retry.Policy[F]) => F[A]): F[A]
}

object Retry {

  trait Policy[F[_]] {
    def shouldRetry: Boolean
    def redeem(error: Throwable): F[Unit]
  }

  object Policy {

    final case class Retries[F[_]](retries: Iterator[F[Unit]]) extends Policy[F] {
      def shouldRetry: Boolean              = retries.hasNext
      def redeem(error: Throwable): F[Unit] = retries.next()
    }

    def alwaysRetry[F[_]: MonadThrow]: Policy[F] =
      Retries(Iterator.continually(MonadThrow[F].unit))

    def neverRetry[F[_]]: Policy[F] =
      Retries(Iterator.empty[F[Unit]])

    def backoffAndRetry[F[_]: Backoff](backoff: Iterator[FiniteDuration]): Policy[F] =
      Retries(backoff.map(Backoff[F].backoff(_)))

    def maxRetries[F[_]: MonadThrow](n: Int): Policy[F] =
      Retries(Iterator.continually(MonadThrow[F].unit).take(n))

    def fromRetries[F[_]](retries: Iterator[F[Unit]]): Policy[F] =
      Retries(retries)

  }

  def apply[F[_]](implicit R: Retry[F]): Retry[F] = R

  def instance[F[_]](p: => Policy[F])(implicit F: MonadThrow[F]): Retry[F] =
    new Retry[F] {
      def use[A](f: (Policy[F]) => F[A]): F[A] = F.flatMap(F.unit)(_ => f(p))
    }

  def retry[F[_], A](fa: F[A])(pf: PartialFunction[Throwable, Unit])(implicit
      F: MonadThrow[F],
      R: Retry[F],
  ): F[A] =
    retryWith(fa)(pf.andThen(_ => F.unit))

  def retryNarrow[F[_], A, EE <: Throwable](fa: F[A])(implicit
      F: MonadThrow[F],
      R: Retry[F],
      CT: ClassTag[EE],
  ): F[A] =
    retryWith[F, A](fa) {
      case error if CT.runtimeClass.isInstance(error) => F.unit
    }

  def retryWith[F[_], A](fa: F[A])(pf: PartialFunction[Throwable, F[Unit]])(implicit
      F: MonadThrow[F],
      R: Retry[F],
  ): F[A] =
    R.use { retryPolicy: Retry.Policy[F] =>
      def attempt: F[A] =
        F.recoverWith(fa) {
          case error if pf.isDefinedAt(error) =>
            if (retryPolicy.shouldRetry)
              handleRetryableError(error)
            else
              F.raiseError(OutOfRetriesException(error))
        }

      def handleRetryableError(error: Throwable): F[A] =
        F.flatMap(pf(error)) { _ =>
          F.flatMap(retryPolicy.redeem(error))(_ => attempt)
        }

      attempt
    }

}
