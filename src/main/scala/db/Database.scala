package db

import zio.{IO, Random, UIO, ZIO}



case class Database(dbName: String) {
  def write(fileName: String, rec: String): UIO[String] = ZIO.succeed((s"[$dbName] Write [$fileName][$rec]")).debug

  def close: UIO[String] = ZIO.succeed(s"Connection Closed").debug
}

object Database {
  def connect(dbName: String): IO[String, Database] = {
    for {
      n <- Random.nextIntBetween(0, 2)
      _ <- ZIO.fail("Failed to establish db connection").when(n == 0)
      conn <- ZIO.succeed(new Database(dbName))
    } yield conn
  }
}
