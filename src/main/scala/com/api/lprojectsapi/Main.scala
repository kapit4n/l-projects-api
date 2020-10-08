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
import scala.collection.Seq
import com.api.lprojectsapi.models._

object Main extends App {

  Class.forName("org.h2.Driver")
  ConnectionPool.singleton("jdbc:h2:mem:hello", "user", "pass")

  implicit val session = AutoSession

  def healthcheck: Endpoint[IO, String] = get(pathEmpty) {
    Ok("OK")
  }

  def buildTables: Endpoint[IO, Int] = get("buildTables") {
    // create projects table
    sql"""
    create table projects (
      id serial not null primary key,
      name varchar(255),
      url varchar(255),
      description varchar(255),
      created_at timestamp not null
    )
    """.execute.apply()
    
    // create skills table
    sql"""
    create table skills (
      id serial not null primary key,
      name varchar(255)
    )
    """.execute.apply()

    // create features table
    sql"""
    create table features (
      id serial not null primary key,
      name varchar(255)
    )
    """.execute.apply()

    // create category table
    sql"""
    create table categories (
      id serial not null primary key,
      name varchar(255)
    )
    """.execute.apply()


    Ok(1)
  }
  
  def initialValues: Endpoint[IO, Int] = get("initialValues") {
    Seq(Seq("Project 1", "url", "description")) foreach { pj =>
      sql"insert into projects (name, url, description, created_at) values (${pj(0)}, ${pj(1)}, ${pj(2)}, current_timestamp)".update.apply()
    }
    Ok(1)
  }

  def projects: Endpoint[IO, List[ProjectInfo]] = get("projects") {

    val entities: List[Map[String, Any]] = sql"select * from projects".map(_.toMap).list.apply()

    val projects: List[ProjectInfo] = sql"select * from projects".map(rs => ProjectInfo(rs)).list.apply()

    Ok(projects)
  }


  def postProject : Endpoint[IO, String] = post("projects" :: jsonBody[ProjectInfo]) { project: ProjectInfo => 
      Ok(s"Project info, ${project.name}")
  }

  def getProject: Endpoint[IO, ProjectInfo] = get("projects" :: path[Int]) { Id: Int =>
    val entities: List[Map[String, Any]] = sql"select * from projects where id = ${Id}".map(_.toMap).list.apply()

    val projects: List[ProjectInfo] = sql"select * from projects".map(rs => ProjectInfo(rs)).list.apply()

    Ok(projects(1))
  }

  def service: Service[Request, Response] = Bootstrap
    .serve[Text.Plain](healthcheck)
    .serve[Application.Json](getProject :+: postProject :+: projects :+: buildTables :+: initialValues)
    .toService


  val policy: Cors.Policy = Cors.Policy(
    allowsOrigin = _ => Some(""),
    allowsMethods = _ => Some(Seq("GET", "POST")),
    allowsHeaders = _ => Some(Seq("Accept"))
  )

  val corsService: Service[Request, Response] = new Cors.HttpFilter(policy).andThen(service)

  Await.ready(Http.server.serve(":8081", service))
}
