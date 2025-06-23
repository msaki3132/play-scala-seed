package services

import com.google.api.gax.rpc.NotFoundException
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.bigtable.admin.v2.{BigtableTableAdminClient, BigtableTableAdminSettings}
import com.google.cloud.bigtable.data.v2.{BigtableDataClient, BigtableDataSettings}
import com.google.cloud.bigtable.data.v2.models._
import com.typesafe.config.ConfigFactory
import play.api.{Configuration, Logger}

import java.io.FileInputStream
import java.util.concurrent.{CompletableFuture, Executors}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

/**
 * Service for interacting with Google Cloud Bigtable.
 *
 * This service provides methods for common Bigtable operations such as:
 * - Reading rows
 * - Writing rows
 * - Deleting rows
 * - Scanning rows with filters
 * - Creating and managing tables
 */
@Singleton
class BigtableService @Inject()(configuration: Configuration) {
  private val logger = Logger(this.getClass)
  
  // Load Bigtable configuration
  private val config = configuration.get[Configuration]("bigtable")
  private val projectId = config.get[String]("projectId")
  private val instanceId = config.get[String]("instanceId")
  private val credentialsPathOpt = config.getOptional[String]("credentialsPath")
  
  // Connection pool settings
  private val connectionConfig = config.get[Configuration]("connection")
  private val channelsPerCpu = connectionConfig.getOptional[Int]("channelsPerCpu").getOrElse(4)
  private val maxRequestsPerChannel = connectionConfig.getOptional[Int]("maxRequestsPerChannel").getOrElse(100)
  private val timeoutMs = connectionConfig.getOptional[Int]("timeoutMs").getOrElse(60000)
  
  // Retry settings
  private val retryConfig = config.get[Configuration]("retry")
  private val maxRetries = retryConfig.getOptional[Int]("maxRetries").getOrElse(10)
  private val initialRetryDelayMs = retryConfig.getOptional[Int]("initialRetryDelayMs").getOrElse(250)
  private val maxRetryDelayMs = retryConfig.getOptional[Int]("maxRetryDelayMs").getOrElse(60000)
  private val retryDelayMultiplier = retryConfig.getOptional[Double]("retryDelayMultiplier").getOrElse(2.0)
  
  // Initialize Bigtable clients
  private val (dataClient, adminClient) = initializeClients()
  
  // Thread pool for async operations
  private val executor = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors() * 2)
  private implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(executor)
  
  /**
   * Initialize Bigtable data and admin clients.
   *
   * @return A tuple of (BigtableDataClient, BigtableTableAdminClient)
   */
  private def initializeClients(): (BigtableDataClient, BigtableTableAdminClient) = {
    try {
      // Build data client settings
      val dataSettingsBuilder = BigtableDataSettings.newBuilder()
        .setProjectId(projectId)
        .setInstanceId(instanceId)
      
      // Build admin client settings
      val adminSettingsBuilder = BigtableTableAdminSettings.newBuilder()
        .setProjectId(projectId)
        .setInstanceId(instanceId)
      
      // Apply credentials if specified
      credentialsPathOpt.foreach { credentialsPath =>
        val credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath))
        dataSettingsBuilder.setCredentialsProvider(() => credentials)
        adminSettingsBuilder.setCredentialsProvider(() => credentials)
      }
      
      // Create clients
      val dataClient = BigtableDataClient.create(dataSettingsBuilder.build())
      val adminClient = BigtableTableAdminClient.create(adminSettingsBuilder.build())
      
      logger.info(s"Successfully connected to Bigtable instance $instanceId in project $projectId")
      (dataClient, adminClient)
    } catch {
      case e: Exception =>
        logger.error("Failed to initialize Bigtable clients", e)
        throw e
    }
  }
  
  /**
   * Read a single row from a Bigtable table.
   *
   * @param tableId The table ID
   * @param rowKey The row key
   * @return Future containing the row or None if not found
   */
  def readRow(tableId: String, rowKey: String): Future[Option[Row]] = {
    val promise = Promise[Option[Row]]()
    
    CompletableFuture.supplyAsync(() => {
      try {
        Option(dataClient.readRow(tableId, rowKey))
      } catch {
        case _: NotFoundException => None
        case e: Exception =>
          logger.error(s"Error reading row $rowKey from table $tableId", e)
          throw e
      }
    }, executor).whenComplete { (result, error) =>
      if (error != null) {
        promise.failure(error)
      } else {
        promise.success(result)
      }
    }
    
    promise.future
  }
  
  /**
   * Read multiple rows from a Bigtable table.
   *
   * @param tableId The table ID
   * @param rowKeys The row keys to read
   * @return Future containing a map of row keys to rows
   */
  def readRows(tableId: String, rowKeys: Seq[String]): Future[Map[String, Row]] = {
    val promise = Promise[Map[String, Row]]()
    
    CompletableFuture.supplyAsync(() => {
      try {
        val query = Query.create(tableId).rowKeys(rowKeys.asJava)
        val rows = dataClient.readRows(query)
        
        val result = scala.collection.mutable.Map[String, Row]()
        rows.forEach(row => result.put(row.getKey.toStringUtf8, row))
        
        result.toMap
      } catch {
        case e: Exception =>
          logger.error(s"Error reading rows from table $tableId", e)
          throw e
      }
    }, executor).whenComplete { (result, error) =>
      if (error != null) {
        promise.failure(error)
      } else {
        promise.success(result)
      }
    }
    
    promise.future
  }
  
  /**
   * Write a mutation to a single row.
   *
   * @param tableId The table ID
   * @param rowKey The row key
   * @param mutations The mutations to apply
   * @return Future indicating success or failure
   */
  def writeRow(tableId: String, rowKey: String, mutations: Seq[Mutation]): Future[Unit] = {
    val promise = Promise[Unit]()
    
    CompletableFuture.runAsync(() => {
      try {
        val rowMutation = RowMutation.create(tableId, rowKey)
        mutations.foreach(rowMutation.add)
        dataClient.mutateRow(rowMutation)
      } catch {
        case e: Exception =>
          logger.error(s"Error writing to row $rowKey in table $tableId", e)
          throw e
      }
    }, executor).whenComplete { (_, error) =>
      if (error != null) {
        promise.failure(error)
      } else {
        promise.success(())
      }
    }
    
    promise.future
  }
  
  /**
   * Write a single value to a row.
   *
   * @param tableId The table ID
   * @param rowKey The row key
   * @param family The column family
   * @param qualifier The column qualifier
   * @param value The value to write
   * @return Future indicating success or failure
   */
  def writeValue(tableId: String, rowKey: String, family: String, qualifier: String, value: String): Future[Unit] = {
    val mutation = Mutation.create().setCell(family, qualifier, value)
    writeRow(tableId, rowKey, Seq(mutation))
  }
  
  /**
   * Delete a row from a Bigtable table.
   *
   * @param tableId The table ID
   * @param rowKey The row key
   * @return Future indicating success or failure
   */
  def deleteRow(tableId: String, rowKey: String): Future[Unit] = {
    val promise = Promise[Unit]()
    
    CompletableFuture.runAsync(() => {
      try {
        val mutation = RowMutation.create(tableId, rowKey).deleteRow()
        dataClient.mutateRow(mutation)
      } catch {
        case e: Exception =>
          logger.error(s"Error deleting row $rowKey from table $tableId", e)
          throw e
      }
    }, executor).whenComplete { (_, error) =>
      if (error != null) {
        promise.failure(error)
      } else {
        promise.success(())
      }
    }
    
    promise.future
  }
  
  /**
   * Scan rows in a Bigtable table with an optional filter.
   *
   * @param tableId The table ID
   * @param filter Optional filter to apply
   * @param limit Optional limit on the number of rows to return
   * @return Future containing the list of rows
   */
  def scanRows(tableId: String, filter: Option[Filters.Filter] = None, limit: Option[Int] = None): Future[Seq[Row]] = {
    val promise = Promise[Seq[Row]]()
    
    CompletableFuture.supplyAsync(() => {
      try {
        val queryBuilder = Query.create(tableId)
        filter.foreach(queryBuilder.filter)
        limit.foreach(queryBuilder.limit)
        
        val rows = dataClient.readRows(queryBuilder.build())
        val result = scala.collection.mutable.ArrayBuffer[Row]()
        rows.forEach(row => result.append(row))
        
        result.toSeq
      } catch {
        case e: Exception =>
          logger.error(s"Error scanning rows in table $tableId", e)
          throw e
      }
    }, executor).whenComplete { (result, error) =>
      if (error != null) {
        promise.failure(error)
      } else {
        promise.success(result)
      }
    }
    
    promise.future
  }
  
  /**
   * Check if a table exists.
   *
   * @param tableId The table ID
   * @return Future containing true if the table exists, false otherwise
   */
  def tableExists(tableId: String): Future[Boolean] = {
    val promise = Promise[Boolean]()
    
    CompletableFuture.supplyAsync(() => {
      try {
        adminClient.exists(tableId)
      } catch {
        case e: Exception =>
          logger.error(s"Error checking if table $tableId exists", e)
          throw e
      }
    }, executor).whenComplete { (result, error) =>
      if (error != null) {
        promise.failure(error)
      } else {
        promise.success(result)
      }
    }
    
    promise.future
  }
  
  /**
   * Create a new table.
   *
   * @param tableId The table ID
   * @param families The column families to create
   * @return Future indicating success or failure
   */
  def createTable(tableId: String, families: Map[String, Int]): Future[Unit] = {
    val promise = Promise[Unit]()
    
    CompletableFuture.runAsync(() => {
      try {
        if (adminClient.exists(tableId)) {
          logger.info(s"Table $tableId already exists")
        } else {
          val tableBuilder = adminClient.newCreateTableBuilder(tableId)
          families.foreach { case (family, maxVersions) =>
            tableBuilder.addFamily(family, maxVersions)
          }
          tableBuilder.build()
          logger.info(s"Created table $tableId")
        }
      } catch {
        case e: Exception =>
          logger.error(s"Error creating table $tableId", e)
          throw e
      }
    }, executor).whenComplete { (_, error) =>
      if (error != null) {
        promise.failure(error)
      } else {
        promise.success(())
      }
    }
    
    promise.future
  }
  
  /**
   * Delete a table.
   *
   * @param tableId The table ID
   * @return Future indicating success or failure
   */
  def deleteTable(tableId: String): Future[Unit] = {
    val promise = Promise[Unit]()
    
    CompletableFuture.runAsync(() => {
      try {
        if (adminClient.exists(tableId)) {
          adminClient.deleteTable(tableId)
          logger.info(s"Deleted table $tableId")
        } else {
          logger.info(s"Table $tableId does not exist")
        }
      } catch {
        case e: Exception =>
          logger.error(s"Error deleting table $tableId", e)
          throw e
      }
    }, executor).whenComplete { (_, error) =>
      if (error != null) {
        promise.failure(error)
      } else {
        promise.success(())
      }
    }
    
    promise.future
  }
  
  /**
   * List all tables in the Bigtable instance.
   *
   * @return Future containing the list of table IDs
   */
  def listTables(): Future[Seq[String]] = {
    val promise = Promise[Seq[String]]()
    
    CompletableFuture.supplyAsync(() => {
      try {
        adminClient.listTables().asScala.toSeq.map(_.getName)
      } catch {
        case e: Exception =>
          logger.error("Error listing tables", e)
          throw e
      }
    }, executor).whenComplete { (result, error) =>
      if (error != null) {
        promise.failure(error)
      } else {
        promise.success(result)
      }
    }
    
    promise.future
  }
  
  /**
   * Close the Bigtable clients and release resources.
   */
  def close(): Unit = {
    Try {
      if (dataClient != null) dataClient.close()
      if (adminClient != null) adminClient.close()
      executor.shutdown()
      logger.info("Bigtable clients closed")
    } match {
      case Success(_) => // Successfully closed
      case Failure(e) => logger.error("Error closing Bigtable clients", e)
    }
  }
}
