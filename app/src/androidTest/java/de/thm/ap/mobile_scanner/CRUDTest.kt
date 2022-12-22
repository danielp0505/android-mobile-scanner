package de.thm.ap.mobile_scanner

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.thm.ap.mobile_scanner.data.AppDatabase
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.model.DocumentImageRelation
import de.thm.ap.mobile_scanner.model.Image
import de.thm.ap.mobile_scanner.model.Tag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class CRUDTest {

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun persistDocument() = runTest {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val dao = AppDatabase.getDb(appContext).documentDao()

    val id = dao.persist(Document(title = "Simple Document"))
    assert(id != null)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun persistTag() = runTest {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val dao = AppDatabase.getDb(appContext).documentDao()

    val id = dao.persist(Tag(name = "toller Tag"))
    assert(id != null)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun persistDocumentTagRelation() = runTest {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val dao = AppDatabase.getDb(appContext).documentDao()

    val docId = dao.persist(Document(title = "Simple Document"))
    val tagId = dao.persist(Tag(name = "toller Tag"))
    assert(docId != null)
    assert(tagId != null)
    dao.persist(docId, tagId)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun findDocumentById() = runTest {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val dao = AppDatabase.getDb(appContext).documentDao()

    dao.persist(Document(title = "Test1"))
    val id = dao.persist(Document(title = "Test2"))
    dao.persist(Document(title = "Test3"))
    dao.persist(Document(title = "Test4"))

    assertEquals(dao.findDocumentById(id).title, "Test2")
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun updateTest() = runTest {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val dao = AppDatabase.getDb(appContext).documentDao()

    val id = dao.persist(Document(title = "Simple Document"))
    dao.update(Document(id, title = "Neuer Titel"))

    assertEquals(dao.findDocumentById(id).title, "Neuer Titel")
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun deleteTest() = runTest {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val dao = AppDatabase.getDb(appContext).documentDao()
    val orig_size = dao.findAllDocuments().size

    val id = dao.persist(Document(title = "Simple Document"))
    assertEquals(orig_size + 1, dao.findAllDocuments().size)
    dao.delete(Document(Long.MAX_VALUE))
    dao.delete(Document())
    assertEquals(orig_size + 1, dao.findAllDocuments().size)
    dao.delete(Document(id))
    assertEquals(orig_size, dao.findAllDocuments().size)

  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun getDocumentsWithImages() = runTest {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val dao = AppDatabase.getDb(appContext).documentDao()

    val docuemntId = dao.persist(Document(title = "Simple Document"))
    val imageId = dao.persist(Image(uri = "content://test"))
    dao.persist(DocumentImageRelation(docuemntId, imageId))
    assert(dao.getDocumentsWithImages().any {
      it.document.documentId == docuemntId &&
              it.images.size == 1 &&
              it.images[0].imageId == imageId
    })

  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun getDocumentWithImages() = runTest {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val dao = AppDatabase.getDb(appContext).documentDao()

    val docuemntId = dao.persist(Document(title = "Simple Document"))
    val imageId = dao.persist(Image(uri = "content://test"))
    dao.persist(DocumentImageRelation(docuemntId, imageId))
    val documentWithImages = dao.getDocumentWithImages(docuemntId)
    assertEquals(docuemntId, documentWithImages.document.documentId)
    assertEquals(1, documentWithImages.images.size)
    assertEquals(imageId, documentWithImages.images[0].imageId)

  }


}