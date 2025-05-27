import ResourceApp.file
import db.Database
import zio.{Console, ExitCode, IO, Scope, Task, UIO, URIO, ZIO, ZIOAppDefault}

import java.io.{File, IOException}
import scala.io.{BufferedSource, Source}
import scala.collection.JavaConverters.*

object FilesReaderApp extends ZIOAppDefault {

  val startMessage = Console.printLine("Enter dir:")
  val enteredDir = Console.readLine

  final case class EndStatus(status: String)

  val defaultDirStr = "src/main/resources/bania"

  val usingQuestionEffect = Console.printLine("Directory not found. Use default directory?")

  val yesOrNoEffect: ZIO[Any, Serializable, File] = for {
    res <- Console.readLine
    result <- if (res.toLowerCase == "yes") ZIO.succeed(new File(defaultDirStr))
    else ZIO.fail(ExitCode.failure)
  } yield result

  def checkDirectory(file: String): ZIO[Any, Serializable, File] = {
    val directory = new File(file)
    if (directory.exists()) {
      ZIO.succeed(directory)
    } else {
      usingQuestionEffect *> yesOrNoEffect
    }
  }

  def fileToContentStr(file: File): IO[String, String] = {

    val fileName = file.getName
    val extension = fileName.slice(fileName.length - 4, fileName.length)
    if (extension == ".txt") {

      val readStr = ZIO.scoped {
        for {
          source <- acureReleaseFile(fileName)
          content <- ZIO.attempt(source.getLines.mkString(";"))
        } yield content
      }
      readStr

    } else ZIO.fail(new IllegalArgumentException("Inappropriate file format!"))
  }
    .foldZIO(
      failure => ZIO.fail(failure.getMessage),
      success => ZIO.succeed(success)
    )

  def readerRecursion(directory: File): IO[List[String], List[String]] = {

    if (directory.exists() && directory.isDirectory) {
      val files: List[File] = directory.listFiles().toList

      val ioResults: List[IO[List[String], List[String]]] = files.map { file =>
        if (file.isDirectory) {
          readerRecursion(file)
        } else if (file.isFile) {
          fileToContentStr(file)
            .mapError(err => List(err))
            .map(content => List(s"$content\n"))
        } else {
          val listStr: List[String] = List.empty
          ZIO.attempt(listStr).mapError(_ => List.empty[String])
        }
      }

      val collected: IO[List[String], List[String]] = ZIO
        .collectAllPar(ioResults.map(_.either))
        .flatMap { results =>
          val (failures, successes) = results.partitionMap(identity)
          if (failures.nonEmpty) ZIO.succeed(failures.flatten)
          else ZIO.succeed(successes.flatten)
        }

      collected

    } else if (directory.exists() && directory.isFile) {
      fileToContentStr(directory)
        .map(content => List(content))
        .mapError(err => List(err))
        
    } else {
      ZIO
        .attempt(List.empty[String])
        .mapError(_ => List("Inappropriate file format!"))
    }
  }

  def acquire(file: String): Task[BufferedSource] = ZIO.attempt(Source.fromFile(file))

  def release(resource: BufferedSource): UIO[Unit] = ZIO.succeed(resource.close())

  def acureReleaseFile(file: String): ZIO[Any & Scope, Throwable, BufferedSource] = ZIO
    .acquireRelease(acquire(file))(source => release(source))

  val acureReleaseDb = ZIO.acquireRelease(Database.connect("db#1"))(db => db.close)


  def poroccessReadingEffect = for {
    _ <- startMessage
    enterDirectory <- enteredDir
    useFile <- checkDirectory(enterDirectory)
    readContent <- readerRecursion(useFile)
//      .mapError(err => err.foreach(errStr => Console.printLine(errStr)))

  } yield readContent.mkString(" ")

  //    left <- either.left
  //    _ <- Console.printLine(left))


  //    _ <- Console.printLine(left.map(str => s"$str\n"))

  //    resultWriting <- acureReleaseDb.map(_.write("src/main/resources/bania/reading_files.txt", right.mkString("")))

  //    resultStr <- resultWriting

  val result = poroccessReadingEffect.foldZIO(
    failure => {
      failure match {
        case str: String => ZIO.succeed(EndStatus(str))
        case _ => ZIO.succeed(EndStatus("Failed to establish db connection"))
      }
    },
    success => ZIO.succeed(EndStatus(success))
  )

  override def run = result.debug
}
