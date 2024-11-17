package com.innovatelabz.tictactoehunters.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.innovatelabz.tictactoehunters.data.GameSession
import com.innovatelabz.tictactoehunters.data.GameStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GameRepository {
    private val db = FirebaseFirestore.getInstance()
    private val gamesCollection = db.collection("games")

    suspend fun createGameSession(): GameSession {
        val sessionId = GameSession.generateSessionId()
        val gameSession = GameSession.createNewGame(sessionId, sessionId)
        gamesCollection.document(sessionId).set(gameSession).await()
        return gameSession
    }

    suspend fun joinGame(sessionId: String, playerId: String): GameSession? {
        val gameRef = gamesCollection.document(sessionId)
        return db.runTransaction { transaction ->
            val snapshot = transaction.get(gameRef)
            val currentGame = snapshot.toObject(GameSession::class.java)

            if (currentGame != null && currentGame.status == GameStatus.WAITING) {
                val updatedGame = currentGame.copy(
                    opponentId = playerId,
                    status = GameStatus.IN_PROGRESS,
                    currentTurn = if (currentGame.currentTurn.isEmpty()) playerId else currentGame.currentTurn,
                    xPlayer = currentGame.xPlayer ?: playerId,
                    oPlayer = currentGame.oPlayer ?: playerId
                )
                transaction.set(gameRef, updatedGame)
                updatedGame
            } else {
                null
            }
        }.await()
    }

    suspend fun makeMove(sessionId: String, playerId: String, position: Int): Boolean {
        val gameRef = gamesCollection.document(sessionId)
        return db.runTransaction { transaction ->
            val snapshot = transaction.get(gameRef)
            val currentGame = snapshot.toObject(GameSession::class.java)

            if (currentGame != null && currentGame.canPlayerMove(playerId)) {
                val newBoard = currentGame.board.toMutableList()
                if (newBoard[position].isEmpty()) {
                    newBoard[position] = currentGame.getPlayerSymbol(playerId)
                    
                    val updatedGame = currentGame.copy(
                        board = newBoard,
                        currentTurn = if (playerId == currentGame.creatorId) 
                            currentGame.opponentId!! else currentGame.creatorId,
                        status = if (currentGame.hasWinner() || currentGame.isBoardFull()) 
                            GameStatus.COMPLETED else GameStatus.IN_PROGRESS,
                        winner = if (currentGame.hasWinner()) playerId else null
                    )
                    
                    transaction.set(gameRef, updatedGame)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }.await()
    }

    suspend fun requestRematch(sessionId: String, playerId: String): Boolean {
        val gameRef = gamesCollection.document(sessionId)
        return db.runTransaction { transaction ->
            val snapshot = transaction.get(gameRef)
            val currentGame = snapshot.toObject(GameSession::class.java)

            if (currentGame != null && currentGame.status == GameStatus.COMPLETED) {
                val updatedGame = currentGame.copy(
                    rematchRequestedBy = playerId,
                    status = GameStatus.REMATCH_PENDING
                )
                transaction.set(gameRef, updatedGame)
                true
            } else {
                false
            }
        }.await()
    }

    suspend fun acceptRematch(sessionId: String, playerId: String): GameSession? {
        val gameRef = gamesCollection.document(sessionId)
        return db.runTransaction { transaction ->
            val snapshot = transaction.get(gameRef)
            val currentGame = snapshot.toObject(GameSession::class.java)

            if (currentGame != null && 
                currentGame.status == GameStatus.REMATCH_PENDING && 
                currentGame.rematchRequestedBy != playerId
            ) {
                // Swap X and O players for fairness
                val newGame = GameSession.createNewGame(sessionId, currentGame.creatorId)
                val updatedGame = newGame.copy(
                    opponentId = currentGame.opponentId,
                    status = GameStatus.IN_PROGRESS
                )
                transaction.set(gameRef, updatedGame)
                updatedGame
            } else {
                null
            }
        }.await()
    }

    fun observeGameSession(sessionId: String): Flow<GameSession?> = callbackFlow {
        var listener: ListenerRegistration? = null
        
        try {
            listener = gamesCollection.document(sessionId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    
                    val gameSession = snapshot?.toObject(GameSession::class.java)
                    trySend(gameSession)
                }
                
            awaitClose {
                listener?.remove()
            }
        } catch (e: Exception) {
            close(e)
        }
    }

    suspend fun leaveGame(sessionId: String, playerId: String) {
        val gameRef = gamesCollection.document(sessionId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(gameRef)
            val currentGame = snapshot.toObject(GameSession::class.java)

            if (currentGame != null) {
                val updatedGame = currentGame.copy(
                    status = GameStatus.COMPLETED,
                    winner = if (playerId == currentGame.creatorId) 
                        currentGame.opponentId else currentGame.creatorId
                )
                transaction.set(gameRef, updatedGame)
            }
        }.await()
    }
}
