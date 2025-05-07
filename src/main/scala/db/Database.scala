package db

import zio.{Task, UIO, ZIO}

case class Database(dbName: String, user: String) {
  def read(recId: Int): UIO[String] =
    ZIO.succeed(s"$dbName-$user-$recId").debug

  def write(rec: String): UIO[String] = ZIO.succeed((s"[$dbName] Write [$rec]")).debug

  def close: UIO[String] = ZIO.succeed(s"[$dbName] Connection Closed").debug
}

object Database {
  def connect(dbName: String, user: String): Task[Database] =
    ZIO.succeed(Database(dbName, user))
}
