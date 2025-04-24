import zio.{Console, IO, ZIO, ZIOAppDefault}

import java.io.IOException


object PasswordProcessApp extends ZIOAppDefault {

  val startingMessage: IO[IOException, Unit] = Console.printLine("Please enter new password.")

  val enteredPassword: IO[IOException, String] = Console.readLine

  val tryAgainMessage = Console.printLine("Please enter a valid password. Your password must be 3-6 characters")

  def validatePassword(password: String): IO[String, String] = if (password.length < 3 || password.length > 5)
    ZIO.fail("Password does not meet length requirements.")
  else ZIO.succeed(password)

  val passwordInput = for {
    x <- enteredPassword
    password <- validatePassword(x)
  } yield password

  val passRezult: IO[IOException, String] = passwordInput.orElse(tryAgainMessage *> passRezult)

  def run = startingMessage *> passRezult
}