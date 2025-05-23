import zio.{Console, ExitCode, Scope, Task, UIO, ZIO, ZIOAppDefault}

import java.io.{File, IOException}
import scala.io.{BufferedSource, Source}
import scala.collection.JavaConverters.*

object FilesReaderApp extends ZIOAppDefault {

  val startMessage = Console.printLine("Enter dir:")
  val enteredDir = Console.readLine

  final case class EndStatus(status: String)

  final case class InappropriateFile(message: String)

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

  def fileToBufferedSource(file: File): Task[List[BufferedSource]] = {
    val fileName = file.getName
    val extension = fileName.slice(fileName.length - 4, fileName.length)
    if (extension == ".txt") {
      ZIO.attempt(List(Source.fromFile(file)))
    } else ZIO.fail(new IllegalArgumentException("Inappropriate file format!"))
  }

  def walkRecursion(directory: File): Task[List[BufferedSource]] = directory match {

    case directory if directory.isDirectory && directory.exists() =>

      val directoryList = directory.listFiles().toList

      ZIO.foreachPar(directoryList)(file => walkRecursion(file))
        .map(_.flatten())

    case file if file.isFile => fileToBufferedSource(file)

    case _ => ZIO.fail(new IllegalArgumentException("Inappropriate file format!"))
  }
  
  
  def acquire(directory: File): Task[List[BufferedSource]] = walkRecursion(directory)
    
    
//    if (directory.isDirectory()) {
//
//      val walkFiles = directory.listFiles()
//        //        .filter(_.isFile)
//        .map {
//          case directory if directory.isDirectory => acquire(directory) 
//          case file if file.isFile => fileToBufferedSource(file)
//            
//          case _ => ZIO.fail(new IllegalArgumentException("Inappropriate file format!")) 
//        }
//        .toList
//
//      val walkList: List[BufferedSource] = walkFiles.collect { case bufferedSource: BufferedSource => bufferedSource }
//
//
//  }

  def release(resource: List[BufferedSource]): UIO[List[Unit]] = {
    ZIO.foreach(resource) { bufferedSource =>
      ZIO.succeed(bufferedSource.close())
    }
  }

  def readFileContent(file: File): ZIO[Any & Scope, Throwable, List[String]] = ZIO
    .acquireRelease(acquire(file))(list => release(list))
    .flatMap(sources => ZIO.foreach(sources) { source =>
      ZIO.attempt(source.getLines.mkString(";"))
    })
//    .foldZIO(
//      failure => ZIO.succeed(failure.getMessage),
//      success => ZIO.succeed(success)
//    )

  def poroccessReading = for {
    _ <- startMessage
    enterDirectory <- enteredDir
    useFile <- checkDirectory(enterDirectory)
    content <- readFileContent(useFile)

  } yield content.mkString("\n")

  override def run = poroccessReading.debug.exitCode
}
