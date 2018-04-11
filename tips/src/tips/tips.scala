package tips

import java.util.concurrent.CompletableFuture

import cats._
import cats.data.{Nested, OptionT}
import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import cats.mtl.implicits._
import io.catbird.util._
import monix.eval.Task
import monix.eval.Task._
import monix.execution.Scheduler.Implicits.global

import scala.compat.java8.FunctionConverters._
import scala.concurrent.Future
import scala.concurrent.duration.Duration

object catstips extends App {

  val id = 10
  case class User(id: Int, name: String, email: String)

  def getUser(id: Int): Future[Option[User]] = {
    User(id, s"name-$id", s"$id-email@gmail.com").some.pure[Future]
  }

  // case 1 : monad transformer
  // when : 중첩된 map이 보인다면 monad transformer를 고려해보라
  val user = getUser(10).map(_.map(_.name.toUpperCase))

  Functor[Future].compose[Option].map(getUser(10))(_.name.toUpperCase)

  val result = OptionT(getUser(10)).map(_.name.toUpperCase)
  println(result.value)

  // case 2 : 여러개의 데이터를 동시에 처리하고 싶다면 traverse를 고려해보라
  def getFriends(ids: List[Int]): Future[List[User]] = {
    val lfou: List[Future[Option[User]]] = ids.map(getUser)
    val flou: Future[List[Option[User]]] = Future.sequence(lfou)
    flou.map(_.flatten)
  }

  case class Item(id: Int, name: String)
  val itemIds: List[Int] = ???

  def findItemById(itemId: Int): Future[Option[Item]] = {
    Future.successful(Option(Item(itemId, "$")))
  }
  def getFriends1(ids: List[Int]): Future[List[User]] =
    ids.traverse(getUser).map(_.flatten)

  def getFriends2(ids: List[Int]): Future[List[User]] =
    ids.traverse(getUser).map(_.flatten)

  val ids = (1 to 10).toList

  // 값의 초기화는 future
  val tf = 1.pure[com.twitter.util.Future]
  val sf = 1.pure[scala.concurrent.Future]

  /*
  trait ItemRepository {
    def findById(id: Long): CompletableFuture[Item]
  }
  trait OptionRepository {
    def findByItemId(itemId: Long): CompletableFuture[List[ItemOption]]
  }

  val itemRepository = new ItemRepository {
    def findById(id: Long): CompletableFuture[Item] = CompletableFuture.completedFuture(Item(id, s"name-$id"))
  }

  val optionRepository = new OptionRepository {
    def findByItemId(itemId: Long): CompletableFuture[List[ItemOption]] =
      CompletableFuture.completedFuture(
        List(
          ItemOption(math.random().longValue(), itemId, s"option-1-$itemId"),
          ItemOption(math.random().longValue(), itemId, s"option-2-$itemId")
        ))
  }
  import scala.concurrent.{ Future => ScalaFuture }
  def getItemScala(id: Long): ScalaFuture[Either[Throwable, ItemDto]] = {
    if(id < 0)
      ScalaFuture.successful(Left(new IllegalArgumentException(s"유효하지 않은 ID입니다. $id")))
    else
      itemRepository.findById(id)
        .thenCompose(item =>
          optionRepository.findByItemId(item.id)
            .thenApply(option =>
              Right(ItemDto(item.id, item.name, option)))
        )
  }
   */

  // java completable future
  val a1: CompletableFuture[Int] = CompletableFuture.completedFuture(10)
  // map
  val b1: CompletableFuture[Int] = a1.thenApply(_ + 10)
  // flatMap
  val c1 = b1.thenCompose(x => CompletableFuture.completedFuture(x * 10))

  implicit val completableFutureInstance = new Monad[CompletableFuture]
  with StackSafeMonad[CompletableFuture] {
    def pure[A](x: A): CompletableFuture[A] =
      CompletableFuture.completedFuture(x)

    def flatMap[A, B](fa: CompletableFuture[A])(
        f: A => CompletableFuture[B]): CompletableFuture[B] =
      fa.thenCompose(f.asJava)
  }

  val a2 = 10.pure[CompletableFuture]
  val b2 = a2.map(x => x + 10)
  val c2 = b2.flatMap(x => (x * 10).pure[CompletableFuture])
  println(c2)

  // combineK을 이용한 cache miss 처리
  def fetchCache[F[_]](id: Int)(implicit F: Effect[F]): OptionT[F, User] =
    OptionT(F.delay {
      println("cached run")
      if (id % 2 == 0)
        User(id, s"cached-name-$id", s"$id@gmail.com").some
      else none[User]
    })

  // combineK을 이용한 cache miss 처리
  def fetchCacheError[F[_]](id: Int)(implicit F: Effect[F]): OptionT[F, User] =
    OptionT(F.delay {
      throw new Exception("hello")
      println("cached run")
      if (id % 2 == 0)
        User(id, s"cached-name-$id", s"$id@gmail.com").some
      else none[User]
    })

  def fetchDB[F[_]](id: Int)(implicit F: Effect[F]): OptionT[F, User] =
    OptionT.liftF(F.delay {
      println("db run")
      User(id, s"db-name-$id", s"$id@gmail.com")
    })

  SemigroupK[OptionT[Task, ?]].combineK(
    fetchCache[Task](id),
    fetchDB[Task](id)
  )

  def fetch0[F[_]: Effect](id: Int): OptionT[F, User] = {
    fetchCache[F](id)
  }

  def fetch[F[_]: Effect](id: Int): OptionT[F, User] =
    fetchCache[F](id) <+> fetchDB[F](id)

  val x = 1
  val a = fetchCache[Task](x)
  val b = fetchDB[Task](x)
  val c = a <+> b: OptionT[Task, User]
  val d = c.value.runSyncUnsafe(Duration.Inf)

  // filterA를 이용한 비동기 필터링
  def isActiveUser[F[_]](id: Int)(implicit F: Effect[F]): F[Boolean] =
    F.async(
      cb =>
        if (id % 5 == 0) cb(Right(false))
        else cb(Right(true)))

  def getFriends3[F[_]: Effect](ids: List[Int]): F[List[User]] =
    ids.traverse(fetch[F](_).value).map(_.flatten)

  val activeUser = ids.filterA(isActiveUser[IO])
  println(activeUser.unsafeRunSync())

  val iAmPure = (x: Int, y: Int) => x + y

  trait Functor[F[_]] {
    def map[A, B](fa: F[A])(f: A => B): F[B]
  }

  trait Monad[F[_]] {
    def pure[A](a: A): F[A]
    def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
  }

  val listF: Task[List[Int]] = (1 to 10).toList.pure[Task]
  val res: Nested[Task, List, Int] = Nested(listF).map(_ + 10)
  println(res.value.runSyncUnsafe(Duration.Inf))
}
