package com.api.lprojectsapi.models
import scalikejdbc._
import java.time._

case class ProjectInfo(id: Long, name: Option[String], url: Option[String], description: Option[String], createdAt: ZonedDateTime)

object ProjectInfo extends SQLSyntaxSupport[ProjectInfo] {
  override val tableName = "projects"
  def apply(rs: WrappedResultSet) = new ProjectInfo(
    rs.long("id"), rs.stringOpt("name"), rs.stringOpt("url"), rs.stringOpt("description"), rs.zonedDateTime("created_at"))
}
