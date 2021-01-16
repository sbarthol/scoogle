package utils

import actors.LevelDBActor
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JsonProtocol extends DefaultJsonProtocol {
  implicit val itemFormat: RootJsonFormat[LevelDBActor.Item] = jsonFormat4(
    LevelDBActor.Item
  )
  implicit val responseFormat: RootJsonFormat[LevelDBActor.Response] = jsonFormat2(
    LevelDBActor.Response
  )
}
