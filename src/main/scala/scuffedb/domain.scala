package scuffedb
package domain

enum Mark:
  case Active, Tombstone

case class Entry(key: String, value: String, mark: Mark)
object Entry:
  def makeActive(key: String, value: String): Entry =
    Entry(key, value, Mark.Active)

  def makeTombstone(key: String): Entry =
    Entry(key, "", Mark.Tombstone)
