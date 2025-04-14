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

import scala.concurrent.duration._

import cats.Eval

import RetryOps._

final object Main {

  implicit val retryStrategy: Retry.Strategy = Retry.ExponentialBackoff(50.millis).withJitter.withMaxRetries(5)

  def main(args: Array[String]): Unit = {
    Eval
      .always(???)
      .retryOn { case _: NotImplementedError =>
        println("Looks like it is not implemented")
      }
      .value
  }

}
