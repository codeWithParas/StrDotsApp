package com.xyz.strapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xyz.strapp.domain.model.entity.FaceImageEntity

@Dao
interface FaceImageDao {

    /**
     * Inserts a face image into the database. If a conflict occurs (e.g., same ID),
     * it replaces the old entry.
     *
     * @param faceImage The FaceImageEntity to insert.
     * @return The row ID of the newly inserted image.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaceImage(faceImage: FaceImageEntity): Long

    /**
     * Retrieves all face images that have not yet been uploaded,
     * ordered by their timestamp (oldest first).
     *
     * @return A list of FaceImageEntity objects that are pending upload.
     */
    @Query("SELECT * FROM face_images WHERE is_uploaded = 0 ORDER BY timestamp ASC")
    suspend fun getPendingUploads(): List<FaceImageEntity>

    /**
     * Marks a specific face image as uploaded.
     *
     * @param imageId The ID of the image to mark as uploaded.
     * @return The number of rows updated (should be 1 if the image existed).
     */
    @Query("UPDATE face_images SET is_uploaded = 1 WHERE id = :imageId")
    suspend fun markAsUploaded(imageId: Long): Int

    /**
     * Deletes a specific face image from the database by its ID.
     *
     * @param imageId The ID of the image to delete.
     * @return The number of rows deleted (should be 1 if the image existed).
     */
    @Query("DELETE FROM face_images WHERE id = :imageId")
    suspend fun deleteImageById(imageId: Long): Int

    /**
     * Retrieves a specific face image by its ID.
     *
     * @param imageId The ID of the image to retrieve.
     * @return The FaceImageEntity if found, otherwise null.
     */
    @Query("SELECT * FROM face_images WHERE id = :imageId")
    suspend fun getImageById(imageId: Long): FaceImageEntity?

    /**
     * Deletes all images from the face_images table.
     * Useful for debugging or if you need a way to clear all cached images.
     */
    @Query("DELETE FROM face_images")
    suspend fun deleteAllImages()

}