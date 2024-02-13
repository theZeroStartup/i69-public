package com.i69.ui.screens.main.moment.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.i69.GetAllUserMomentsQuery
import com.i69.data.models.Moment
import com.i69.data.models.User
import com.i69.data.remote.responses.DefaultPicker

@Dao
interface MomentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMomentsList(moments: List<Moment>)

    @Query("SELECT * FROM moment_table")
    fun getMomentsList(): List<Moment>

    @Query("DELETE FROM moment_table")
    fun deleteAllMoments()
}