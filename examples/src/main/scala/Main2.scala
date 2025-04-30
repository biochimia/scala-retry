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

import io.github.biochimia.retry.Retry
import io.github.biochimia.retry.instances.all._
import io.github.biochimia.retry.support.EvalTry
import io.github.biochimia.retry.syntax.all._

object Main2 {

  def main(args: Array[String]): Unit = {
    implicit val retryPolicy: Retry[EvalTry] = Retry.instance {
      var retries = 1

      new Retry.Policy[EvalTry] {
        def shouldRetry: Boolean = retries < 3
        def redeem(e: Throwable): EvalTry[Unit] =
          EvalTry {
            retries += 1
            println(s"Caught retryable error, will retry: $e")
          }
      }
    }

    println("Using a custom retry policy that logs errors")

    EvalTry[Unit](???)
      .retryNarrow[NotImplementedError]
      .map(_.fold(error => error.printStackTrace(), identity))
      .value
  }

}
