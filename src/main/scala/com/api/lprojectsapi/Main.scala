package com.api.lprojectsapi

import cats.effect.IO
import com.twitter.finagle.http.filter.Cors
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Await
import io.finch._
import io.finch.catsEffect._
import io.finch.circe._
import io.circe.generic.auto._
import scalikejdbc._
import java.time._


object Main extends App {

case class Project2(id: Long, name: Option[String], createdAt: ZonedDateTime)
object Project2 extends SQLSyntaxSupport[Project2] {
  override val tableName = "projects"
  def apply(rs: WrappedResultSet) = new Project2(
    rs.long("id"), rs.stringOpt("name"), rs.zonedDateTime("created_at"))
}

import scala.collection.Seq
  Class.forName("org.h2.Driver")
  ConnectionPool.singleton("jdbc:h2:mem:hello", "user", "pass")

  implicit val session = AutoSession
  case class Project(name: String, url: String, description: String, features: Seq[Feature],
    skills: Seq[Skill], categories: Seq[Category])

  case class Feature(name: String)
  case class Skill(name: String)
  case class Category(name: String)

  def healthcheck: Endpoint[IO, String] = get(pathEmpty) {
    Ok("OK")
  }

  def projects2: Endpoint[IO, Seq[Project]] = get("projects2") {
    Ok(Seq(
      Project(
        "l-project", 
        "https://github.com/kapit4n/l-projects", 
        "Register projects", 
        Seq(Feature("Finch")),
        Seq(Skill("JavaScript"), Skill("React js"), Skill("Github")),
        Seq(Category(""))
      )
    )) 
  }
  

  def buildTables: Endpoint[IO, Int] = get("buildTables") {
    sql"""
    create table projects (
      id serial not null primary key,
      name varchar(64),
      created_at timestamp not null
    )
    """.execute.apply()
    Ok(1)
  }

  
  def initialValues: Endpoint[IO, Int] = get("initialValues") {
    Seq("Project 1", "Project 2", "Project 3") foreach { name =>
      sql"insert into projects (name, created_at) values (${name}, current_timestamp)".update.apply()
    }
    Ok(1)
  }



  def projects: Endpoint[IO, List[Project2]] = get("projects") {

    val entities: List[Map[String, Any]] = sql"select * from projects".map(_.toMap).list.apply()

    val projects: List[Project2] = sql"select * from projects".map(rs => Project2(rs)).list.apply()

    Ok(projects)
  }

   
  def getProject: Endpoint[IO, Project] = get("projects" :: path[String]) { Id: String =>
    Ok(Project(
        "l-project", 
        "https://github.com/kapit4n/l-projects", 
        "Register projects", 
        Seq(Feature("Finch")),
        Seq(Skill("JavaScript"), Skill("React js"), Skill("Github")),
        Seq(Category("Full Stack"))
      ))
  }

  def service: Service[Request, Response] = Bootstrap
    .serve[Text.Plain](healthcheck)
    .serve[Application.Json](getProject :+: projects :+: buildTables)
    .toService


  val policy: Cors.Policy = Cors.Policy(
    allowsOrigin = _ => Some(""),
    allowsMethods = _ => Some(Seq("GET", "POST")),
    allowsHeaders = _ => Some(Seq("Accept"))
  )

  val corsService: Service[Request, Response] = new Cors.HttpFilter(policy).andThen(service)

  Await.ready(Http.server.serve(":8081", service))
}
