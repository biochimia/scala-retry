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

import io.github.biochimia.retry.Backoff
import io.github.biochimia.retry.Retry
import io.github.biochimia.retry.instances.all._
import io.github.biochimia.retry.support.EvalTry
import io.github.biochimia.retry.syntax.all._

object Main {

  def main(args: Array[String]): Unit = {
    implicit val retryPolicy: Retry[EvalTry] =
      Retry.instance(Retry.Policy.fromRetries(Backoff.exponential(50.millis).take(5)))

    println("Using a retry policy with jittered exponential backoff, 5 retries")
    EvalTry[Unit](???)
      .retry { case _: NotImplementedError =>
        println("Looks like this is not implemented")
      }
      .map(_.fold(error => error.printStackTrace(), identity))
      .value
  }

}
