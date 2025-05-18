import zio.{Console, UIO, ZIO, ZIOAppDefault}

object MovieApp extends ZIOAppDefault {

  final case class Movie(id: Int, language: String)

  final case class UnsupportedLanguage(language: String)

  final case class CompoundError(errs: List[UnsupportedLanguage])

  val movies = List(
    Movie(1, "En"),
    Movie(2, "Fr"),
    Movie(3, "Ru"),
    Movie(4, "Cz"),
    Movie(5, "Ru")
  )

  def movieToZIO(movie: Movie): ZIO[Any, UnsupportedLanguage, Unit] =
    if (movie.language == "Ru") Console.printLine(s"Found Movie $movie").orDie
    else ZIO.fail(UnsupportedLanguage(movie.language))

  def scanMovies(movies: List[Movie]): UIO[String] = for {

    res <- ZIO.foreach(movies) { movie =>
      movieToZIO(movie).either
    }

    compoundError = CompoundError(
      res.collect{case Left(er) => er}
    )

    _ <- ZIO
      .when(compoundError.errs.nonEmpty)(Console.printLine(compoundError.errs))
      .orDie

  } yield "Movie Scan is Over"

  def run = scanMovies(movies)
    .flatMap(res => Console.printLine(res))

}
