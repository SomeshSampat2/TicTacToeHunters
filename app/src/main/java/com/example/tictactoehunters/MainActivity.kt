package com.example.tictactoehunters

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tictactoehunters.ui.theme.TictacToeHuntersTheme
import android.app.Activity

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
    var currentScreen by remember { mutableStateOf("menu") }
    var gridSize by remember { mutableStateOf(3) }
    
    Crossfade(
        targetState = currentScreen,
        animationSpec = tween(300)
    ) { screen ->
        when (screen) {
            "menu" -> MenuScreen(
                onPlayClick = { size -> 
                    gridSize = size
                    currentScreen = "game" 
                }
            )
            "game" -> TicTacToeGame(
                gridSize = gridSize,
                onBackToMenu = { currentScreen = "menu" }
            )
        }
    }
}

@Composable
fun MenuScreen(onPlayClick: (Int) -> Unit) {
    val context = LocalContext.current
    
    val titleScale = remember { Animatable(0.8f) }
    LaunchedEffect(Unit) {
        titleScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .scale(titleScale.value)
                .padding(bottom = 64.dp)
        ) {
            Text(
                text = "Tic Tac Toe",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .blur(radius = 4.dp)
                    .alpha(0.3f)
            )
            Text(
                text = "Tic Tac Toe",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = "Choose Your Game Mode",
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            modifier = Modifier
                .width(280.dp)
                .padding(vertical = 8.dp)
                .clickable { onPlayClick(3) },
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Classic Mode",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "3×3",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        Card(
            modifier = Modifier
                .width(280.dp)
                .padding(vertical = 8.dp)
                .clickable { onPlayClick(4) },
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Extended Mode",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "4×4",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { (context as? Activity)?.finish() },
            modifier = Modifier
                .width(280.dp)
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(
                text = "Exit Game",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun TicTacToeGame(gridSize: Int, onBackToMenu: () -> Unit) {
    var gameState by remember { mutableStateOf(List(gridSize * gridSize) { "" }) }
    var currentPlayer by remember { mutableStateOf("X") }
    var winner by remember { mutableStateOf<String?>(null) }
    var gameOver by remember { mutableStateOf(false) }
    var winningCells by remember { mutableStateOf<List<Int>?>(null) }
    
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

        // Game content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp), 
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    text = if (winner != null) {
                        if (winner == "Draw") "Game Draw!" else "Player $winner Wins!"
                    } else {
                        "Player $currentPlayer's Turn"
                    },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .blur(radius = 4.dp)
                        .alpha(0.3f)
                )
                Text(
                    text = if (winner != null) {
                        if (winner == "Draw") "Game Draw!" else "Player $winner Wins!"
                    } else {
                        "Player $currentPlayer's Turn"
                    },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            GameBoard(
                gameState = gameState,
                gridSize = gridSize,
                winningCells = winningCells,
                onCellClick = { index ->
                    if (gameState[index].isEmpty() && !gameOver) {
                        val newGameState = gameState.toMutableList()
                        newGameState[index] = currentPlayer
                        gameState = newGameState

                        val result = checkWinner(newGameState, gridSize)
                        winner = result?.first
                        winningCells = result?.second
                        if (winner != null) {
                            gameOver = true
                        } else if (!gameState.contains("")) {
                            winner = "Draw"
                            gameOver = true
                        } else {
                            currentPlayer = if (currentPlayer == "X") "O" else "X"
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    gameState = List(gridSize * gridSize) { "" }
                    currentPlayer = "X"
                    winner = null
                    gameOver = false
                    winningCells = null
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                modifier = Modifier.animateContentSize()
            ) {
                Text(
                    text = "New Game",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
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

fun checkWinner(gameState: List<String>, gridSize: Int): Pair<String, List<Int>>? {
    // Check rows
    for (row in 0 until gridSize) {
        val startIndex = row * gridSize
        val rowIndices = (0 until gridSize).map { startIndex + it }
        if (checkLine(gameState, rowIndices)) {
            return gameState[startIndex] to rowIndices
        }
    }

    // Check columns
    for (col in 0 until gridSize) {
        val colIndices = (0 until gridSize).map { it * gridSize + col }
        if (checkLine(gameState, colIndices)) {
            return gameState[col] to colIndices
        }
    }

    // Check diagonals
    val diagonal1 = (0 until gridSize).map { it * gridSize + it }
    if (checkLine(gameState, diagonal1)) {
        return gameState[0] to diagonal1
    }

    val diagonal2 = (0 until gridSize).map { it * gridSize + (gridSize - 1 - it) }
    if (checkLine(gameState, diagonal2)) {
        return gameState[gridSize - 1] to diagonal2
    }

    return null
}

fun checkLine(gameState: List<String>, indices: List<Int>): Boolean {
    val firstValue = gameState[indices.first()]
    return firstValue.isNotEmpty() && indices.all { gameState[it] == firstValue }
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