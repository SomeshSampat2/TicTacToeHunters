package com.innovatelabz.tictactoehunters.data

data class GameSession(
    val sessionId: String = "",
    val creatorId: String = "",
    val opponentId: String? = null,
    val status: GameStatus = GameStatus.WAITING,
    val board: List<String> = List(9) { "" },
    val currentTurn: String = "",
    val winner: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val xPlayer: String? = null,  // Player who plays as X
    val oPlayer: String? = null,  // Player who plays as O
    val rematchRequestedBy: String? = null, // Player who requested rematch
    val rematchAccepted: Boolean = false
) {
    companion object {
        fun generateSessionId(): String {
            return (1000..9999).random().toString()
        }

        fun createNewGame(sessionId: String, creatorId: String): GameSession {
            val isCreatorX = Math.random() < 0.5
            return GameSession(
                sessionId = sessionId,
                creatorId = creatorId,
                currentTurn = if (isCreatorX) creatorId else "",  // Empty means waiting for opponent
                xPlayer = if (isCreatorX) creatorId else null,
                oPlayer = if (isCreatorX) null else creatorId
            )
        }
    }

    fun isBoardFull(): Boolean = board.none { it.isEmpty() }

    fun hasWinner(): Boolean {
        val winPatterns = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),  // Rows
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),  // Columns
            listOf(0, 4, 8), listOf(2, 4, 6)                     // Diagonals
        )

        return winPatterns.any { pattern ->
            val first = board[pattern[0]]
            if (first.isNotEmpty()) {
                first == board[pattern[1]] && first == board[pattern[2]]
            } else false
        }
    }

    fun isGameOver(): Boolean = hasWinner() || isBoardFull()

    fun canPlayerMove(playerId: String): Boolean {
        return status == GameStatus.IN_PROGRESS && 
               currentTurn == playerId && 
               !isGameOver()
    }

    fun getPlayerSymbol(playerId: String): String {
        return when (playerId) {
            xPlayer -> "X"
            oPlayer -> "O"
            else -> ""
        }
    }
}

enum class GameStatus {
    WAITING,
    IN_PROGRESS,
    COMPLETED,
    REMATCH_PENDING
}
