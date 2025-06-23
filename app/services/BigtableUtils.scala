package services

import com.google.cloud.bigtable.data.v2.models.Filters
import com.google.cloud.bigtable.data.v2.models.Filters.Filter
import com.google.protobuf.ByteString

/**
 * Utility methods for working with Google Cloud Bigtable.
 */
object BigtableUtils {
  
  /**
   * Create a family filter.
   *
   * @param family The column family name
   * @return A filter that matches cells from the specified family
   */
  def familyFilter(family: String): Filter = {
    Filters.FILTERS.family().exactMatch(family)
  }
  
  /**
   * Create a qualifier filter.
   *
   * @param qualifier The column qualifier
   * @return A filter that matches cells with the specified qualifier
   */
  def qualifierFilter(qualifier: String): Filter = {
    Filters.FILTERS.qualifier().exactMatch(qualifier)
  }
  
  /**
   * Create a value filter.
   *
   * @param value The cell value
   * @return A filter that matches cells with the specified value
   */
  def valueFilter(value: String): Filter = {
    Filters.FILTERS.value().exactMatch(value)
  }
  
  /**
   * Create a value prefix filter.
   *
   * @param prefix The prefix to match
   * @return A filter that matches cells with values that start with the specified prefix
   */
  def valuePrefixFilter(prefix: String): Filter = {
    Filters.FILTERS.value().prefix(prefix)
  }
  
  /**
   * Create a row key prefix filter.
   *
   * @param prefix The row key prefix
   * @return A filter that matches rows with keys that start with the specified prefix
   */
  def rowKeyPrefixFilter(prefix: String): Filter = {
    Filters.FILTERS.key().prefix(prefix)
  }
  
  /**
   * Create a row key range filter.
   *
   * @param startKey The start key (inclusive)
   * @param endKey The end key (exclusive)
   * @return A filter that matches rows with keys in the specified range
   */
  def rowKeyRangeFilter(startKey: String, endKey: String): Filter = {
    Filters.FILTERS.key().range(startKey, endKey)
  }
  
  /**
   * Create a timestamp range filter.
   *
   * @param startTimestampMicros The start timestamp in microseconds (inclusive)
   * @param endTimestampMicros The end timestamp in microseconds (exclusive)
   * @return A filter that matches cells with timestamps in the specified range
   */
  def timestampRangeFilter(startTimestampMicros: Long, endTimestampMicros: Long): Filter = {
    Filters.FILTERS.timestamp().range(startTimestampMicros, endTimestampMicros)
  }
  
  /**
   * Create a limit filter.
   *
   * @param limit The maximum number of cells to return
   * @return A filter that limits the number of cells returned
   */
  def limitFilter(limit: Int): Filter = {
    Filters.FILTERS.limit().cellsPerRow(limit)
  }
  
  /**
   * Create a chain filter (AND).
   *
   * @param filters The filters to chain
   * @return A filter that matches cells that match all of the specified filters
   */
  def chainFilter(filters: Filter*): Filter = {
    Filters.FILTERS.chain().filter(filters: _*)
  }
  
  /**
   * Create an interleave filter (OR).
   *
   * @param filters The filters to interleave
   * @return A filter that matches cells that match any of the specified filters
   */
  def interleaveFilter(filters: Filter*): Filter = {
    Filters.FILTERS.interleave().filter(filters: _*)
  }
  
  /**
   * Create a column filter (family and qualifier).
   *
   * @param family The column family
   * @param qualifier The column qualifier
   * @return A filter that matches cells from the specified column
   */
  def columnFilter(family: String, qualifier: String): Filter = {
    chainFilter(
      familyFilter(family),
      qualifierFilter(qualifier)
    )
  }
  
  /**
   * Create a latest version filter.
   *
   * @param numVersions The number of versions to return
   * @return A filter that returns only the most recent versions of cells
   */
  def latestVersionFilter(numVersions: Int = 1): Filter = {
    Filters.FILTERS.limit().cellsPerColumn(numVersions)
  }
  
  /**
   * Convert a string to a ByteString.
   *
   * @param s The string to convert
   * @return The ByteString representation of the string
   */
  def toByteString(s: String): ByteString = {
    ByteString.copyFromUtf8(s)
  }
  
  /**
   * Convert a ByteString to a string.
   *
   * @param bs The ByteString to convert
   * @return The string representation of the ByteString
   */
  def fromByteString(bs: ByteString): String = {
    bs.toStringUtf8()
  }
}
