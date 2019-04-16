package com.perhac.experiments.cats.free

import cats.data.EitherK
import cats.effect.Effect
import cats.free.Free
import cats.free.Free.inject
import cats.{InjectK, ~>}
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux
import monix.eval.Task
import monix.execution.Scheduler

import scala.util.Try

object CheckWeather {

  def transactor[F[_] : Effect]: Aux[F, Unit] = Transactor.fromDriverManager[F](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql://localhost:54321/generic",
    user = "generic",
    pass = "generic"
  )

  class ConsoleUserIOInterpreter[F[_]](implicit E: Effect[F]) extends (UserInteraction ~> F) {
    override def apply[A](fa: UserInteraction[A]): F[A] = fa match {
      case AskUser(prompt) => E.delay {
        println(prompt)
        scala.io.StdIn.readLine()
      }
      case TellUser(line) => E.delay(println(line))
    }
  }

  class DummyInterpreter[F[_]](implicit E: Effect[F]) extends (WeatherOperation ~> F) {
    val iVeBeenTo = List(
      "Reno", "Chicago", "Fargo", "Minnesota",
      "Buffalo", "Toronto", "Winslow", "Sarasota",
      "Whichta", "Tulsa", "Ottowa", "Oklahoma",
      "Tampa", "Panama", "Mattua", "LaPaloma",
      "Bangor", "Baltimore", "Salvador", "Amarillo",
      "Tocapillo", "Pocotello", "Amperdllo") // I've been everywhere, man

    override def apply[A](fa: WeatherOperation[A]): F[A] = fa match {
      case ListLocations => E.delay(iVeBeenTo)
      case WeatherForLocation(location) => E.delay {
        iVeBeenTo.find(_ == location).map(_ =>
          WeatherInfo(location, location.length, location.length * 2))
      }
    }
  }

  class DbConnectedInterpreter[F[_]](transactor: Transactor[F])(implicit E: Effect[F]) extends (WeatherOperation ~> F) {

    override def apply[A](fa: WeatherOperation[A]): F[A] = fa match {
      case ListLocations =>
        sql"SELECT name FROM entity ORDER BY name".query[String].to[List].transact(transactor)
      case WeatherForLocation(location) =>
        E.map(sql"select * from entity where name=$location".query[Entity].option.transact(transactor)) { optEntity =>
          for {
            entity <- optEntity
            parsedData <- Try(entity.data.toDouble).toOption
          } yield WeatherInfo(location, parsedData, parsedData * 2)
        }
    }
  }

  //@formatter:off
  final case class Entity(id: Long, name: String, data: String)
  final case class WeatherInfo(location: String, minTemp: Double, maxTemp: Double)

  sealed trait UserInteraction[T]
  final case class AskUser(prompt: String) extends UserInteraction[String]
  final case class TellUser(what: String) extends UserInteraction[Unit]

  sealed trait WeatherOperation[T]
  case object ListLocations extends WeatherOperation[List[String]]
  final case class WeatherForLocation(location: String) extends WeatherOperation[Option[WeatherInfo]]


  class Weather[F[_]](implicit I: InjectK[WeatherOperation, F]) {
    type WeatherOp[A] = Free[F, A]
    def listLocations: WeatherOp[List[String]] =
      inject[WeatherOperation, F](ListLocations)
    def weatherForLocation(location: String): WeatherOp[Option[WeatherInfo]] =
      inject[WeatherOperation, F](WeatherForLocation(location))
  }

  object Weather{
    implicit def weatherInj[F[_]](implicit I: InjectK[WeatherOperation, F]): Weather[F] = new Weather[F]
  }

  class UserIO[F[_]](implicit I: InjectK[UserInteraction, F]) {
    type UserIOOp[A] = Free[F, A]
    def askUser(prompt: String): UserIOOp[String] = inject[UserInteraction, F](AskUser(prompt))
    def tellUser(what: String): UserIOOp[Unit] = inject[UserInteraction, F](TellUser(what))
  }

  object UserIO{
    implicit def userIoInj[F[_]](implicit I: InjectK[UserInteraction, F]): UserIO[F] = new UserIO[F]
  }
  //@formatter:on

  type WeatherApp[A] = EitherK[WeatherOperation, UserInteraction, A]

  implicit val scheduler: Scheduler = Scheduler.Implicits.global

  def program(implicit W: Weather[WeatherApp], U: UserIO[WeatherApp]): Free[WeatherApp, Unit] = {
    import U._
    import W._
    for {
      _ <- tellUser("List of available locations:")
      allLocations <- listLocations
      _ <- tellUser(allLocations.mkString(", "))
      location <- askUser("Enter location:")
      weatherInfo <- weatherForLocation(location)
      _ <- tellUser(weatherInfo.fold(
        s"No Weather information currently available for $location")(wi =>
        s"Weather in $location: temperatures between ${wi.minTemp} and ${wi.maxTemp}"))
    } yield ()
  }

  def interpreter[F[_] : Effect](connectToDb: Boolean): WeatherApp ~> F =
    (if (connectToDb) new DbConnectedInterpreter(transactor[F]) else new DummyInterpreter) or new ConsoleUserIOInterpreter

  def main(args: Array[String]): Unit = {
    import UserIO._
    import Weather._
    //run disconnected from database
    program.foldMap(interpreter[Task](connectToDb = false)).runSyncMaybe
    //run connected to database
    program.foldMap(interpreter[Task](connectToDb = true)).runSyncMaybe
  }

}
