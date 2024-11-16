package com.example.tictactoehunters

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tictactoehunters.ui.theme.TictacToeHuntersTheme
import kotlinx.coroutines.delay

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
                    TicTacToeGame()
                }
            }
        }
    }
}

data class WinningLine(
    val start: Offset,
    val end: Offset,
    val cells: List<Int>
)

@Composable
fun TicTacToeGame() {
    var gameState by remember { mutableStateOf(List(9) { "" }) }
    var currentPlayer by remember { mutableStateOf("X") }
    var winner by remember { mutableStateOf<String?>(null) }
    var gameOver by remember { mutableStateOf(false) }
    var winningCells by remember { mutableStateOf<List<Int>?>(null) }
    
    // Animation for the game title
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated title with glow effect
        Box(
            modifier = Modifier
                .scale(titleScale.value)
                .padding(bottom = 32.dp)
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
            winningCells = winningCells,
            onCellClick = { index ->
                if (gameState[index].isEmpty() && !gameOver) {
                    val newGameState = gameState.toMutableList()
                    newGameState[index] = currentPlayer
                    gameState = newGameState

                    val result = checkWinner(gameState)
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

        // Animated reset button
        var buttonScale by remember { mutableStateOf(1f) }
        Button(
            onClick = {
                buttonScale = 0.8f
                gameState = List(9) { "" }
                currentPlayer = "X"
                winner = null
                gameOver = false
                winningCells = null
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .scale(buttonScale)
                .animateContentSize()
        ) {
            Text(
                text = "Reset Game",
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun GameBoard(
    gameState: List<String>,
    winningCells: List<Int>?,
    onCellClick: (Int) -> Unit
) {
    val boardScale = remember { Animatable(0.8f) }
    LaunchedEffect(Unit) {
        boardScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 500,
                easing = EaseOutBack
            )
        )
    }

    // Strike line animation
    val strikeLineProgress = remember { Animatable(0f) }
    LaunchedEffect(winningCells) {
        if (winningCells != null) {
            strikeLineProgress.snapTo(0f)
            strikeLineProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 500,
                    easing = EaseOutExpo
                )
            )
        }
    }

    Box {
        Card(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(16.dp)
                .scale(boardScale.value),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                for (row in 0..2) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        for (col in 0..2) {
                            val index = row * 3 + col
                            GameCell(
                                value = gameState[index],
                                isWinning = winningCells?.contains(index) == true,
                                onClick = { onCellClick(index) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            // Draw strike line
            if (winningCells != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val startCell = winningCells.first()
                    val endCell = winningCells.last()
                    
                    val startRow = startCell / 3
                    val startCol = startCell % 3
                    val endRow = endCell / 3
                    val endCol = endCell % 3

                    val cellWidth = size.width / 3
                    val cellHeight = size.height / 3

                    val start = Offset(
                        (startCol * cellWidth) + (cellWidth / 2),
                        (startRow * cellHeight) + (cellHeight / 2)
                    )
                    val end = Offset(
                        (endCol * cellWidth) + (cellWidth / 2),
                        (endRow * cellHeight) + (cellHeight / 2)
                    )

                    val currentEnd = Offset(
                        start.x + (end.x - start.x) * strikeLineProgress.value,
                        start.y + (end.y - start.y) * strikeLineProgress.value
                    )

                    // Draw glow effect
                    drawLine(
                        color = Color.Yellow.copy(alpha = 0.3f),
                        start = start,
                        end = currentEnd,
                        strokeWidth = 24f,
                        cap = StrokeCap.Round
                    )

                    // Draw main line
                    drawLine(
                        color = Color.Yellow,
                        start = start,
                        end = currentEnd,
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isWinning) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface,
                            if (isWinning) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                    )
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
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

fun checkWinner(gameState: List<String>): Pair<String, List<Int>>? {
    val winningCombinations = listOf(
        // Rows
        listOf(0, 1, 2),
        listOf(3, 4, 5),
        listOf(6, 7, 8),
        // Columns
        listOf(0, 3, 6),
        listOf(1, 4, 7),
        listOf(2, 5, 8),
        // Diagonals
        listOf(0, 4, 8),
        listOf(2, 4, 6)
    )

    for (combination in winningCombinations) {
        val (a, b, c) = combination
        if (gameState[a].isNotEmpty() &&
            gameState[a] == gameState[b] &&
            gameState[a] == gameState[c]
        ) {
            return Pair(gameState[a], combination)
        }
    }
    return null
}