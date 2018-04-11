package tips
import cats.{Monad, StackSafeMonad}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import utest._

/**
  * Created by Liam.M on 2018. 03. 20..
  */
object MonadInstanceTest extends TestSuite {

  val tests = Tests {

    'either_instance {

      import cats.implicits._

      val a: Either[Throwable, Int] = Right(10)

      def f(x: Int): Either[Throwable, String] = Right(x.toString)

      def g(ex: Throwable): Either[Exception, String] =
        Left(new Exception(ex.getMessage))

      val b: Either[Throwable, String] = a.flatMap(f)
      val c: Either[Exception, String] = b.left.flatMap(g)
      1.some

      // Either monad instance
      implicit def eitherInstance[A]: Monad[({ type L[B] = Either[A, B] })#L] =
        new Monad[({ type L[B] = Either[A, B] })#L]
        with StackSafeMonad[({ type L[B] = Either[A, B] })#L] {

          def pure[B](x: B): Either[A, B] = Right(x)

          def flatMap[B, C](fa: Either[A, B])(
              f: B => Either[A, C]): Either[A, C] = fa.flatMap(f)

        }

    }
  }

}
