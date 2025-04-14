# `scala-retry`

Retries.

## Example

Code example in [`src/main/scala/Main.scala`](src/main/scala/Main.scala):

```scala
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
```

### Running the Example

```
sbt run
```

```
[info] welcome to sbt 1.10.11 (Homebrew Java 23.0.2)
[info] loading settings for project scala-retry-build-build from metals.sbt...
[info] loading project definition from /Users/joao.abecasis/code/github.com/biochimia/scala-retry/project/project
[info] loading settings for project scala-retry-build from metals.sbt...
[info] loading project definition from /Users/joao.abecasis/code/github.com/biochimia/scala-retry/project
[success] Generated .bloop/scala-retry-build.json
[success] Total time: 1 s, completed Apr 14, 2025, 8:51:26 AM
[info] loading settings for project scala-retry from build.sbt...
[info] set current project to retry (in build file:/Users/joao.abecasis/code/github.com/biochimia/scala-retry/)
[info] running Main 
Looks like it is not implemented
Looks like it is not implemented
Looks like it is not implemented
Looks like it is not implemented
Looks like it is not implemented
[error] scala.NotImplementedError: an implementation is missing
[error]         at scala.Predef$.$qmark$qmark$qmark(Predef.scala:344)
[error]         at Main$.$anonfun$main$1(Main.scala:27)
[error]         at cats.Always.value(Eval.scala:194)
[error]         at RetryOps$RichEval.$anonfun$retryOn$2(RetryOps.scala:21)
[error]         at scala.util.Try$.apply(Try.scala:217)
[error]         at Retry$.tryWithRetries(Retry.scala:65)
[error]         at Retry$.apply(Retry.scala:57)
[error]         at RetryOps$RichEval.$anonfun$retryOn$1(RetryOps.scala:21)
[error]         at cats.Always.value(Eval.scala:194)
[error]         at Main$.main(Main.scala:28)
[error]         at Main.main(Main.scala)
[error]         at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
[error]         at java.base/java.lang.reflect.Method.invoke(Method.java:580)
[error] stack trace is suppressed; run last Compile / run for the full output
[error] (Compile / run) scala.NotImplementedError: an implementation is missing
[error] Total time: 1 s, completed Apr 14, 2025, 8:51:27 AM
```
