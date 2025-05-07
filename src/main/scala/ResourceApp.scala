import db.Database
import zio.{Console, Exit, ExitCode, IO, RIO, Scope, Task, UIO, URIO, ZIO, ZIOAppDefault}

import scala.io.{BufferedSource, Source}

object ResourceApp extends ZIOAppDefault {

  //  def acquire(file: String): Task[BufferedSource] = ZIO.attempt(Source.fromFile(file))
  //
  //  def use(resource: BufferedSource): ZIO[Any, Nothing, String] = ZIO.succeed {
  //    throw new RuntimeException("Failure...")
  //    resource.getLines.mkString(" ")
  //  }
  //
  //  def release(resource: BufferedSource, exit: Exit[Throwable, String]): UIO[Unit] = {
  //
  //    ZIO.succeed {
  //        exit match
  //          case Exit.Success(v) => v
  //          case Exit.Failure(cause) => if (cause.isFailure) "failure" else "defect"
  //      }
  //      .debug *> ZIO.succeed(resource.close())
  //  }
  //
  //  val anEffect = ZIO.acquireReleaseExitWith(acquire("src/main/resources/example.txt"))(release)(use)
  //
  //
  //  val db1WithUser: Task[Database] = Database.connect("db#1", "my-user")
  //  val db2WithUser: Task[Database] = Database.connect("db#2", "my-user")


  //  val database1 = ZIO.acquireReleaseWith(Database.connect("db#1", "my-user"))(db => db.close)
  //  val database2: ZIO.Release[Any, Throwable, Database] = ZIO.acquireReleaseWith(Database.connect("db#2", "my-user"))(db => db.close)
  //
  //  def dbWithFinalizer(dbName: String, user: String): ZIO[Scope, Throwable, Database] =
  //    Database.connect(dbName, user).withFinalizer(_.close)
  //
  //  val ef: ZIO[Scope, Throwable, Database] = dbWithFinalizer("db#2", "my-user")
  //
  //  val dbWorkflow = database1(db1 =>
  //    db1.read(1).flatMap(rec1 => database2(db2 =>
  //      db2.read(2).flatMap(rec2 => db2.write(rec1 + rec2))
  //    ))
  //  )


  def acquire(file: String): Task[BufferedSource] = ZIO.attempt(Source.fromFile(file))

  def release(resource: BufferedSource): UIO[Unit] = ZIO.succeed(resource.close())

  val file: RIO[Any & Scope, BufferedSource] = ZIO
    .acquireRelease(acquire("src/main/resources/example.txt"))(release)

  val fileContent: Task[String] = ZIO.scoped {
    for {
      source <- file
      content <- ZIO.attempt(source.getLines.mkString(";"))
    } yield content
  }

  def run: URIO[Any, ExitCode] = fileContent.debug.exitCode


  //  def withDatabase(user: String, dbName: String)(use: Database => Task[String]): Task[String] =
  //    ZIO.acquireReleaseWith(Database.connect(dbName, user))(_.close)(use)
  //
  //  def use(db1: Database, db2: Database) = for {
  //    rec1 <- db1.read(1)
  //    rec2 <- db2.read(2)
  //    res <- db2.write(rec1 + rec2)
  //  } yield res

  //  val dbWorkflow = withDatabase("db#1", "my-user") { db1 =>
  //    withDatabase("db#2", "my-user") { db2 =>
  //      use(db1, db2)
  //    }
  //  }


  //  def run = dbWorkflow


  //  override def run = dbWorkflow.exitCode

}
