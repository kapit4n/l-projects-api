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

import scala.collection.Seq

object Main extends App {

  case class Project(name: String, url: String, description: String, features: Seq[Feature],
    skills: Seq[Skill], categories: Seq[Category])
  case class Feature(name: String)
  case class Skill(name: String)
  case class Category(name: String)

  def healthcheck: Endpoint[IO, String] = get(pathEmpty) {
    Ok("OK")
  }

  def projects: Endpoint[IO, Seq[Project]] = get("projects") {
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
    .serve[Application.Json](getProject :+: projects)
    .toService


  val policy: Cors.Policy = Cors.Policy(
    allowsOrigin = _ => Some(""),
    allowsMethods = _ => Some(Seq("GET", "POST")),
    allowsHeaders = _ => Some(Seq("Accept"))
  )

  val corsService: Service[Request, Response] = new Cors.HttpFilter(policy).andThen(service)

  Await.ready(Http.server.serve(":8081", service))
}
