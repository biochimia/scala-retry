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

package io.github.biochimia.retry.syntax

import scala.reflect.ClassTag

import cats.MonadThrow

import io.github.biochimia.retry.Retry

trait MonadThrowRetrySyntax {
  implicit final def monadThrowRetrySyntax[F[_]: MonadThrow, A](fa: F[A]): MonadThrowRetryOps[F, A] =
    new MonadThrowRetryOps(fa)
}

final class MonadThrowRetryOps[F[_]: MonadThrow, A](private val fa: F[A]) {

  def retry(pf: PartialFunction[Throwable, Unit])(implicit R: Retry[F]): F[A] =
    Retry.retry(fa)(pf)

  def retryNarrow[EE <: Throwable: ClassTag](implicit R: Retry[F]): F[A] =
    Retry.retryNarrow(fa)

  def retryWith(pf: PartialFunction[Throwable, F[Unit]])(implicit R: Retry[F]): F[A] =
    Retry.retryWith(fa)(pf)

}
