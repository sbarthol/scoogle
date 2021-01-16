package utils

import actors.DBActor
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JsonProtocol extends DefaultJsonProtocol {
  implicit val itemFormat: RootJsonFormat[DBActor.Item] = jsonFormat4(
    DBActor.Item
  )
  implicit val responseFormat: RootJsonFormat[DBActor.Response] = jsonFormat2(
    DBActor.Response
  )
}
