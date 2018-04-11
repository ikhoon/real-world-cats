package tips

import java.util.concurrent.CompletableFuture

import cats.{Monad, MonadError, StackSafeMonad}

import scala.compat.java8.FunctionConverters._
import utest._



object MonadTest extends TestSuite {
  val tests = Tests {
    case class ItemOption(id: Long, itemId: Long, optionName: String)
    case class ItemDto(id: Long, name: String, options: List[ItemOption])
    case class Item(id: Long, name: String)
    'javaFuture {

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

      def errorHandler(ex: Throwable): Either[Throwable, ItemDto] = Left(ex)

      def getItem(id: Long): CompletableFuture[Either[Throwable, ItemDto]] =
        if(id < 0) CompletableFuture.completedFuture(
            Left(new IllegalArgumentException(s"유효하지 않은 ID입니다. $id")))
        else
          itemRepository.findById(id)
            .thenCompose(item =>
              optionRepository.findByItemId(item.id)
                .thenApply(option =>
                  Right(ItemDto(item.id, item.name, option))))

      println(getItem(10).get())
    }

    'scalaFuture {
      import scala.concurrent.{Future => ScalaFuture}
      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.concurrent.Await
      import scala.concurrent.duration.Duration

      trait ItemRepository {
        def findById(id: Long): ScalaFuture[Item]
      }
      trait OptionRepository {
        def findByItemId(itemId: Long): ScalaFuture[List[ItemOption]]
      }

      val itemRepository = new ItemRepository {
        def findById(id: Long): ScalaFuture[Item] = ScalaFuture.successful(Item(id, s"name-$id"))
      }
      val optionRepository = new OptionRepository {
        def findByItemId(itemId: Long): ScalaFuture[List[ItemOption]] =
          ScalaFuture.successful(
            List(
              ItemOption(math.random().longValue(), itemId, s"option-1-$itemId"),
              ItemOption(math.random().longValue(), itemId, s"option-2-$itemId")
            ))
      }

      def getItem(id: Long): ScalaFuture[Either[Throwable, ItemDto]] =
        if(id < 0) ScalaFuture.successful(
          Left(new IllegalArgumentException(s"유효하지 않은 ID입니다. $id")))
        else
          itemRepository.findById(id)
            .flatMap(item =>
              optionRepository.findByItemId(item.id)
                .map(option =>
                  Right(ItemDto(item.id, item.name, option))))
            .recover { case ex: Throwable => Left(ex)}

      println(Await.result(getItem(10), Duration.Inf))

    }

    'twitterFuture {
      import com.twitter.util.{ Future => TwitterFuture }
      import com.twitter.util.{Await, Duration}


      trait ItemRepository {
        def findById(id: Long): TwitterFuture[Item]
      }
      trait OptionRepository {
        def findByItemId(itemId: Long): TwitterFuture[List[ItemOption]]
      }

      val itemRepository = new ItemRepository {
        def findById(id: Long): TwitterFuture[Item] = TwitterFuture.value(Item(id, s"name-$id"))
      }
      val optionRepository = new OptionRepository {
        def findByItemId(itemId: Long): TwitterFuture[List[ItemOption]] =
          TwitterFuture.value(
            List(
              ItemOption(math.random().longValue(), itemId, s"option-1-$itemId"),
              ItemOption(math.random().longValue(), itemId, s"option-2-$itemId")
            ))
      }

      def getItem(id: Long): TwitterFuture[Either[Throwable, ItemDto]] =
        if(id < 0) TwitterFuture.value(
            Left(new IllegalArgumentException(s"유효하지 않은 ID입니다. $id")))
        else
          itemRepository.findById(id)
            .flatMap(item =>
              optionRepository.findByItemId(item.id)
                .map(option =>
                  Right(ItemDto(item.id, item.name, option))))
            .handle { case ex: Throwable => Left(ex) }


      println(Await.result(getItem(10), Duration.Top))
    }

    'task {
      import monix.eval.{Task => MonixTask}
      import scala.concurrent.duration.Duration
      import monix.execution.Scheduler.Implicits.global

      trait ItemRepository {
        def findById(id: Long): MonixTask[Item]
      }
      trait OptionRepository {
        def findByItemId(itemId: Long): MonixTask[List[ItemOption]]
      }

      val itemRepository = new ItemRepository {
        def findById(id: Long): MonixTask[Item] = MonixTask.pure(Item(id, s"name-$id"))
      }
      val optionRepository = new OptionRepository {
        def findByItemId(itemId: Long): MonixTask[List[ItemOption]] =
          MonixTask.pure(
            List(
              ItemOption(math.random().longValue(), itemId, s"option-1-$itemId"),
              ItemOption(math.random().longValue(), itemId, s"option-2-$itemId")
            ))
      }

      def getItem(id: Long): MonixTask[Either[Throwable, ItemDto]] =
        if(id < 0) MonixTask.pure(
          Left(new IllegalArgumentException(s"유효하지 않은 ID입니다. $id")))
        else
          itemRepository.findById(id)
            .flatMap(item =>
              optionRepository.findByItemId(item.id)
                .map(option =>
                  Right(ItemDto(item.id, item.name, option))))
            .onErrorRecover { case ex: Throwable => Left(ex) }

      println(getItem(10).runSyncUnsafe(Duration.Inf))
    }

    'tagless {
      import cats.implicits._
      import scala.concurrent.{Future => ScalaFuture}
      import com.twitter.util.{ Future => TwitterFuture }
      import monix.eval.{Task => MonixTask}
      import monix.execution.Scheduler.Implicits.global
      import io.catbird.util._

      implicit val completableFutureInstance = new Monad[CompletableFuture] with StackSafeMonad[CompletableFuture] {
        def pure[A](x: A): CompletableFuture[A] = CompletableFuture.completedFuture(x)

        def flatMap[A, B](fa: CompletableFuture[A])(f: A => CompletableFuture[B]): CompletableFuture[B] =
          fa.thenCompose(f.asJava)
      }

      trait ItemRepository[F[_]] {
        def findById(id: Long): F[Item]
      }
      trait OptionRepository[F[_]] {
        def findByItemId(itemId: Long): F[List[ItemOption]]
      }

      implicit def itemRepository[F[_]](implicit F: Monad[F]) = new ItemRepository[F] {
        def findById(id: Long): F[Item] = F.pure(Item(id, s"name-$id"))
      }
      implicit def optionRepository[F[_]](implicit F: Monad[F])= new OptionRepository[F] {
        def findByItemId(itemId: Long): F[List[ItemOption]] =
          F.pure(
            List(
              ItemOption(math.random().longValue(), itemId, s"option-1-$itemId"),
              ItemOption(math.random().longValue(), itemId, s"option-2-$itemId")
            ))
      }

      def getItem[F[_]]
        (id: Long)(implicit F: MonadError[F, Throwable])
      : F[Either[Throwable, ItemDto]] =
        if(id < 0)
          F.pure(Left(new IllegalArgumentException(s"유효하지 않은 ID입니다. $id")))
        else
          itemRepository[F].findById(id)
            .flatMap(item =>
              optionRepository[F].findByItemId(item.id)
                .map(option =>
                  Either.right[Throwable, ItemDto](
                    ItemDto(item.id, item.name, option))))
            .handleError(ex => Left(ex))

      val scalaFuture: ScalaFuture[Either[Throwable, ItemDto]] = getItem[ScalaFuture](10)
      val twiiterFuture: TwitterFuture[Either[Throwable, ItemDto]] = getItem[TwitterFuture](10)
      val monixTask: MonixTask[Either[Throwable, ItemDto]] = getItem[MonixTask](10)

    }
  }
}