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

import cats.Eval

final object RetryOps {

  implicit class RichEval[A](eval: Eval[A]) {
    def retryOn(pf: PartialFunction[Throwable, Unit])(implicit retryStrategy: Retry.Strategy): Eval[A] =
      Eval.always(Retry(eval.value)(pf))
  }

}
