# `scala-retry`

Retries.

## Syntax

`MonadThrow` instances enriched with three new methods.

### `retry`, match retryable errors in partial function

```scala
def retry(pf: PartialFunction[Throwable, Unit])(implicit R: Retry[F]): F[A]
```

```scala
fa.retry {
  case e: MyError if e.isRetryable => ()
}
```

### `retryNarrow`, match retryable errors by type

```scala
def retryNarrow[EE <: Throwable: ClassTag](implicit R: Retry[F]): F[A]
```

```scala
fa.retry[SomeRetryableError]
```

### `retryWith`, match retryable errors in partial function with effectful return

```scala
def retryWith(pf: PartialFunction[Throwable, F[Unit]])(implicit R: Retry[F]): F[A]
```

```scala
fa.retryWith {
  case e: MyError if e.isRetryable =>
    Logger[F].info(s"Caught retryable error: $e")
}
```

## Examples

Code example in [`examples/src/main/scala/Main.scala`](examples/src/main/scala/Main.scala):

```scala
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
```

### Running the examples

```
sbt run
```

```
Using a retry policy with jittered exponential backoff, 5 retries
Looks like this is not implemented
Looks like this is not implemented
Looks like this is not implemented
Looks like this is not implemented
Looks like this is not implemented
io.github.biochimia.retry.OutOfRetriesException: retryable error caught, but no retries left
        at io.github.biochimia.retry.Retry$$anonfun$attempt$1$1.applyOrElse(Retry.scala:90)
        at io.github.biochimia.retry.Retry$$anonfun$attempt$1$1.applyOrElse(Retry.scala:85)
        at cats.ApplicativeError.$anonfun$recoverWith$1(ApplicativeError.scala:172)
        at io.github.biochimia.retry.instances.EvalTryInstances$$anon$1.$anonfun$handleErrorWith$1(evalTry.scala:40)
        at cats.Eval$.loop$1(Eval.scala:361)
        at cats.Eval$.cats$Eval$$evaluate(Eval.scala:386)
        at cats.Eval$FlatMap.value(Eval.scala:307)
        at Main$.main(Main.scala:34)
        at Main.main(Main.scala)
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        at sbt.Run.invokeMain(Run.scala:135)
        at sbt.Run.execute$1(Run.scala:85)
        at sbt.Run.$anonfun$runWithLoader$5(Run.scala:112)
        at sbt.Run$.executeSuccess(Run.scala:178)
        at sbt.Run.runWithLoader(Run.scala:112)
        at sbt.Defaults$.$anonfun$bgRunTask$6(Defaults.scala:2072)
        at sbt.Defaults$.$anonfun$termWrapper$2(Defaults.scala:2011)
        at scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.java:23)
        at scala.util.Try$.apply(Try.scala:213)
        at sbt.internal.BackgroundThreadPool$BackgroundRunnable.run(DefaultBackgroundJobService.scala:378)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
        at java.base/java.lang.Thread.run(Thread.java:1575)
Caused by: scala.NotImplementedError: an implementation is missing
        at scala.Predef$.$qmark$qmark$qmark(Predef.scala:344)
        at Main$.$anonfun$main$2(Main.scala:30)
        at scala.util.Try$.apply(Try.scala:217)
        at io.github.biochimia.retry.support.EvalTry$.$anonfun$apply$1(EvalTry.scala:25)
        at cats.Always.value(Eval.scala:194)
        ... 20 more
```
