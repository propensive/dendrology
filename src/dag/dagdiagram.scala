package dendrology

import rudiments.*
import anticipation.*
import gossamer.*
import acyclicity.*

import scala.reflect.*

import language.experimental.captureChecking

enum DagTile:
  case Space, Corner, Vertical, FirstMid, Horizontal, MidLast, Cross, Overlap

import DagTile.*

package dagStyles:
  given default[TextType: Textual]: TextualDagStyle[TextType] =
    TextualDagStyle("  ".tt, "└─".tt, "│ ".tt, "├─".tt, "──".tt,  "┴─".tt, "│─".tt, "┼─".tt)
  
  given ascii[TextType: Textual]: TextualDagStyle[TextType] =
    TextualDagStyle("  ".tt, "+-".tt, "| ".tt, "+-".tt, "--".tt, "+-".tt, "|-".tt, "+-".tt)

case class TextualDagStyle
    [LineType]
    (space: Text, corner: Text, vertical: Text, firstMid: Text, horizontal: Text, midLast: Text, cross: Text,
        overlap: Text)
    (using textual: Textual[LineType])
extends DagStyle[LineType]:
  def serialize(tiles: List[DagTile], node: LineType): LineType =
    textual.make(tiles.map(text(_)).join.s)+node

  def text(tile: DagTile) = tile match
    case Space      => space
    case Corner     => corner
    case Vertical   => vertical
    case FirstMid   => firstMid
    case Horizontal => horizontal
    case MidLast    => midLast
    case Cross      => cross
    case Overlap    => overlap
  
  def followOnText(tile: DagTile): Text = tile match
    case Space | Horizontal | Corner | MidLast | Overlap => space
    case _                                                  => vertical

trait DagStyle[LineType]:
  def serialize(tiles: List[DagTile], node: LineType): LineType


object DagDiagram:
  def apply[NodeType: ClassTag](dag: Dag[NodeType]): DagDiagram[NodeType] =
    val nodes = Array.from(dag.sorted)
    val indexes: Map[NodeType, Int] = nodes.zipWithIndex.to(Map)

    val layout: Array[Array[Int]] = Array.from:
      nodes.indices.map: i =>
        Array.range(0, i).map(_ => 0)
    
    dag.edges.map: (source, destination) =>
      val si = indexes(source)
      val di = indexes(destination)

      layout(si)(di) |= 1
      
      for i <- (di + 1) until si do
        layout(i)(di) |= 2
        layout(si)(i) |= 4

    DagDiagram:
      layout.to(List).map: row =>
        val tiles = row.to(List).map(DagTile.fromOrdinal)
        (tiles, nodes(row.length))

case class DagDiagram[NodeType](lines: List[(List[DagTile], NodeType)]):
  def render[LineType](line: NodeType => LineType)(using style: DagStyle[LineType]): List[LineType] =
    lines.map { (tiles, node) => style.serialize(tiles, line(node)) }
  
  def nodes: List[NodeType] = lines.map(_(1))
  def tiles: List[List[DagTile]] = lines.map(_(0))

@main
def run(): Unit =
  import dagStyles.default
  val dag = Dag(t"two" -> Set(), t"three" -> Set(), t"four" -> Set(t"two"), t"five" -> Set(), t"six" -> Set(t"two", t"three"), t"seven" -> Set(), t"eight" -> Set(t"two"), t"nine" -> Set(t"three"), t"ten" -> Set(t"two", t"five"), t"eleven" -> Set(), t"twelve" -> Set(t"two", t"three", t"four", t"six"), t"thirteen" -> Set(), t"fourteen" -> Set(t"seven", t"two"), t"fifteen" -> Set(t"three", t"five"))
  DagDiagram[Text](dag).render(desc => t"► $desc").foreach(println(_))
  