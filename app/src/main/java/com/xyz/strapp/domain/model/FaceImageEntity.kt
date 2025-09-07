package com.xyz.strapp.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "face_images") // Defines the table name
data class FaceImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Auto-generated primary key

    @ColumnInfo(
        name = "image_data",
        typeAffinity = ColumnInfo.BLOB
    ) // Ensure BLOB type for byte array
    val imageData: ByteArray,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "is_uploaded")
    var isUploaded: Boolean = false // Default to false, as image is initially not uploaded
) {
    // Auto-generated equals() and hashCode() by data class are usually sufficient
    // for Room's purposes, especially when relying on the primary key.
    // However, if you ever need to compare instances where ByteArray content matters
    // and they might not be the same instance, a custom equals/hashCode is needed.
    // For most Room use cases with a primary key, this is not strictly necessary.

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceImageEntity

        if (id != other.id) return false
        if (!imageData.contentEquals(other.imageData)) return false
        if (timestamp != other.timestamp) return false
        if (isUploaded != other.isUploaded) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + imageData.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + isUploaded.hashCode()
        return result
    }
}


