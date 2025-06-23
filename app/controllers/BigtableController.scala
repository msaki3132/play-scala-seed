package controllers

import javax.inject._
import play.api.libs.json._
import play.api.mvc._
import services.BigtableService

import scala.concurrent.{ExecutionContext, Future}
import com.google.cloud.bigtable.data.v2.models._
import scala.jdk.CollectionConverters._

/**
 * Controller for Bigtable operations.
 * 
 * This controller provides REST endpoints for interacting with Google Cloud Bigtable.
 */
@Singleton
class BigtableController @Inject()(
  val controllerComponents: ControllerComponents,
  bigtableService: BigtableService
)(implicit ec: ExecutionContext) extends BaseController {
  
  // JSON formatters
  implicit val cellFormat: Writes[Cell] = (cell: Cell) => Json.obj(
    "family" -> cell.getFamily,
    "qualifier" -> cell.getQualifier.toStringUtf8,
    "value" -> cell.getValue.toStringUtf8,
    "timestamp" -> cell.getTimestamp
  )
  
  implicit val rowFormat: Writes[Row] = (row: Row) => {
    val cells = row.getCells.asScala.map(cell => Json.toJson(cell)).toSeq
    Json.obj(
      "key" -> row.getKey.toStringUtf8,
      "cells" -> JsArray(cells)
    )
  }
  
  /**
   * List all tables in the Bigtable instance.
   */
  def listTables(): Action[AnyContent] = Action.async {
    bigtableService.listTables().map { tables =>
      Ok(Json.obj("tables" -> tables))
    }.recover {
      case e: Exception => 
        InternalServerError(Json.obj("error" -> e.getMessage))
    }
  }
  
  /**
   * Create a new table.
   */
  def createTable(): Action[JsValue] = Action.async(parse.json) { request =>
    val tableIdResult = (request.body \ "tableId").validate[String]
    val familiesResult = (request.body \ "families").validate[Map[String, Int]]
    
    (tableIdResult, familiesResult) match {
      case (JsSuccess(tableId, _), JsSuccess(families, _)) =>
        bigtableService.createTable(tableId, families).map { _ =>
          Created(Json.obj("message" -> s"Table $tableId created successfully"))
        }.recover {
          case e: Exception => 
            InternalServerError(Json.obj("error" -> e.getMessage))
        }
      case _ =>
        Future.successful(BadRequest(Json.obj(
          "error" -> "Invalid request body. Required: tableId (string) and families (map of family names to max versions)"
        )))
    }
  }
  
  /**
   * Delete a table.
   */
  def deleteTable(tableId: String): Action[AnyContent] = Action.async {
    bigtableService.deleteTable(tableId).map { _ =>
      Ok(Json.obj("message" -> s"Table $tableId deleted successfully"))
    }.recover {
      case e: Exception => 
        InternalServerError(Json.obj("error" -> e.getMessage))
    }
  }
  
  /**
   * Read a row from a table.
   */
  def readRow(tableId: String, rowKey: String): Action[AnyContent] = Action.async {
    bigtableService.readRow(tableId, rowKey).map {
      case Some(row) => Ok(Json.toJson(row))
      case None => NotFound(Json.obj("error" -> s"Row $rowKey not found in table $tableId"))
    }.recover {
      case e: Exception => 
        InternalServerError(Json.obj("error" -> e.getMessage))
    }
  }
  
  /**
   * Write a value to a row.
   */
  def writeValue(): Action[JsValue] = Action.async(parse.json) { request =>
    val tableIdResult = (request.body \ "tableId").validate[String]
    val rowKeyResult = (request.body \ "rowKey").validate[String]
    val familyResult = (request.body \ "family").validate[String]
    val qualifierResult = (request.body \ "qualifier").validate[String]
    val valueResult = (request.body \ "value").validate[String]
    
    if (tableIdResult.isSuccess && rowKeyResult.isSuccess && 
        familyResult.isSuccess && qualifierResult.isSuccess && valueResult.isSuccess) {
      
      val tableId = tableIdResult.get
      val rowKey = rowKeyResult.get
      val family = familyResult.get
      val qualifier = qualifierResult.get
      val value = valueResult.get
      
      bigtableService.writeValue(tableId, rowKey, family, qualifier, value).map { _ =>
        Ok(Json.obj("message" -> "Value written successfully"))
      }.recover {
        case e: Exception => 
          InternalServerError(Json.obj("error" -> e.getMessage))
      }
    } else {
      Future.successful(BadRequest(Json.obj(
        "error" -> "Invalid request body. Required: tableId, rowKey, family, qualifier, and value (all strings)"
      )))
    }
  }
  
  /**
   * Delete a row from a table.
   */
  def deleteRow(tableId: String, rowKey: String): Action[AnyContent] = Action.async {
    bigtableService.deleteRow(tableId, rowKey).map { _ =>
      Ok(Json.obj("message" -> s"Row $rowKey deleted successfully from table $tableId"))
    }.recover {
      case e: Exception => 
        InternalServerError(Json.obj("error" -> e.getMessage))
    }
  }
  
  /**
   * Scan rows in a table.
   */
  def scanRows(tableId: String, limit: Option[Int]): Action[AnyContent] = Action.async {
    bigtableService.scanRows(tableId, None, limit).map { rows =>
      Ok(Json.obj("rows" -> Json.toJson(rows)))
    }.recover {
      case e: Exception => 
        InternalServerError(Json.obj("error" -> e.getMessage))
    }
  }
}
