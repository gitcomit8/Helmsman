package helmsman.client.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TokenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToken(token: TokenEntity)

    @Query("SELECT * FROM token WHERE id = 0 LIMIT 1")
    suspend fun getToken(): TokenEntity?

    @Query("DELETE FROM token")
    suspend fun clearToken()
}
