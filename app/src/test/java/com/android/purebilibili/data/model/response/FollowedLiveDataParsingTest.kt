package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class FollowedLiveDataParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `followed live data accepts live_count alias`() {
        val response = json.decodeFromString(
            FollowedLiveResponse.serializer(),
            """
            {
              "code": 0,
              "message": "",
              "data": {
                "list": [],
                "live_count": 7,
                "count": 12
              }
            }
            """.trimIndent()
        )

        assertEquals(7, response.data?.livingNum)
        assertEquals(12, response.data?.notLivingNum)
    }

    @Test
    fun `followed live room maps watched count without extra room info request`() {
        val response = json.decodeFromString(
            FollowedLiveResponse.serializer(),
            """
            {
              "code": 0,
              "message": "",
              "data": {
                "list": [
                  {
                    "roomid": 6,
                    "uid": 10,
                    "title": "直播中",
                    "uname": "主播",
                    "face": "https://example.com/face.jpg",
                    "room_cover": "https://example.com/room.jpg",
                    "online": 0,
                    "popularity": 0,
                    "watched_show": {
                      "num": 12345,
                      "text_small": "1.2万"
                    },
                    "area_name": "单机游戏",
                    "live_status": 1
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val room = response.data?.list?.first()?.toLiveRoom()

        assertEquals(12345, room?.online)
        assertEquals("https://example.com/room.jpg", room?.cover)
    }
}
