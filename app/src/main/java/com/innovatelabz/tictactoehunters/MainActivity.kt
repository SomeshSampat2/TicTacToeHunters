package com.innovatelabz.tictactoehunters

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatelabz.tictactoehunters.ui.theme.TictacToeHuntersTheme
import android.app.Activity
import android.widget.Toast
import androidx.compose.ui.graphics.Brush
import kotlin.random.Random
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.innovatelabz.tictactoehunters.repository.GameRepository
import com.innovatelabz.tictactoehunters.data.GameSession
import com.innovatelabz.tictactoehunters.data.GameStatus

enum class GameMode {
    TWO_PLAYER,
    VS_COMPUTER_EASY,
    VS_COMPUTER_MEDIUM,
    VS_COMPUTER_HARD,
    ONLINE_CREATE,
    ONLINE_JOIN
}

enum class Screen {
    MAIN_MENU,
    GRID_SELECTION,
    GAME
}

data class GameState(
    val cells: List<String> = List(9) { "" },
    val currentPlayer: String = "X",
    val winner: String? = null,
    val gameOver: Boolean = false,
    val winningCells: List<Int>? = null,
    val gameMode: GameMode = GameMode.TWO_PLAYER
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TictacToeHuntersTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameApp()
                }
            }
        }
    }
}

@Composable
fun GameApp() {
    var currentScreen by remember { mutableStateOf(Screen.MAIN_MENU) }
    var gridSize by remember { mutableStateOf(3) }
    var gameMode by remember { mutableStateOf(GameMode.TWO_PLAYER) }
    
    val context = LocalContext.current

    Crossfade(targetState = currentScreen, label = "") { screen ->
        when (screen) {
            Screen.MAIN_MENU -> MenuScreen(
                onStartGame = { size, mode ->
                    gridSize = size
                    gameMode = mode
                    currentScreen = Screen.GAME
                },
                onExitGame = {
                    (context as? Activity)?.finish()
                }
            )
            Screen.GRID_SELECTION -> GridSelectionScreen(
                onStartGame = { size, mode ->
                    gridSize = size
                    gameMode = mode
                    currentScreen = Screen.GAME
                },
                onBackToMenu = {
                    currentScreen = Screen.MAIN_MENU
                }
            )
            Screen.GAME -> GameScreen(
                gridSize = gridSize,
                gameMode = gameMode,
                onBackToMenu = {
                    currentScreen = Screen.MAIN_MENU
                }
            )
        }
    }
}

@Composable
fun MenuScreen(onStartGame: (Int, GameMode) -> Unit, onExitGame: () -> Unit) {
    var currentScreen by remember { mutableStateOf(Screen.MAIN_MENU) }
    var showDifficultyDialog by remember { mutableStateOf(false) }
    var showOnlineDialog by remember { mutableStateOf(false) }
    var showWaitingDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var gameCode by remember { mutableStateOf("") }
    var gameSession by remember { mutableStateOf<GameSession?>(null) }
    val context = LocalContext.current
    val gameRepository = remember { GameRepository() }
    val scope = rememberCoroutineScope()

    // Observe game session changes
    LaunchedEffect(gameSession?.sessionId) {
        gameSession?.sessionId?.let { sessionId ->
            gameRepository.observeGameSession(sessionId)
                .collect { updatedSession ->
                    gameSession = updatedSession
                    if (updatedSession?.status == GameStatus.IN_PROGRESS) {
                        showWaitingDialog = false
                        // Start game only when status changes to IN_PROGRESS
                        if (updatedSession.oPlayer != null) {
                            // For O player (joiner)
                            onStartGame(3, GameMode.ONLINE_JOIN)
                        } else {
                            // For X player (creator)
                            onStartGame(3, GameMode.ONLINE_CREATE)
                        }
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Tic Tac Toe",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        when (currentScreen) {
            Screen.MAIN_MENU -> {
                // Player vs Player button
                Card(
                    modifier = Modifier
                        .width(280.dp)
                        .padding(vertical = 8.dp)
                        .clickable { currentScreen = Screen.GRID_SELECTION },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = "Play with Friend",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Player vs Computer button
                Card(
                    modifier = Modifier
                        .width(280.dp)
                        .padding(vertical = 8.dp)
                        .clickable { showDifficultyDialog = true },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(
                        text = "Play with Computer",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                // Online Play button
                Card(
                    modifier = Modifier
                        .width(280.dp)
                        .padding(vertical = 8.dp)
                        .clickable { showOnlineDialog = true },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Text(
                        text = "Play Online",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Screen.GRID_SELECTION -> {
                // Back button for grid selection
                Button(
                    onClick = { currentScreen = Screen.MAIN_MENU },
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Back",
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                // 3x3 Grid button
                Card(
                    modifier = Modifier
                        .width(280.dp)
                        .padding(vertical = 8.dp)
                        .clickable { onStartGame(3, GameMode.TWO_PLAYER) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = "Classic Mode (3×3)",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // 4x4 Grid button
                Card(
                    modifier = Modifier
                        .width(280.dp)
                        .padding(vertical = 8.dp)
                        .clickable { onStartGame(4, GameMode.TWO_PLAYER) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Text(
                        text = "Extended Mode (4×4)",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            else -> {}
        }

        if (showDifficultyDialog) {
            AlertDialog(
                onDismissRequest = { showDifficultyDialog = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Select Difficulty",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            thickness = 2.dp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Easy Mode Button
                        Button(
                            onClick = {
                                onStartGame(3, GameMode.VS_COMPUTER_EASY)
                                showDifficultyDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Easy",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Perfect for beginners",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Medium Mode Button
                        Button(
                            onClick = {
                                onStartGame(3, GameMode.VS_COMPUTER_MEDIUM)
                                showDifficultyDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Medium",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "For experienced players",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Hard Mode Button
                        Button(
                            onClick = {
                                onStartGame(3, GameMode.VS_COMPUTER_HARD)
                                showDifficultyDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Hard",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Ultimate challenge",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = { showDifficultyDialog = false },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            )
        }

        if (showOnlineDialog) {
            AlertDialog(
                onDismissRequest = { 
                    if (!showWaitingDialog) showOnlineDialog = false 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Online Game",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            thickness = 2.dp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Create Game Button
                        Button(
                            onClick = {
                                Toast.makeText(context, "Online multiplayer coming soon!", Toast.LENGTH_SHORT).show()
                                /* Keeping the implementation for future use
                                scope.launch {
                                    try {
                                        val newSession = gameRepository.createGameSession()
                                        gameSession = newSession
                                        showOnlineDialog = false
                                        showWaitingDialog = true
                                        currentScreen = Screen.GAME
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to create game: ${e.message}", Toast.LENGTH_SHORT).show()
                                        showWaitingDialog = false
                                    }
                                }
                                */
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Create Game",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Start a new game and invite a friend",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Join Game Button
                        Button(
                            onClick = {
//                                showJoinDialog = true
                                Toast.makeText(context, "Online multiplayer coming soon!", Toast.LENGTH_SHORT).show()
                                showJoinDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Join Game",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Join an existing game with a code",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                },
                confirmButton = { },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            if (!showWaitingDialog) showOnlineDialog = false 
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showJoinDialog) {
            AlertDialog(
                onDismissRequest = { showJoinDialog = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Join Game",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            thickness = 2.dp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TextField(
                            value = gameCode,
                            onValueChange = { 
                                // Only allow 4 digits
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    gameCode = it
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            label = { Text("Game Code") },
                            placeholder = { Text("Enter 4-digit code") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                Toast.makeText(context, "Online multiplayer coming soon!", Toast.LENGTH_SHORT).show()
                                showJoinDialog = false
                                /* Keeping the implementation for future use
                                if (gameCode.length == 4) {
                                    scope.launch {
                                        try {
                                            val joinedSession = gameRepository.joinGame(gameCode, gameCode)
                                            if (joinedSession != null) {
                                                gameSession = joinedSession
                                                showJoinDialog = false
                                                showOnlineDialog = false
                                                currentScreen = Screen.GAME
                                            } else {
                                                Toast.makeText(context, "Game not found or no longer available", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to join game: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Please enter a valid 4-digit code", Toast.LENGTH_SHORT).show()
                                }
                                */
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = gameCode.length == 4,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        ) {
                            Text("Join Game")
                        }
                    }
                },
                confirmButton = { },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showJoinDialog = false
                            gameCode = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showWaitingDialog && gameSession != null) {
            AlertDialog(
                onDismissRequest = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Waiting for Opponent",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            thickness = 2.dp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Game Code",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = gameSession?.sessionId ?: "",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Share this code with your friend to join the game",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                confirmButton = { },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showWaitingDialog = false
                            showOnlineDialog = false
                            gameSession = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Exit button always visible at the bottom
        Button(
            onClick = onExitGame,
            modifier = Modifier
                .width(280.dp)
                .padding(top = 32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text(
                text = "Exit Game",
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun GridSelectionScreen(onStartGame: (Int, GameMode) -> Unit, onBackToMenu: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Select Grid Size",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Back button for grid selection
        Button(
            onClick = onBackToMenu,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Back",
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        // 3x3 Grid button
        Card(
            modifier = Modifier
                .width(280.dp)
                .padding(vertical = 8.dp)
                .clickable { onStartGame(3, GameMode.TWO_PLAYER) },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                text = "Classic Mode (3×3)",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // 4x4 Grid button
        Card(
            modifier = Modifier
                .width(280.dp)
                .padding(vertical = 8.dp)
                .clickable { onStartGame(4, GameMode.TWO_PLAYER) },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Text(
                text = "Extended Mode (4×4)",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

// Helper function to check for winner
fun checkWinner(cells: List<String>, size: Int): List<Int>? {
    // Check rows
    for (i in 0 until size) {
        val row = (0 until size).map { i * size + it }
        if (row.all { cells[it] == "X" } || row.all { cells[it] == "O" }) {
            return row
        }
    }

    // Check columns
    for (i in 0 until size) {
        val column = (0 until size).map { it * size + i }
        if (column.all { cells[it] == "X" } || column.all { cells[it] == "O" }) {
            return column
        }
    }

    // Check diagonals
    val diagonal1 = (0 until size).map { it * size + it }
    if (diagonal1.all { cells[it] == "X" } || diagonal1.all { cells[it] == "O" }) {
        return diagonal1
    }

    val diagonal2 = (0 until size).map { it * size + (size - 1 - it) }
    if (diagonal2.all { cells[it] == "X" } || diagonal2.all { cells[it] == "O" }) {
        return diagonal2
    }

    return null
}

// Helper function to find winning move
fun findWinningMove(cells: List<String>, player: String, size: Int): Int? {
    for (i in cells.indices) {
        if (cells[i].isEmpty()) {
            val testBoard = cells.toMutableList()
            testBoard[i] = player
            if (checkWinner(testBoard, size) != null) {
                return i
            }
        }
    }
    return null
}

fun minimax(
    board: List<String>,
    depth: Int,
    isMaximizing: Boolean,
    alpha: Int = Int.MIN_VALUE,
    beta: Int = Int.MAX_VALUE
): Int {
    val winningResult = checkWinner(board, 3)
    
    when {
        winningResult?.isNotEmpty() == true && board[winningResult[0]] == "O" -> return 10 - depth
        winningResult?.isNotEmpty() == true && board[winningResult[0]] == "X" -> return depth - 10
        board.none { it.isEmpty() } -> return 0
    }

    if (isMaximizing) {
        var maxEval = Int.MIN_VALUE
        var currentAlpha = alpha
        for (i in board.indices) {
            if (board[i].isEmpty()) {
                val newBoard = board.toMutableList()
                newBoard[i] = "O"
                val eval = minimax(newBoard, depth + 1, false, currentAlpha, beta)
                maxEval = maxOf(maxEval, eval)
                currentAlpha = maxOf(currentAlpha, eval)
                if (beta <= currentAlpha) break
            }
        }
        return maxEval
    } else {
        var minEval = Int.MAX_VALUE
        var currentBeta = beta
        for (i in board.indices) {
            if (board[i].isEmpty()) {
                val newBoard = board.toMutableList()
                newBoard[i] = "X"
                val eval = minimax(newBoard, depth + 1, true, alpha, currentBeta)
                minEval = minOf(minEval, eval)
                currentBeta = minOf(currentBeta, eval)
                if (currentBeta <= alpha) break
            }
        }
        return minEval
    }
}

// Function to get best move for hard difficulty
fun getBestMove(board: List<String>): Int {
    var bestScore = Int.MIN_VALUE
    var bestMove = -1
    var alpha = Int.MIN_VALUE
    val beta = Int.MAX_VALUE

    for (i in board.indices) {
        if (board[i].isEmpty()) {
            val newBoard = board.toMutableList()
            newBoard[i] = "O"
            val score = minimax(newBoard, 0, false, alpha, beta)
            if (score > bestScore) {
                bestScore = score
                bestMove = i
            }
            alpha = maxOf(alpha, bestScore)
        }
    }
    return bestMove
}

@Composable
fun OnlineGameScreen(
    gameSession: GameSession,
    playerId: String,
    onMove: (Int) -> Unit,
    onRematchRequest: () -> Unit,
    onRematchAccept: () -> Unit,
    onLeaveGame: () -> Unit
) {
    var showRematchDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Game Status
        Text(
            text = when {
                gameSession.status == GameStatus.COMPLETED && gameSession.winner == playerId -> "You Won!"
                gameSession.status == GameStatus.COMPLETED && gameSession.winner != null -> "You Lost!"
                gameSession.status == GameStatus.COMPLETED && gameSession.winner == null -> "Game Draw!"
                gameSession.currentTurn == playerId -> "Your Turn"
                else -> "Opponent's Turn"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = when {
                gameSession.status == GameStatus.COMPLETED && gameSession.winner == playerId -> MaterialTheme.colorScheme.primary
                gameSession.status == GameStatus.COMPLETED && gameSession.winner != null -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Game Board
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
        ) {
            items(9) { index ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(
                            enabled = gameSession.canPlayerMove(playerId) && gameSession.board[index].isEmpty()
                        ) {
                            onMove(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = gameSession.board[index],
                        style = MaterialTheme.typography.headlineLarge,
                        color = when (gameSession.board[index]) {
                            "X" -> MaterialTheme.colorScheme.primary
                            "O" -> MaterialTheme.colorScheme.secondary
                            else -> Color.Transparent
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Game Actions
        if (gameSession.status == GameStatus.COMPLETED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { 
                        if (gameSession.rematchRequestedBy == null) {
                            onRematchRequest()
                        } else if (gameSession.rematchRequestedBy != playerId) {
                            onRematchAccept()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        when {
                            gameSession.rematchRequestedBy == playerId -> "Waiting for opponent..."
                            gameSession.rematchRequestedBy != null -> "Accept Rematch"
                            else -> "Request Rematch"
                        }
                    )
                }

                Button(
                    onClick = onLeaveGame,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Leave Game")
                }
            }
        }
    }
}

@Composable
fun GameScreen(gridSize: Int, gameMode: GameMode, onBackToMenu: () -> Unit) {
    var gameSession by remember { mutableStateOf<GameSession?>(null) }
    val gameRepository = remember { GameRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val playerId = remember { gameSession?.sessionId ?: "" }

    // Observe game session updates
    LaunchedEffect(gameMode) {
        if (gameMode in listOf(GameMode.ONLINE_CREATE, GameMode.ONLINE_JOIN)) {
            gameSession?.sessionId?.let { sessionId ->
                gameRepository.observeGameSession(sessionId).collect { updatedSession ->
                    gameSession = updatedSession
                }
            }
        }
    }

    // Handle back button and cleanup
    DisposableEffect(Unit) {
        onDispose {
            if (gameMode in listOf(GameMode.ONLINE_CREATE, GameMode.ONLINE_JOIN)) {
                scope.launch {
                    gameSession?.sessionId?.let { sessionId ->
                        gameRepository.leaveGame(sessionId, playerId)
                    }
                }
            }
        }
    }

    when {
        gameMode in listOf(GameMode.ONLINE_CREATE, GameMode.ONLINE_JOIN) && gameSession != null -> {
            OnlineGameScreen(
                gameSession = gameSession!!,
                playerId = playerId,
                onMove = { position ->
                    scope.launch {
                        try {
                            val success = gameRepository.makeMove(gameSession!!.sessionId, playerId, position)
                            if (!success) {
                                Toast.makeText(context, "Invalid move", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to make move: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onRematchRequest = {
                    scope.launch {
                        try {
                            gameRepository.requestRematch(gameSession!!.sessionId, playerId)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to request rematch: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onRematchAccept = {
                    scope.launch {
                        try {
                            gameRepository.acceptRematch(gameSession!!.sessionId, playerId)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to accept rematch: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onLeaveGame = {
                    scope.launch {
                        try {
                            gameRepository.leaveGame(gameSession!!.sessionId, playerId)
                            onBackToMenu()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to leave game: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
        else -> {
            // Existing offline game implementation
            var gameState by remember { mutableStateOf(GameState(cells = List(gridSize * gridSize) { "" }, gameMode = gameMode)) }
            val scope = rememberCoroutineScope()

            // Break circular dependency with lateinit
            lateinit var makeComputerMove: () -> Unit
            lateinit var makeMove: (Int) -> Unit

            makeMove = { index ->
                if (!gameState.gameOver && gameState.cells[index].isEmpty()) {
                    val newCells = gameState.cells.toMutableList()
                    newCells[index] = gameState.currentPlayer
                    
                    val winningResult = checkWinner(newCells, gridSize)
                    val newWinner = if (winningResult != null) gameState.currentPlayer else null
                    val isDrawGame = newWinner == null && newCells.none { it.isEmpty() }
                    
                    gameState = gameState.copy(
                        cells = newCells,
                        currentPlayer = if (gameState.currentPlayer == "X") "O" else "X",
                        winner = if (isDrawGame) "Draw" else newWinner,
                        gameOver = isDrawGame || newWinner != null,
                        winningCells = winningResult
                    )

                    // Make computer move if it's computer's turn
                    if ((gameMode == GameMode.VS_COMPUTER_EASY || 
                         gameMode == GameMode.VS_COMPUTER_MEDIUM || 
                         gameMode == GameMode.VS_COMPUTER_HARD) && 
                        !gameState.gameOver && 
                        gameState.currentPlayer == "O") {
                        scope.launch {
                            delay(500) // Add slight delay for better UX
                            makeComputerMove()
                        }
                    }
                }
            }

            makeComputerMove = {
                if (!gameState.gameOver) {
                    // First try to win
                    val emptyCells = gameState.cells.mapIndexedNotNull { index, cell ->
                        if (cell.isEmpty()) index else null
                    }

                    if (emptyCells.isNotEmpty()) {
                        val moveIndex = when (gameState.gameMode) {
                            GameMode.VS_COMPUTER_EASY -> {
                                // Easy: 20% optimal move, 80% random
                                if (Random.nextFloat() < 0.2f) {
                                    val winningMove = findWinningMove(gameState.cells, "O", gridSize)
                                    winningMove ?: emptyCells.random()
                                } else {
                                    emptyCells.random()
                                }
                            }
                            GameMode.VS_COMPUTER_MEDIUM -> {
                                // Medium: Try to win, block, or make strategic move
                                val winningMove = findWinningMove(gameState.cells, "O", gridSize)
                                val blockingMove = findWinningMove(gameState.cells, "X", gridSize)
                                val center = if (gridSize == 3 && gameState.cells[4].isEmpty()) 4 else null
                        
                                when {
                                    winningMove != null -> winningMove
                                    Random.nextFloat() < 0.7f && blockingMove != null -> blockingMove
                                    center != null -> center
                                    else -> emptyCells.random()
                                }
                            }
                            GameMode.VS_COMPUTER_HARD -> {
                                // Hard: Use minimax algorithm
                                getBestMove(gameState.cells)
                            }
                            else -> emptyCells.random()
                        }

                        if (moveIndex != -1) {
                            makeMove(moveIndex)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Back button at the top-left
                Button(
                    onClick = onBackToMenu,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 16.dp, bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Menu",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Back to Menu",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    GameStatus(
                        winner = gameState.winner,
                        currentPlayer = gameState.currentPlayer,
                        gameOver = gameState.gameOver
                    )

                    GameBoard(
                        gameState = gameState.cells,
                        gridSize = gridSize,
                        winningCells = gameState.winningCells,
                        onCellClick = { index -> makeMove(index) }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            gameState = GameState(
                                cells = List(gridSize * gridSize) { "" },
                                gameMode = gameMode
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        modifier = Modifier.width(280.dp)
                    ) {
                        Text(
                            text = "New Game",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameStatus(winner: String?, currentPlayer: String, gameOver: Boolean) {
    val status = when {
        winner == "Draw" -> "Game Draw!"
        winner != null -> "Winner: $winner"
        gameOver -> "Game Over"
        else -> "Player ${currentPlayer}'s Turn"
    }
    
    Text(
        text = status,
        style = MaterialTheme.typography.headlineMedium,
        color = when {
            winner == "Draw" -> MaterialTheme.colorScheme.tertiary
            winner != null -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onBackground
        },
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun GameBoard(
    gameState: List<String>,
    gridSize: Int,
    winningCells: List<Int>?,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(8.dp)
            .aspectRatio(1f)
    ) {
        for (row in 0 until gridSize) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0 until gridSize) {
                    val index = row * gridSize + col
                    GameCell(
                        value = gameState[index],
                        isWinning = winningCells?.contains(index) == true,
                        onClick = { onCellClick(index) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GameCell(
    value: String,
    isWinning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 4.dp,
            hoveredElevation = 12.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isWinning) 
                MaterialTheme.colorScheme.primaryContainer
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 2.dp,
            color = if (isWinning)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isWinning) 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else 
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            if (isWinning) 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else 
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
            ) {
                when (value) {
                    "X" -> {
                        // Glowing X
                        val glowColor = Color.Blue.copy(alpha = 0.3f)
                        // Glow effect
                        drawLine(
                            color = glowColor,
                            start = Offset(size.width * 0.2f, size.height * 0.2f),
                            end = Offset(size.width * 0.8f, size.height * 0.8f),
                            strokeWidth = 16f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = glowColor,
                            start = Offset(size.width * 0.8f, size.height * 0.2f),
                            end = Offset(size.width * 0.2f, size.height * 0.8f),
                            strokeWidth = 16f,
                            cap = StrokeCap.Round
                        )
                        // Main X
                        drawLine(
                            color = Color.Blue,
                            start = Offset(size.width * 0.2f, size.height * 0.2f),
                            end = Offset(size.width * 0.8f, size.height * 0.8f),
                            strokeWidth = 8f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color.Blue,
                            start = Offset(size.width * 0.8f, size.height * 0.2f),
                            end = Offset(size.width * 0.2f, size.height * 0.8f),
                            strokeWidth = 8f,
                            cap = StrokeCap.Round
                        )
                    }
                    "O" -> {
                        // Glowing O
                        val glowColor = Color.Red.copy(alpha = 0.3f)
                        // Glow effect
                        drawCircle(
                            color = glowColor,
                            radius = size.minDimension * 0.35f,
                            style = Stroke(width = 16f)
                        )
                        // Main O
                        drawCircle(
                            color = Color.Red,
                            radius = size.minDimension * 0.3f,
                            style = Stroke(width = 8f)
                        )
                    }
                }
            }
        }
    }
}