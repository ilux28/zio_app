import zio.{Console, ExitCode, Scope, Task, UIO, ZIO, ZIOAppDefault}

import java.io.File
import scala.io.{BufferedSource, Source}
import scala.collection.JavaConverters.*

object FilesReaderApp extends ZIOAppDefault {

  val startMessage = Console.printLine("Enter dir:")
  val enteredDir = Console.readLine

  final case class EndStatus(status: String)

  val useDefaultDirectoryQuestion = Console.printLine("Use default directory?")

  val useDefaultDirectoryAnswer = Console.readLine

  val defaultDirStr = "src/main/resources/bania"
  val defaultDirStr1 = "src/main/resources/bania/example1.txt"


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

  def processFile(file: File): Task[List[BufferedSource]] = {
    val fileName = file.getName
    val extension = fileName.slice(fileName.length - 4, fileName.length)
    if (extension == ".txt") {
      ZIO.attempt(List(Source.fromFile(file)))
    } else ZIO.fail(new IllegalArgumentException("Inappropriate file format!"))
  }
  
  
  def walkRecursion(directory: File) = directory match {
    case directory if directory.isDirectory => walkRecursion(directory)
    case file if file.isFile => processFile(file)

    case _ => ZIO.fail(new IllegalArgumentException("Inappropriate file format!"))
  }
  
  
  def acquire(directory: File): Task[List[BufferedSource]] = {

    if (directory.isDirectory()) {

      val walkFiles = directory.listFiles()
        //        .filter(_.isFile)
        .map {
          case directory if directory.isDirectory => acquire(directory) 
          case file if file.isFile => processFile(file)
            
          case _ => ZIO.fail(new IllegalArgumentException("Inappropriate file format!")) 
        }
        .toList

      val walkList: List[BufferedSource] = walkFiles.collect { case bufferedSource: BufferedSource => bufferedSource }

      ZIO.foreach(walkList) { file =>
        ZIO.attempt(file)
      }

    } else if (directory.isFile) ZIO.attempt(List(Source.fromFile(directory)))
    else ZIO.fail()

  }

  def release(resource: List[BufferedSource]): UIO[List[Unit]] = {
    ZIO.foreach(resource) { bufferedSource =>
      ZIO.succeed(bufferedSource.close())
    }
  }

  def readFileContent(file: File) = ZIO
    .acquireRelease(acquire(file))(list => release(list))
    .flatMap(sources => ZIO.foreach(sources) { source =>
      ZIO.attempt(source.getLines.mkString(";"))
    })

  def poroccessReading: ZIO[Any & Scope, Serializable, String] = for {
    _ <- startMessage
    enterDirectory <- enteredDir
    useFile <- checkDirectory(enterDirectory)
    content <- readFileContent(useFile)


  } yield content.mkString("\n")

  override def run = poroccessReading.debug.exitCode
}
