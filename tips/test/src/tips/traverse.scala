package tips

import java.time.{Instant, LocalDateTime}

import cats.effect.{Effect, IO}
import monix.eval.Task
import utest._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Created by ikhoon on 08/04/2018.
  */
object TraverseTest extends TestSuite {

  val tests = Tests {

    'traverse {

      def withTS(block: => Unit) = {
        println(s"# Start  - ${LocalDateTime.now()}")
        block
        println(s"# Finish - ${LocalDateTime.now()}")
      }
      import cats.implicits._

      case class Item(id: Int, name: String)

      import java.util.concurrent.Executors

      implicit val ec =
        ExecutionContext.fromExecutor(Executors.newFixedThreadPool(20))
      val itemIds: List[Int] = (1 to 10).toList
      //  val items: Future[List[Item]] = itemIds.traverse(findItemById)

      def findItemById(itemId: Int): Future[Item] = Future {
        Thread.sleep(500)
        println(Instant.now)
        Item(itemId, "$")
      }

      def findItemByIdEffect[F[_]](itemId: Int)(
          implicit F: Effect[F]): F[Item] =
        F.delay {
          Thread.sleep(500)
          println("Find Item by ID 2 : " + Instant.now)
          Item(itemId, "$")
        }

      def findItemByIdTask(itemId: Int): Task[Item] = Task {
        Thread.sleep(500)
        println("Find Item by ID 2 : " + Instant.now)
        Item(itemId, "$")
      }
      //  import cats.implicits._
      //  withTS {
      //    val items =
      //      itemIds
      //        .grouped(10)
      //        .toList
      //        .foldLeft(List.empty[Item].pure[Future]) { (acc, group) =>
      //          acc.flatMap(xs => group.traverse(findItemById).map(xs ::: _))
      //        }
      //
      //    val xs = Await.result(items, Duration.Inf)
      //    println(xs)
      //  }
      'io_traverse {
        withTS {
          val res = itemIds.traverse(findItemByIdEffect[IO])
          val xs = res.unsafeRunSync()
          println(xs)
        }
      }

      'io_parTraverse {
        withTS {
          val res = itemIds.parTraverse(findItemByIdEffect[IO](_))
          val xs = res.unsafeRunSync()
          println(xs)
        }
      }

      // 병렬처리됨
      'io_shift_parTraverse {
        withTS {
          val res = itemIds.parTraverse(IO.shift *> findItemByIdEffect[IO](_))
          val xs = res.unsafeRunSync()
          println(xs)
        }
      }

      'io_shift_traverse {
        withTS {
          val res = itemIds.traverse(IO.shift *> findItemByIdEffect[IO](_))
          val xs = res.unsafeRunSync()
          println(xs)
        }
      }

      'task_parTraverse_effect {
        import monix.eval.instances._
        import monix.execution.Scheduler.Implicits.global
        withTS {
          val res = itemIds.parTraverse(findItemByIdEffect[Task])
          val xs = res.runAsync
          println(Await.result(xs, Duration.Inf))
        }
      }

      // 병렬처리됨
      'task_parTraverse_task {
        import monix.eval.instances._
        import monix.execution.Scheduler.Implicits.global
        withTS {
          val res = itemIds.parTraverse(findItemByIdTask)
          val xs = res.runAsync
          println(Await.result(xs, Duration.Inf))
        }
      }

      'task_traverse_effect {
        import monix.execution.Scheduler.Implicits.global
        withTS {
          val res = itemIds.traverse(findItemByIdEffect[Task])
          val xs = res.runAsync
          println(Await.result(xs, Duration.Inf))
        }
      }

      'task_traverse_task {
        import monix.execution.Scheduler.Implicits.global
        withTS {
          val res = itemIds.traverse(findItemByIdTask)
          val xs = res.runAsync
          println(Await.result(xs, Duration.Inf))
        }
      }

      //  {
      //    val start = System.nanoTime()
      //    val res = itemIds.traverse(findItemById)
      //    Await.result(res, Duration.Inf)
      //    val interval = (System.nanoTime() - start) / 500000.0
      //    println(s"interval: $interval ms")
      //  }

      //  {
      //    val start = System.nanoTime()
      //    val eventualList = itemIds.grouped(10).toList.traverse[Future, List[Option[Item]]](group => {
      //      println(group)
      //      group.traverse(findItemById)
      //    })
      //    val xs = Await.result(eventualList, Duration.Inf)
      //    val interval = (System.nanoTime() - start) / 500000.0
      //    println(s"2 interval: $interval ms")
      //    println(xs)
      //  }
    }
  }
}
