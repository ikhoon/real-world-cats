package tips

import java.util.concurrent.CompletableFuture

import utest._
import cats._
import cats.data.{Nested, OptionT}
import cats.effect.Effect
import cats.implicits._
import tips.catstips.User


/**
  * Created by ikhoon on 29/03/2018.
  */
class SemigroupKTest extends TestSuite {
  val tests = Tests {

    def fetchCache[F[_]](id: Int)(implicit F: Effect[F]): F[Option[User]] =
      F.delay {
        println("cached run")
        if (id % 2 == 0)
          User(id, s"cached-name-$id", s"$id@gmail.com").some
        else none[User]
      }

    // combineK을 이용한 cache miss 처리
    def fetchCacheError[F[_]](id: Int)(implicit F: Effect[F]): OptionT[F, User] =
      OptionT(F.delay {
        throw new Exception("hello")
        println("cached run")
        if (id % 2 == 0)
          User(id, s"cached-name-$id", s"$id@gmail.com").some
        else none[User]
      })

    def fetchDB[F[_]](id: Int)(implicit F: Effect[F]): F[Option[User]] =
      F.delay {
        println("db run")
        User(id, s"db-name-$id", s"$id@gmail.com").some
      }

    def fetchCache1[F[_]](id: Int)(implicit F: Effect[F]): OptionT[F, User] =
      OptionT(F.delay {
        println("cached run")
        if (id % 2 == 0)
          User(id, s"cached-name-$id", s"$id@gmail.com").some
        else none[User]
      })

    def fetchDB1[F[_]](id: Int)(implicit F: Effect[F]): OptionT[F, User] =
      OptionT.liftF(F.delay {
        println("db run")
        User(id, s"db-name-$id", s"$id@gmail.com")
      })

//    def fetch[F[_]: Effect](id: Int): OptionT[F, User] =
//      (OptionT(fetchCache[F](id)): OptionT[F, User]) <+> (OptionT(fetchDB[F](id)): OptionT[F, User])
    //OptionT(fetchCache[F](id).handleError(_ => none)) <+> OptionT(fetchDB[F](id))
//    def fetch1[F[_]: Effect: Monad](id: Int): OptionT[F, User] =
//      fetchCache1[F](id) <+> fetchDB1[F](id)


  }


}
