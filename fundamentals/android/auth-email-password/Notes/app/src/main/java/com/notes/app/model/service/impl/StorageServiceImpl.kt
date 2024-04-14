package com.notes.app.model.service.impl

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.InsertOneOptions
import com.mongodb.client.model.ReplaceOptions
import com.notes.app.model.Note
import com.notes.app.model.service.AccountService
import com.notes.app.model.service.StorageService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.tasks.await
import org.bson.Document
import javax.inject.Inject

class StorageServiceImpl @Inject constructor(
    private val auth: AccountService,
    private val mongoClient: MongoClient
) : StorageService {

    private val notesCollection: MongoCollection<Document> by lazy {
        mongoClient.getDatabase("yourDatabaseName").getCollection("notes")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val notes: Flow<List<Note>>
        get() =
            auth.currentUser.flatMapLatest { note ->
                notesCollection.find(Filters.eq("userId", note?.id))
                    .map { document -> document.toNote() }
            }

    override suspend fun createNote(note: Note) {
        val noteDocument = note.toDocument()
        notesCollection.insertOne(noteDocument).await()
    }

    override suspend fun readNote(noteId: String): Note? {
        val document = notesCollection.find(Filters.eq("_id", noteId)).firstOrNull()
        return document?.toNote()
    }

    override suspend fun updateNote(note: Note) {
        val noteDocument = note.toDocument()
        notesCollection.replaceOne(Filters.eq("_id", note.id), noteDocument, ReplaceOptions().upsert(true)).await()
    }

    override suspend fun deleteNote(noteId: String) {
        notesCollection.deleteOne(Filters.eq("_id", noteId)).await()
    }

    companion object {
        private const val USER_ID_FIELD = "userId"
    }
}

private fun Document.toNote(): Note {
    return Note(
        id = this["_id"] as String,
        title = this["title"] as String,
        content = this["content"] as String,
        userId = this["userId"] as String
    )
}

private fun Note.toDocument(): Document {
    val document = Document()
    document["_id"] = this.id
    document["title"] = this.title
    document["content"] = this.content
    document["userId"] = this.userId
    return document
}

