package de.thm.ap.mobile_scanner.data

import androidx.lifecycle.LiveData
import androidx.room.*
import de.thm.ap.mobile_scanner.model.*

@Dao
interface DocumentDAO {

  @Query("SELECT * from document")
  fun findAllDocuments(): List<Document>

  @Query("SELECT * from document")
  fun findAllDocumentsSync(): LiveData<List<Document>>

  @Transaction
  @Query("SELECT * from document")
  fun findAllDocumentsWithTagsSync(): LiveData<List<DocumentWithTags>>

  @Query("SELECT * from document WHERE documentId = :documentId")
  fun findDocumentById(documentId: Long): Document

  @Update(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(document: Document): Int

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun persist(document: Document): Long

  @Delete
  suspend fun delete(document: Document): Int

  @Delete
  suspend fun deleteDocumentList(documents: List<Document>)

  @Query("SELECT * from tag")
  fun findAllTags(): List<Tag>

  @Query("SELECT * from tag")
  fun findAllTagsSync(): LiveData<List<Tag>>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun persist(tag: Tag): Long

  @Update(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(tag: Tag): Int

  @Delete
  suspend fun delete(tag: Tag): Int

  @Delete
  suspend fun deleteTagList(tags: List<Tag>)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun persist(documentTagRelation: DocumentTagRelation)

  @Query("SELECT * from documentTagRelation")
  fun findAllDocumentTagRelation(): List<DocumentTagRelation>

  @Delete
  suspend fun delete(documentTagRelation: DocumentTagRelation)

  suspend fun persist(documentID: Long, tagID: Long) =
    persist(DocumentTagRelation(documentID, tagID))

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun persist(image: Image): Long

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun persist(documentImageRelation: DocumentImageRelation)


  data class TagWithDocuments(
    @Embedded
    val tag: Tag,
    @Relation(
      parentColumn = "tagId",
      entityColumn = "documentId",
      associateBy = Junction(DocumentTagRelation::class)
    )
    val documents: List<Document>
  )

  data class DocumentWithTags(
    @Embedded
    val document: Document,
    @Relation(
      parentColumn = "documentId",
      entityColumn = "tagId",
      associateBy = Junction(DocumentTagRelation::class)
    )
    val tags: List<Tag>
  )

  @Transaction
  @Query("SELECT * FROM tag")
  suspend fun getTagsWithDocument(): List<TagWithDocuments>



  data class DocumentWithImages(
    @Embedded
    val document: Document,
    @Relation(
      parentColumn = "documentId",
      entityColumn = "imageId",
      associateBy = Junction(DocumentImageRelation::class)
    )
    val images: List<Image>

  )

  @Transaction
  @Query("SELECT * FROM document")
  suspend fun getDocumentsWithImages(): List<DocumentWithImages>

  @Transaction
  @Query("SELECT * FROM document where documentId = :documentId")
  suspend fun getDocumentWithImages(documentId: Long): DocumentWithImages

  @Transaction
  @Query("SELECT * FROM document where documentId = :documentId")
  fun getDocumentWithImagesSync(documentId: Long): LiveData<DocumentWithImages>
}

