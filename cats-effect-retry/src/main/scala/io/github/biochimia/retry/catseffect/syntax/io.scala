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

package io.github.biochimia.retry.catseffect.syntax

import cats.effect.kernel.MonadCancelThrow
import cats.effect.kernel.Resource

import io.github.biochimia.retry.Retry

trait ResourceSyntax {

  implicit def resourceToRetryPolicy[F[_]: MonadCancelThrow](r: Resource[F, Retry.Policy[F]]): Retry[F] =
    new Retry[F] {
      def use[A](f: (Retry.Policy[F]) => F[A]): F[A] = r.use(f)
    }

}
