package helmsman.client.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "token")
data class TokenEntity(
    @PrimaryKey val id: Int = 0,
    val token: String
)
